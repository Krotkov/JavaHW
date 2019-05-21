package ru.ifmo.rain.krotkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    private static final int SOCKET_SO_TIMEOUT = 500;

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        if (threads <= 0) {
            throw new AssertionError("Threads number must be positive");
        }

        InetAddress addr;
        try {
            addr = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.err.println("Unable to find host :" + host);
            return;
        }

        final SocketAddress socketAddress = new InetSocketAddress(addr, port);
        final ExecutorService workers = Executors.newFixedThreadPool(threads);

        IntStream.range(0, threads).forEach(id -> workers.submit(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(SOCKET_SO_TIMEOUT);
                final byte[] buffer = new byte[socket.getReceiveBufferSize()];
                final DatagramPacket response = new DatagramPacket(buffer, socket.getReceiveBufferSize());
                for (int num = 0; num < requests; num++) {
                    final String requestText = prefix + id + "_" + num;
                    byte[] dataRequest = requestText.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket msg = new DatagramPacket(dataRequest, requestText.length(), socketAddress);
                    while (!socket.isClosed() || Thread.currentThread().isInterrupted()) {
                        try {
                            socket.send(msg);
                            System.out.println("Request sent:\n" + requestText + "\n");
                            socket.receive(response);
                            String responseText = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
                            if (responseText.contains(requestText)) {
                                System.out.println("Response received:\n" + responseText + "\n");
                                break;
                            }
                        } catch (IOException e) {
                            System.err.println("Error while processing request: " + e.getMessage());
                        }
                    }
                }
            } catch (SocketException e) {
                System.err.println("Can't create socket: " + addr.toString());
            }
        }));

        workers.shutdown();
        try {
            workers.awaitTermination(requests * 5, TimeUnit.SECONDS); // ten tries per request
        } catch (InterruptedException ignored) {
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Five arguments expected");
            return;
        }
        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Non-null arguments expected");
            return;
        }
        try {
            new HelloUDPClient().run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (NumberFormatException e) {
            System.err.println("Integer expected: " + e.getMessage());
        }
    }
}