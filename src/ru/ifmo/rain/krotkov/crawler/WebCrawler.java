package ru.ifmo.rain.krotkov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;
import net.java.quickcheck.collection.Pair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final int perHostLimit;
    private final ExecutorService downloadersPool;
    private final ExecutorService extractorsPool;
    private final Downloader downloader;
    private final ConcurrentMap<String, HostInfo> hostInfoMap;

    private final int DEFAULT_TIMEOUT = 1000; //milliseconds;

    private class HostInfo {
        int fullness;
        Queue<Runnable> tasks = new LinkedBlockingQueue<>();

        synchronized void pollTask() {
            if (!tasks.isEmpty()) {
                downloadersPool.submit(tasks.poll());
            } else {
                fullness--;
            }
        }

        synchronized void pushTask(Runnable task) {
            if (fullness >= perHostLimit) {
                tasks.add(task);
            } else {
                fullness++;
                downloadersPool.submit(task);
            }
        }
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.perHostLimit = perHost;
        this.downloader = downloader;
        this.downloadersPool = Executors.newFixedThreadPool(downloaders);
        this.extractorsPool = Executors.newFixedThreadPool(extractors);

        this.hostInfoMap = new ConcurrentHashMap<>();
    }

    @Override
    public Result download(String url, int depth) {
        Set<String> result = new ConcurrentSkipListSet<>();
        Map<String, IOException> exceptions = new ConcurrentHashMap<>();
        Phaser phaser = new Phaser(1);

        bfsDownload(url, depth, phaser, result, exceptions);
        phaser.arriveAndAwaitAdvance();

        result.removeAll(exceptions.keySet());
        return new Result(new ArrayList<>(result), exceptions);
    }

    @Override
    public void close() {
        shutdownExecutorService(extractorsPool, DEFAULT_TIMEOUT);
        shutdownExecutorService(downloadersPool, DEFAULT_TIMEOUT);
    }

    private void shutdownExecutorService(ExecutorService executorService, long timeout) {
        try {
            executorService.shutdown();
            executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void bfsDownload(final String initialUrl, final int initialDepth, final Phaser phaser,
                             final Set<String> result, final Map<String, IOException> exceptions) {
        Queue<Pair<Integer, List<String>>> urlPackQueue = new ArrayDeque<>();
        urlPackQueue.add(new Pair<>(initialDepth, List.of(initialUrl)));

        while (!urlPackQueue.isEmpty()) {
            Pair<Integer, List<String>> urlPack = urlPackQueue.poll();
            int depth = urlPack.getFirst();

            List<String> nextUrlPack = new CopyOnWriteArrayList<>();
            Phaser localPhaser = new Phaser(1);

            for (String url : urlPack.getSecond()) {
                if (exceptions.keySet().contains(url) || !result.add(url)) {
                    continue;
                }

                String hostName;
                try {
                    hostName = URLUtils.getHost(url);
                } catch (MalformedURLException e) {
                    exceptions.put(url, e);
                    continue;
                }

                Runnable downloaderTask = () -> {
                    try {
                        Document document = downloader.download(url);

                        localPhaser.register();
                        Runnable extractorsTask = () -> {
                            try {
                                nextUrlPack.addAll(document.extractLinks());
                            } catch (IOException e) {
                                exceptions.put(url, e);
                            } finally {
                                localPhaser.arrive();
                            }
                        };
                        extractorsPool.submit(extractorsTask);
                    } catch (IOException e) {
                        exceptions.put(url, e);
                    }

                    hostInfoMap.get(hostName).pollTask();
                    localPhaser.arrive();
                };

                localPhaser.register();
                hostInfoMap.computeIfAbsent(hostName, h -> new HostInfo()).pushTask(downloaderTask);
            }

            localPhaser.arriveAndAwaitAdvance();
            if (depth > 1) {
                urlPackQueue.add(new Pair<>(depth - 1, nextUrlPack));
            }
        }

        phaser.arrive();
    }


    private static int getArgumentByIndex(final String[] args, int index) {
        return index < args.length ? Integer.parseInt(args[index]) : 1;
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length <= 2) {
                System.out.println("At least two arguments expected");
                return;
            }
            for (int index = 0; index < args.length; index++) {
                if (args[index] == null) {
                    System.out.println("Non-null argument expected at index " + index);
                    return;
                }
            }

            int downloaders = getArgumentByIndex(args, 1);
            int extractors = getArgumentByIndex(args, 2);
            int perHost = getArgumentByIndex(args, 3);
            int depth = getArgumentByIndex(args, 4);

            try (WebCrawler webCrawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost)) {
                webCrawler.download(args[0], depth);
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println(e.getMessage());
        }
    }


}
