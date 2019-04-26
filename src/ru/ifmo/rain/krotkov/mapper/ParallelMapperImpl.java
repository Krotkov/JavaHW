package ru.ifmo.rain.krotkov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {

    private List<Thread> workingThreads;
    private final Queue<TaskWrapper> taskWrappersQueue;

    public ParallelMapperImpl(int threadsNumber) {
        if (threadsNumber <= 0) {
            throw new IllegalArgumentException("Threads number must be positive");
        }

        workingThreads = new ArrayList<>();
        taskWrappersQueue = new ArrayDeque<>();

        Runnable worker = () -> {
            try {
                while (!Thread.interrupted()) {
                    doTask();
                }
            } catch (InterruptedException ignored) {
                // anyway will interrupt current thread
            } finally {
                Thread.currentThread().interrupt();
            }
        };
        for (int i = 0; i < threadsNumber; i++) {
            workingThreads.add(new Thread(worker));
        }
        workingThreads.forEach(Thread::start);
    }

    private void doTask() throws InterruptedException {
        TaskWrapper taskWrapper;
        synchronized (taskWrappersQueue) {
            while (taskWrappersQueue.isEmpty()) {
                taskWrappersQueue.wait();
            }

            taskWrapper = taskWrappersQueue.poll();
        }

        taskWrapper.run();
    }

    private class Counter {
        private int cnt;
        private int bound;

        Counter(int bound) {
            this.bound = bound;
        }

        void increment() {
            cnt++;
        }

        boolean isFull() {
            return cnt >= bound;
        }
    }

    private class TaskMeta {
        Exception exception = null;
        final Counter counter;

        TaskMeta(Counter counter) {
            this.counter = counter;
        }

        boolean hasException() {
            return exception != null;
        }

        void setException(Exception e) {
            exception = e;
        }

        Exception getException() {
            return exception;
        }

        void incrementCounter() {
            synchronized (counter) {
                counter.increment();
                if (counter.isFull()) {
                    counter.notify();
                }
            }
        }

        void waitCompletion() throws InterruptedException {
            synchronized (counter) {
                while (!counter.isFull()) {
                    counter.wait();
                }
            }
        }
    }

    private class TaskWrapper {
        Runnable task;
        final TaskMeta meta;

        TaskWrapper(Runnable task, TaskMeta meta) {
            this.task = task;
            this.meta = meta;
        }

        void incrementCounter() {
            meta.incrementCounter();
        }

        Exception getException() {
            return meta.getException();
        }

        void run() {
            try {
                task.run();
            } catch (Exception e) {
                synchronized (meta) {
                    meta.setException(e);
                }
            } finally {
                incrementCounter();
            }
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @return {@link List} of mapped args or nulls on {@link Exception}
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        List<R> resultList = new ArrayList<>(Collections.nCopies(args.size(), null));
        TaskMeta taskMeta = new TaskMeta(new Counter(args.size()));

        for (int i = 0; i < args.size(); i++) {
            final int pos = i;
            TaskWrapper wrapper = new TaskWrapper(/*Task*/() -> resultList.set(pos, f.apply(args.get(pos))), taskMeta);
            synchronized (taskWrappersQueue) {
                taskWrappersQueue.add(wrapper);
                taskWrappersQueue.notify();
            }
        }

        taskMeta.waitCompletion();

        if (taskMeta.hasException()) {
            throw new RuntimeException(taskMeta.getException());
        }
        return resultList;
    }

    /**
     * Stops all threads. All unfinished mappings are leaved in undefined state.
     */
    @Override
    public void close() {
        workingThreads.forEach(Thread::interrupt);

        for (Thread thread : workingThreads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
                // ignore
            }
        }
    }
}
