package ru.ifmo.rain.krotkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService workers;
    private ExecutorService listener;
    private int inBuffSize = 0;

    private static final int TERMINATION_AWAIT_TIMEOUT = 5; // seconds

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            inBuffSize = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            System.err.println("Unable to create socket on port " + port);
            return;
        }
        workers = Executors.newFixedThreadPool(threads);
        listener = Executors.newSingleThreadExecutor();

        listener.submit(() -> {
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    final byte[] buffer = new byte[inBuffSize];
                    final DatagramPacket msg = new DatagramPacket(buffer, inBuffSize);
                    socket.receive(msg);
                    workers.submit(() -> {
                        final String msgText = new String(msg.getData(), 0, msg.getLength(), StandardCharsets.UTF_8);
                        final String responseText = "Hello, " + msgText;
                        try {
                            msg.setData(responseText.getBytes(StandardCharsets.UTF_8));
                            socket.send(msg);
                        } catch (IOException e) {
                            if (!socket.isClosed()) {
                                System.err.println("Error occurred during processing datagram: " + e.getMessage());
                            }
                        }
                    });
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        System.err.println("Error occurred during processing datagram: " + e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void close() {
        socket.close();
        workers.shutdown();
        listener.shutdown();
        try {
            workers.awaitTermination(TERMINATION_AWAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Two non-null arguments expected");
            return;
        }
        try {
            new HelloUDPServer().start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } catch (NumberFormatException e) {
            System.err.println("Integer expected: " + e.getMessage());
        }
    }
}