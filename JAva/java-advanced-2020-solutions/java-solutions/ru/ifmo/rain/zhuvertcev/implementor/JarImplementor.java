package ru.ifmo.rain.zhuvertcev.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class JarImplementor extends Implementor implements JarImpler {

    /**
     * Delete all content by recursion
     * <p>
     * If <var>path</var> is directory recursively delete all content
     * in this, otherwise just delete file by <var>path</var>
     *
     * @param path path to directory or file to be cleaned by recursion
     */
    private static void clear(Path path) {
        File tmpFile = path.toFile();
        File[] allContents = tmpFile.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                clear(file.toPath());
            }
        }
        tmpFile.delete();
    }


    /**
     * Produces <var>.jar</var> file implementing class or interface specified by provided <var>token</var>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <var>Impl</var> suffix
     * added.
     *
     * @param token type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        nullCheck(token, jarFile);
        creatDirectories(jarFile);
        Path tempDirectory;
        try {
            tempDirectory = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp");
        } catch (IOException e) {
            throw new ImplerException("Couldn't create temp directory", e);
        }

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.EXTENSION_NAME, "Fraks");
        attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, "1.0");
        try (JarOutputStream jarWriter = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            implement(token, tempDirectory);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

            if (compiler.run(null, null, null, "-cp",
                    tempDirectory.toString() + File.pathSeparator + System.getProperty("java.class.path"),
                    outputFile) != 0) {
                throw new ImplerException("Couldn't compile file: " + outputFile);
            }

            jarWriter.putNextEntry(new ZipEntry(token.getPackageName().replace('.', File.separatorChar)
                    + File.separatorChar  + getClassName(clazz) + ".class"));
            String classPath = outputFile.substring(0, outputFile.length() - 4) + "class";
            Files.copy(Paths.get(classPath), jarWriter);
        } catch (IOException e) {
            throw new ImplerException("IOException in JarOutputSteam: ", e);
        } finally {
            clear(tempDirectory);
        }
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg == null) {
                System.out.println("Arg(s) is null");
            }
        }
        JarImpler implementor = new JarImplementor();
        try {
            implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
        } catch (ClassNotFoundException e) {
            System.out.println("Couldn't find class: " + e.getMessage());
        } catch (ImplerException e) {
            System.out.println("Couldn't implement: " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
        }
    }
}
