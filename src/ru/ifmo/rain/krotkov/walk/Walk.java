package ru.ifmo.rain.krotkov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walk {
    private static byte[] buf = new byte[1024];

    private static final class FNVHash {
        private static final int FNV_32_INIT = 0x811c9dc5;
        private static final int FNV_32_PRIME = 0x01000193;
        private int res;

        FNVHash() {
            res = FNV_32_INIT;
        }

        void hash32(final byte[] k, final int end) {
            for (int i = 0; i < end; i++) {
                res = (res * FNV_32_PRIME) ^ (k[i] & 0xff);
            }
        }

        void makeBadHash() {
            res = 0;
        }

        int getHash() {
            return res;
        }
    }

    static void writeLine(int hash, String name, Writer writer) {
        try {
            writer.write(String.format("%08x %s%s", hash, name, System.lineSeparator()));
        } catch (IOException e) {
            System.out.println("Error of writing hash of file: " + name);
        }
    }

    static void readOneFile(Path path, Writer writer) {
        FNVHash fnv = new FNVHash();
        try (InputStream hashReader = Files.newInputStream(path)) {
            int c;
            while ((c = hashReader.read(buf)) >= 0) {
                fnv.hash32(buf, c);
            }
        } catch (IOException | SecurityException e) {
            fnv.makeBadHash();
        } finally {
            writeLine(fnv.getHash(), path.toString(), writer);
        }
    }


    static boolean isCorrectArgs(String[] args) {
        if (args == null) {
            System.out.println("null was received as pathes");
            return false;
        }
        if (args.length != 2) {
            System.out.println("Wrong number of arguments. Expected 2, got " + args.length);
            return false;
        }
        if (args[0] == null) {
            System.out.println("null was received as path to input file");
            return false;
        }
        if (args[1] == null) {
            System.out.println("null was received as path to output file");
            return false;
        }
        return true;
    }

    static boolean createDirectories(String path){
        Path dirPath = null;
        try {
            Path p = Paths.get(path);
            dirPath = p.getParent();
        } catch (InvalidPathException e) {
            System.out.println("Wrong path to output file");
        }

        if (dirPath != null) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                System.out.println("Access to output file is denied");
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        if (!isCorrectArgs(args)) {
            return;
        }
        if (!createDirectories(args[1])) {
            return;
        }

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new FileInputStream(args[0]), StandardCharsets.UTF_8));
                FileWriter writer = new FileWriter(args[1])
        ) {
            String name;
            while ((name = reader.readLine()) != null) {
                try {
                    readOneFile(Paths.get(name), writer);
                } catch (InvalidPathException e) {
                    writeLine(0, name, writer);
                }
            }
        } catch (IOException e) {
            System.out.println("Input or output file is not existed");
        } catch (SecurityException e) {
            System.out.println("Access to input or output file is denied");
        }
    }
}