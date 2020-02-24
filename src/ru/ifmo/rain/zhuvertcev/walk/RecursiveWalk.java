package ru.ifmo.rain.zhuvertcev.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class RecursiveWalk {
    private final String input;
    private final String output;

    RecursiveWalk(final String input, final String output) {
        this.input = input;
        this.output = output;
    }

    public void start() throws WalkException {
        try (final BufferedReader fileReader = new BufferedReader(new FileReader(input, StandardCharsets.UTF_8))) {
            final HashVisitor hashVisitor = new HashVisitor(output);
            String path = fileReader.readLine();
            while (path != null) {
                try {
                    Files.walkFileTree(Paths.get(path), hashVisitor);
                } catch (final InvalidPathException e) {
                    hashVisitor.writeHash(path, 0);
                }
                path = fileReader.readLine();
            }
            hashVisitor.close();
        } catch (final FileNotFoundException e) {
            throw new WalkException("File " + input + " not found");
        } catch (final IOException e) {
            throw new WalkException("IOException:\n" + e.getMessage());
        }
    }

    public static void main(String[] args)
    {
        try {
            if (args == null) {
                throw new WalkException("Have no args, or it's null");
            }
            if (args.length != 2) {
                throw new WalkException("Please put correct data: input_string output_string");
            }
            if (args[0] == null || args[1] == null) {
                throw new WalkException("Args can't be null");
            }
            final RecursiveWalk rw = new RecursiveWalk(args[0], args[1]);
            rw.start();
        } catch (final WalkException e) {
            System.err.println(e.getMessage());
        }
    }
}
