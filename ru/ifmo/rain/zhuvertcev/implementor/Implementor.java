package ru.ifmo.rain.zhuvertcev.implementor;

import info.kgeorgiy.java.advanced.implementor.*;

import java.io.BufferedWriter;
import java.io.File;
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
     * Line separator for current Operation system
     */
    private final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Tabulation string
     */
    private final String INDENT = "    ";

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
     *
     * @param implementedClazz this class in implementing
     * @param implementFileString in this {@link StringBuilder} write implementation of current
     * <var>implementedClazz</var>
     * @return {@link StringBuilder} implementation of <var>implementedClazz</var> with name, extends/implements and exceptions
     */
    private StringBuilder getOutClass(Class<?> implementedClazz, StringBuilder implementFileString) {
        StringBuilder before = new StringBuilder("package " + implementedClazz.getPackage().getName() + ";"
                + LINE_SEPARATOR + LINE_SEPARATOR);
        before.append(LINE_SEPARATOR).append("public class ").append(getClassName(implementedClazz)).append(" ");
        if (implementedClazz.isInterface()) {
            before.append("implements ");
        } else {
            before.append("extends ");
        }
        before.append(implementedClazz.getCanonicalName()).append("{").append(LINE_SEPARATOR);
        before.append(implementFileString).append(LINE_SEPARATOR).append("}");
        return before;
    }

    /**
     * Write modifiers of class member except Abstract, Native, Transient
     *
     * @param member member of class, target to write modifiers
     * @param <T> class extends {@link Member}
     * @return {@link StringBuilder} modifiers of <var>member</var> except Abstract,
     * Native, Transient
     */
    private <T extends Member> StringBuilder getModifier(T member) {
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
     * @param executable Method or constructor to implement parameters for <var>implementedClazz</var>
     * @param withTypes if <var>withTypes</var> is 1 write types for parameters
     *                  else don't write types for parameters
     * @return {@link String} parameters of <var>executable</var>
     */
    private String getParameters(Executable executable, boolean withTypes) {
        return List.of(executable.getParameters()).stream().map(parameter -> makeParameter(parameter, withTypes)).
                collect(Collectors.joining(", ", "(", ")"));
    }

    /**
     * Build in java format return type and name for method or constructor.
     *
     * @param executable Method or constructor to implement return type and name for <var>implementedClazz</var>
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
     * @param executable Method to implement body for <var>implementedClazz</var>
     * @return {@link StringBuilder} body of <var>executable</var>
     */
    private StringBuilder returnString(Executable executable) {
        StringBuilder exeString = new StringBuilder(INDENT + INDENT);
        if (executable instanceof Method) {
            exeString.append("return").append(getDefaultValue(((Method) executable).getReturnType()));
        } else {
            exeString.append("super").append(getParameters(executable, false));
        }
        return exeString.append(";");
    }

    /**
     * Build in java format throwing exceptions for method or constructor.
     *
     * @param executable Method to implement exceptions for <var>implementedClazz</var>
     * @return {@link StringBuilder} of exceptions for implemented method
     */
    private StringBuilder getExceptions(Executable executable) {
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
     * @param executable Method or constructor to implement for <var>implementedClazz</var>
     * @return {@link StringBuilder} of implemented method
     */
    private StringBuilder getExecutableBody(Executable executable) {
        StringBuilder methodString = new StringBuilder();
        methodString.append(INDENT).append(getModifier(executable)).append(getReturnTypeAndName(executable))
                .append(getParameters(executable, true)).append(getExceptions(executable)).append(" {").
                append(LINE_SEPARATOR).append(returnString(executable)).
                append(LINE_SEPARATOR).append(INDENT + "}").append(LINE_SEPARATOR).append(LINE_SEPARATOR);
        return methodString;
    }


    /**
     * Implement constructors for abstract class <var>implementedClazz</var>, write them in <var>file</var>
     * <p>
     * Implement only non-private constructors for abstract class <var>implementedClazz</var>, if there are none, throws
     * {@link ImplerException}. Add correct constructors in <var>file</var>
     *
     * @param implementedClazz this class are implementing
     * @param implementFileString in this {@link StringBuilder} write implementation of current
     * @throws ImplerException if <code>constructors.size() == 0 </code>, means has not non-private
     * constructors
     */
    private void implementConstructors(Class<?> implementedClazz, StringBuilder implementFileString) throws ImplerException {
        List<Constructor<?>> constructors = Arrays.stream(implementedClazz.getDeclaredConstructors())
                .filter(constructor -> !Modifier.isPrivate(constructor.getModifiers()))
                .collect(Collectors.toList());
        for (var constructor : constructors) {
            implementFileString.append(getExecutableBody(constructor));
        }
        if (constructors.isEmpty()) {
            throw new ImplerException("Hasn't non-private constructors");
        }
    }

    /**
     * Add only abstract methods in set
     *
     * @param methods methods array
     * @param methodsSet target to add abstract methods
     * @param finalMethods finals method container
     */
    private void addMethodsInSet(Method[] methods, Set<MethodWrapper> methodsSet, final Set<MethodWrapper> finalMethods) {
        for (Method method : methods) {
            MethodWrapper methodWrapper = new MethodWrapper(method);
            if (Modifier.isFinal(method.getModifiers())) {
                finalMethods.add(methodWrapper);
            }
            else if (Modifier.isAbstract(method.getModifiers()) && !finalMethods.contains(methodWrapper)) {
                methodsSet.add(methodWrapper);
            }

        }
    }

    /**
     * Implement methods, write them in <var>file</var>
     *
     * @param clazz Methods of this <var>clazz</var> to write in implement file
     * @param implementFileString in this {@link StringBuilder} write implementation of current
     */
    private void implementMethods(Class<?> clazz, StringBuilder implementFileString) {
        Set<MethodWrapper> abstractMethods = new HashSet<>();
        Set<MethodWrapper> finalMethods = new HashSet<>();
        addMethodsInSet(clazz.getMethods(), abstractMethods, finalMethods);
        while (clazz != null) {
            addMethodsInSet(clazz.getDeclaredMethods(), abstractMethods, finalMethods);
            clazz = clazz.getSuperclass();
        }
        for (MethodWrapper methodWrapper : abstractMethods) {
            implementFileString.append(getExecutableBody(methodWrapper.getMethod()));
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
    protected void checkNotNulls(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("root and class cannot be null");
        }
    }

    /**
     * Try create directories by <var>path</var>
     *
     * @param path all directories by this path should be created
     * @throws ImplerException when function createDirectories couldn't
     * create directories
     */
    protected void createDirectories(Path path) throws ImplerException {
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
    protected String toUTF(String original) {
        char[] chars = original.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : chars) {
            if (c < 128) {
                stringBuilder.append(c);
            } else {
                stringBuilder.append("\\u").append(String.format("%04x", (int) c));
            }
        }
        return stringBuilder.toString();
    }


    /**
     * Get  output file name with path
     *
     * @param implementedClazz this class are implementing
     * @param root root directory.
     * @return Get {@link String} of output file name with path
     */
    protected Path getOutputFile(Class<?> implementedClazz, Path root, String end) {
        String classDirectory = implementedClazz.getPackageName();
        return Paths.get(root.toString() + File.separatorChar + classDirectory.replace('.', File.separatorChar)
                + File.separatorChar + getClassName(implementedClazz) + "." + end);

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
        checkNotNulls(token, root);
        int modifiers = token.getModifiers();
        if (token.isPrimitive() || token.isArray() || token == Enum.class || Modifier.isFinal(modifiers)
                || Modifier.isPrivate(modifiers)) {
            throw new ImplerException("Unable to implement " + token.getCanonicalName());
        }
        StringBuilder implementFileString = new StringBuilder();
        if (!token.isInterface()) {
            implementConstructors(token, implementFileString);
        }
        implementMethods(token, implementFileString);
        Path outputFile = getOutputFile(token, root, "java");
        createDirectories(outputFile);
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            implementFileString = getOutClass(token, implementFileString);
            writer.write(toUTF(implementFileString.toString()));
        } catch (IOException e) {
            throw new ImplerException("Can't write in " + outputFile, e);
        }
    }

    /**
     * Class for comparing {@link Method}
     */
    private static class MethodWrapper {
        /**
         * Method to compare
         */
        private Method method;

        /**
         * Constructor of class
         *
         * @param method current method
         */
        public MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Get current method
         *
         * @return <var>method</var>
         */
        public Method getMethod() {
            return method;
        }

        /**
         * Get hash code of this class
         *
         * @return hash code of <var>method</var>
         */
        @Override
        public int hashCode() {
            return Arrays.hashCode(method.getParameterTypes()) * 3571 + method.getName().hashCode() * 317;
        }

        /**
         * Checks if items are equal
         *
         * @param object target to comparing whit this class
         * @return true if methods are equals, else get false
         */
        @Override
        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object instanceof MethodWrapper) {
                MethodWrapper methodWrapper = (MethodWrapper) object;
                return methodWrapper.getMethod().getName().equals(method.getName()) &&
                        Arrays.equals(methodWrapper.getMethod().getParameterTypes(), method.getParameterTypes());
            }
            return false;
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
