package ru.ifmo.rain.zhuvertcev.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class HashVisitor extends SimpleFileVisitor<Path> {
    final FileWriter fileWriter;
    final int START_HASH = 0x811c9dc5;

    HashVisitor(String outputFile) throws WalkException {
        final File file = new File(outputFile);
        if (file.getParent() != null) {
            try {
                Files.createDirectories(Paths.get(outputFile).getParent());
            } catch (final IOException e) {
                throw new WalkException("Can't create directory or directories for output file");
            }
        }
        try {
            fileWriter = new FileWriter(file, StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new WalkException("Can't create or open output file");
        }
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes ignore) throws IOException {
        int hash = START_HASH;
        final File file = path.toFile();
        final FileInputStream inputStream = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int bufferLength;
        while ((bufferLength = inputStream.read(buffer)) != -1) {
            hash = hashing(buffer, hash, bufferLength);
        }
        writeHash(file.toString(), hash);
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException ignore) throws IOException {
        writeHash(path.toString(), 0);
        return FileVisitResult.SKIP_SUBTREE;
    }

    public void writeHash (final String file, final int hash) throws IOException {
        fileWriter.write(String.format("%08x%n", hash).concat(" ".concat(file)));
    }

    private int hashing(final byte[] bytes, int h, final int bufferLength) {
        for (int i = 0; i < bufferLength; i++) {
            h = (h * 0x01000193) ^ (bytes[i] & 0xff);
        }
        return h;
    }

    public void close() throws IOException {fileWriter.close();}
}
