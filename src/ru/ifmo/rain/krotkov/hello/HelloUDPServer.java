package ru.ifmo.rain.krotkov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket;
    private ExecutorService workers;
    private ExecutorService listener;
    private boolean closed = true;
    private int inBuffSize = 0;

    private static final int POOL_SIZE = 100000;
    private static final int TERMINATION_AWAIT_TIMEOUT = 5; // seconds
    private static final int EXECUTOR_LIFE_TIME = 1; // minute

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
            inBuffSize = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            System.err.println("Unable to create socket on port " + port);
            return;
        }
        workers = new ThreadPoolExecutor(threads, threads,
                EXECUTOR_LIFE_TIME, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(POOL_SIZE), new ThreadPoolExecutor.DiscardPolicy());
        listener = Executors.newSingleThreadExecutor();

        closed = false;
        listener.submit(this::receiveAndRespond);
    }

    private void receiveAndRespond() {
        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
            try {
                final DatagramPacket msg = MessageSupport.newMessage(inBuffSize);
                socket.receive(msg);
                workers.submit(() -> sendResponse(msg));
            } catch (IOException e) {
                if (!closed) {
                    System.err.println("Error occurred during processing datagram: " + e.getMessage());
                }
            }
        }
    }

    private void sendResponse(final DatagramPacket msg) {
        final String msgText = MessageSupport.getText(msg);
        try {
            MessageSupport.setText(msg, "Hello, " + msgText);
            socket.send(msg);
        } catch (IOException e) {
            if (!closed) {
                System.err.println("Error occurred during processing datagram: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
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