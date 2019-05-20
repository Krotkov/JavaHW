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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    private static final int SOCKET_SO_TIMEOUT = 500; //milliseconds

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

        final SocketAddress dst = new InetSocketAddress(addr, port);
        final ExecutorService workers = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int id = i;
            workers.submit(() -> sendAndReceive(dst, prefix, requests, id));
        }

        workers.shutdown();
        try {
            workers.awaitTermination(requests * 5, TimeUnit.SECONDS); // ten tries per request
        } catch (InterruptedException ignored) {
        }
    }

    private static void sendAndReceive(final SocketAddress addr, final String prefix, int cnt, int id) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_SO_TIMEOUT);
            final DatagramPacket msg = MessageSupport.newEmptyMessage();
            msg.setSocketAddress(addr);

            for (int num = 0; num < cnt; num++) {
                final String requestText = buildRequestText(prefix, id, num);
                while (!socket.isClosed() || Thread.currentThread().isInterrupted()) {
                    try {
                        MessageSupport.setText(msg, requestText);
                        socket.send(msg);
                        System.out.println("Request sent:\n" + requestText + "\n");

                        MessageSupport.resize(msg, socket.getReceiveBufferSize());
                        socket.receive(msg);
                        String responseText = MessageSupport.getText(msg);
                        if (responseText.contains(requestText)) {
                            System.out.println("Response received:\n" + responseText + "\n");
                            break;
                        }
                    } catch (IOException e) {
                        System.err.println("Error occurred while processing request: " + e.getMessage());
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Unable to create socket: " + addr.toString());
        }
    }

    private static String buildRequestText(final String prefix, int thread, int num) {
        return prefix + thread + "_" + num;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Five arguments expected");
            return;
        }
        for (int i = 0; i < 5; i++) {
            if (args[i] == null) {
                System.err.println("Non-null argument expected at position " + i);
                return;
            }
        }
        try {
            new HelloUDPClient().run(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
        } catch (NumberFormatException e) {
            System.err.println("Integer expected: " + e.getMessage());
        }
    }
}