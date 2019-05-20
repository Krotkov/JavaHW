package ru.ifmo.rain.krotkov.hello;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class MessageSupport {
    public static String getText(final DatagramPacket msg) {
        return new String(msg.getData(), msg.getOffset(), msg.getLength(), StandardCharsets.UTF_8);
    }

    public static void setText(final DatagramPacket msg, final String text) {
        msg.setData(text.getBytes(StandardCharsets.UTF_8));
    }

    public static DatagramPacket newMessage(int buffSize) {
        final byte[] buff = new byte[buffSize];
        return new DatagramPacket(buff, buff.length);
    }

    public static DatagramPacket newEmptyMessage() {
        return new DatagramPacket(new byte[0], 0);
    }

    public static DatagramPacket resize(final DatagramPacket msg, int length) {
        msg.setData(new byte[length]);
        return msg;
    }
}