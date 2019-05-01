package ru.ifmo.rain.krotkov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk extends Walk {

    private static class MyFileVisitor extends SimpleFileVisitor<Path> {
        Writer writer;

        MyFileVisitor(Writer w) {
            writer = w;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            readOneFile(file, writer);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            writeLine(0, file.toString(), writer);
            return FileVisitResult.CONTINUE;
        }
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
                OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(args[1]), StandardCharsets.UTF_8)
        ) {
            String directory;
            while ((directory = reader.readLine()) != null) {
                try {
                    Files.walkFileTree(Paths.get(directory), new MyFileVisitor(writer));
                } catch (IOException e) {
                    System.out.println("File or directory: " + directory + " is not existed");
                } catch (SecurityException e) {
                    System.out.println("Access to file or directory: " + directory + " is denied");
                } catch (InvalidPathException e) {
                    writeLine(0, directory, writer);
                }
            }
        } catch (IOException e) {
            System.out.println("Input or output file is not existed");
        } catch (SecurityException e) {
            System.out.println("Access to input or output file is denied");
        }
    }
}