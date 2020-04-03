package ru.ifmo.rain.zhuvertcev.implementor;

import info.kgeorgiy.java.advanced.implementor.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Implementor implements Impler {

    /**
     * This string need write
     */
    private StringBuilder file;

    /**
     * This class is implemented
     */
    protected Class<?> clazz;

    /**
     * File with path, there class implementation will be
     */
    protected String outputFile;

    /**
     * Set of Methods, contains methods, in order to not implement 1 method twice
     */
    private Set<Method> methodsSet;

    /**
     * Line separator for current Operation system
     */
    private final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Tabulation string
     */
    private final String TAB = "    ";

    /**
     * Get simple class name with Impl on end
     *
     * @param implementsClass target to get
     * @return {@link String}
     */
    protected String getClassName(Class<?> implementsClass) {
        return implementsClass.getSimpleName() + "Impl";
    }

    /**
     * Write default value for class
     *
     * @param returnType class target to find default value
     * @return {@link String} default value for <var>returnType</var>
     */
    private static String getDefaultValue(Class<?> returnType) {
        if (returnType.equals(boolean.class)) {
            return " false";
        } else if (returnType.equals(void.class)) {
            return "";
        } else if (returnType.isPrimitive()) {
            return " 0";
        }
        return " null";
    }

    /**
     * Write class without implementation.
     * <p>
     * Write package, class name, extends or implement from implemented class
     */
    private void writeOutClass() {
        StringBuilder before = new StringBuilder("package " + clazz.getPackage().getName() + ";"
                + LINE_SEPARATOR + LINE_SEPARATOR);
        before.append(LINE_SEPARATOR).append("public class ").append(getClassName(clazz)).append(" ");
        if (clazz.isInterface()) {
            before.append("implements ");
        } else {
            before.append("extends ");
        }
        before.append(clazz.getCanonicalName()).append("{").append(LINE_SEPARATOR);
        before.append(file).append(LINE_SEPARATOR).append("}");
        file = before;
    }

    /**
     * Write modifiers of class member except Abstract, Native, Transient
     *
     * @param member member of class, target to write modifiers
     * @param <T> class extends <class>Member</class>
     * @return {@link StringBuilder} modifiers of <var>member</var> except Abstract,
     * Native, Transient
     */
    private <T extends Member> StringBuilder writeModifier(T member) {
        return new StringBuilder(Modifier
                .toString(member.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.NATIVE & ~Modifier.TRANSIENT))
                .append(" ");
    }

    /**
     * Write parameter <var>parameter</var> in java format
     *
     * @param parameter target to write this parameter in java format
     * @param withType if <code>withTypes == 1</code> write types for parameter
     *                 else don't write types for parameter
     * @return {@link String} parameter in java format
     */
    private String makeParameter(Parameter parameter, boolean withType) {
        return (withType ? parameter.getType().getCanonicalName() + " " : "") + parameter.getName();
    }

    /**
     * Build in java format parameters for method or constructor.
     *
     * @param executable Method or constructor to implement parameters for <var>clazz</var>
     * @param withTypes if <var>withTypes</var> is 1 write types for parameters
     *                  else don't write types for parameters
     * @return {@link String} parameters of <var>executable</var>
     */
    private String writeParameters(Executable executable, boolean withTypes) {
        return List.of(executable.getParameters()).stream().map(parameter -> makeParameter(parameter, withTypes)).
                collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Build in java format return type and name for method or constructor.
     *
     * @param executable Method or constructor to implement return type and name for <var>clazz</var>
     * @return {@link String} type and name of <var>executable</var>
     */
    private String getReturnTypeAndName(Executable executable) {
        if (executable instanceof Method) {
            Method tmp = (Method) executable;
            return tmp.getReturnType().getCanonicalName() + " " + tmp.getName();
        } else {
            return getClassName(((Constructor<?>) executable).getDeclaringClass());
        }
    }

    /**
     * Build in java format body for method or constructor.
     *
     * @param executable Method to implement body for <var>clazz</var>
     * @return {@link StringBuilder} body of <var>executable</var>
     */
    private StringBuilder returnString(Executable executable) {
        StringBuilder exeString = new StringBuilder(TAB + TAB);
        if (executable instanceof Method) {
            exeString.append("return").append(getDefaultValue(((Method) executable).getReturnType()));
        } else {
            exeString.append("super").append(writeParameters(executable, false));
        }
        return exeString.append(";");
    }

    /**
     * Build in java format throwing exceptions for method or constructor.
     *
     * @param executable Method to implement exceptions for <var>clazz</var>
     * @return {@link StringBuilder} of exceptions for implemented method
     */
    private StringBuilder writeExceptions(Executable executable) {
        Class<?>[] exceptionTypes = executable.getExceptionTypes();
        StringBuilder throwExceptionsString = new StringBuilder();
        if (exceptionTypes.length != 0) {
            throwExceptionsString.append(" throws ").append(List.of(exceptionTypes).stream()
                    .map(Class::getCanonicalName).collect(Collectors.joining(", ")));
        }
        return throwExceptionsString;
    }

    /**
     * Build in java format method or constructor.
     *
     * @param executable Method or constructor to implement for <var>clazz</var>
     * @return {@link StringBuilder} of implemented method
     */
    private StringBuilder executableFormat(Executable executable) {
        StringBuilder methodString = new StringBuilder();
        methodString.append(TAB).append(writeModifier(executable)).append(getReturnTypeAndName(executable))
                .append(writeParameters(executable, true)).append(writeExceptions(executable)).append(" {").
                append(LINE_SEPARATOR).append(returnString(executable)).
                append(LINE_SEPARATOR).append(TAB + "}").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        return methodString;
    }


    /**
     * Implement constructors for abstract class <var>clazz</var>, write them in <var>file</var>
     * <p>
     * Implement only non-private constructors for abstract class <var>clazz</var>, if there are none, throws
     * {@link ImplerException}. Add correct constructors in <var>file</var>
     *
     * @throws ImplerException if <code>constructors.size() == 0 </code>, means have not non-private
     * constructors
     */
    private void implementConstructors() throws ImplerException {
        List<Constructor<?>> constructors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .collect(Collectors.toList());
        for (var constructor : constructors) {
            file.append(executableFormat(constructor));
        }
        if (constructors.size() == 0) {
            throw new ImplerException("Haven't non-private constructors");
        }
    }

    /**
     * Implement methods, write them in <var>file</var>
     *
     * @param methods <class>Method</class> array to write in implement file
     */
    private void implementMethods(Method[] methods) {
        for (Method method : methods) {
            if (!methodsSet.contains(method)) {
                methodsSet.add(method);
                if (Modifier.isAbstract(method.getModifiers())) {
                    file.append(executableFormat(method));
                }
            }
        }
    }

    /**
     * Check, that token and root not null, if we have null throw
     * {@link ImplerException}
     *
     * @param token Class <var>token</var> to check for null
     * @param root {@link Path} <var>root</var> to check for null
     * @throws ImplerException if token or root is null
     */
    protected void nullCheck(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("root and class cannot be null");
        }
    }

    /**
     * Try create directories by <var>path</var>
     *
     * @param path all directories by this path should be created
     * @throws ImplerException when function <method>createDirectories</method> couldn't
     * create directories
     */
    protected void creatDirectories(Path path) throws ImplerException {
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new ImplerException("Couldn't make directory(s)", e);
        }
    }

    /**
     * Presents string in UTF-8 format
     *
     * @param original {@link StringBuilder} to be presented in UTF string
     * @return String in UTF format
     */
    private String toUTF(StringBuilder original) {
        byte[] utf8Bytes = original.toString().getBytes(StandardCharsets.UTF_8);
        return new String(utf8Bytes, StandardCharsets.UTF_8);
    }

    /**
     * Produces code implementing class or interface specified by provided <var>token</var>.
     * <p>
     * Generated class classes name should be same as classes name of the type token with <var>Impl</var> suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * <var>root</var> directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to <var>$root/java/util/ListImpl.java</var>
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException when implementation cannot be
     * generated.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        nullCheck(token, root);
        int modifiers = token.getModifiers();
        if (token.isPrimitive() || token.isArray() || token == Enum.class || Modifier.isFinal(modifiers)
                || Modifier.isPrivate(modifiers)) {
            throw new ImplerException("Unable implement " + token.getCanonicalName());
        }
        file = new StringBuilder();
        clazz = token;
        methodsSet = new HashSet<>();
        if (!clazz.isInterface()) {
            implementConstructors();
        }
        implementMethods(clazz.getMethods());
        implementMethods(token.getDeclaredMethods());
        String classDirectory = clazz.getPackageName();
        outputFile = root.toString() + File.separatorChar + classDirectory.replace('.', File.separatorChar) + File.separatorChar + getClassName(clazz) + ".java";
        creatDirectories(Paths.get(outputFile));
        try (FileWriter fw = new FileWriter(outputFile)) {
            writeOutClass();
            fw.write(toUTF(file));
        } catch (IOException e) {
            throw new ImplerException("Can't write in " + outputFile, e);
        }
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg == null) {
                System.out.println("Arg(s) is null");
            }
        }
        Impler implementor = new Implementor();
        try {
            implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ClassNotFoundException e) {
            System.out.println("Couldn't find class: " + e.getMessage());
        } catch (ImplerException e) {
            System.out.println("Couldn't implement: " + e.getMessage());
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
        }
    }
}
