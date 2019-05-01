package ru.ifmo.rain.krotkov.implementor;

import info.kgeorgiy.java.advanced.implementor.BaseImplementorTest;
import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * A class that implements classes and interfaces.
 */
public class Implementor implements Impler, JarImpler {


    /**
     * Default gap called 'tab' represented by four spaces
     */
    private static final String TAB = "    ";

    /**
     * Create instance of {@link Implementor}
     */
    public Implementor() {

    }

    /**
     * Returns the {@link java.nio.file.Path} to the implementation of token relative from the root.
     * Implementation of token has suffix "Impl".
     *
     * @param token type token to create the implementation's path to
     * @param root  root directory
     * @param end   if true then add .java to the suffix of path
     *              if false then add .class to the suffix of path
     * @return path to the implementation of token starting from root
     */
    private static Path getFilePath(Class<?> token, Path root, boolean end) {
        root = getFileDirectory(token, root);
        return root.resolve(getImplClassName(token) + (end ? ".java" : ".class"));
    }

    /**
     * Creates the {@link Path} to the token relative from the root.
   ~  * The return value is same as call getFilePath(token, root, true) but if the path does not exist
     * the directories in it are created.
     *
     * @param token type token to create the path to
     * @param root  root directory
     * @return path to the implementation of token starting from root.
     * @throws IOException if an I/O error occurs during the creating directories
     */
    private static Path createJavaDirectory(Class<?> token, Path root) throws IOException {
        root = getFileDirectory(token, root);
        Files.createDirectories(root);
        return root.resolve(getImplClassName(token) + ".java");
    }

    /**
     * Return the {@link Path} to the directory of implementation of token starting in root.
     *
     * @param token type token to create the path of directory to
     * @param root  root directory
     * @return the {@link Path} to the directory of implementation of token starting in root.
     */
    private static Path getFileDirectory(Class<?> token, Path root) {
        if (token.getPackage() != null) {
            root = root.resolve(token.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar);
        }
        return root;
    }

    /**
     * Returns name of class which is an implementation of token.
     *
     * @param token type token to create implementation for
     * @return the name of implementation of token
     */
    private static String getImplClassName(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Produces code implementing class or interface specified by provided token.
     * <p>
     * Generated class full name is same as full name of the type token with "Impl" suffix
     * added. Generated source code is placed in the correct subdirectory of the specified
     * root directory and have correct file name.
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException when implementation cannot be generated
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null || root == null) {
            throw new ImplerException("Passed arguments are incorrect");
        }

        if (token.isPrimitive() || token.isArray() || token == Enum.class || Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Class token is incorrect");
        }

        try (Writer writer = Files.newBufferedWriter(createJavaDirectory(token, root))) {
            writeClassFile(token, writer);
        } catch (IOException e) {
            throw new ImplerException(e);
        }

    }

    /**
     * Writes implementation of token by writer
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException     if an I/O error occurs
     * @throws ImplerException if implementation cannot be generated
     */
    private void writeClassFile(Class<?> token, Writer writer) throws IOException, ImplerException {
        writePackage(token, writer);
        writeClassName(token, writer);
        writeConstructors(token, writer);
        writeMethods(token, writer);
        writer.write("}\n");
    }

    /**
     * Writes information about token's implementation package by writer.
     * If token.getPackage() is null then this method does nothing.
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException if an I/O error occurs
     */
    private void writePackage(Class<?> token, Writer writer) throws IOException {
        if (token.getPackage() != null) {
            writer.write("package " + toUnicode(token.getPackage().getName()) + ";\n\n");
        }
    }

    /**
     * Represent in's chars to unicode escape.
     * If char code is not less than 128. Otherwise char is not changed.
     *
     * @param in input {@link String}
     * @return string with unicode escapes
     */
    private static String toUnicode(String in) {
        StringBuilder b = new StringBuilder();

        for (char c : in.toCharArray()) {
            if (c >= 128)
                b.append("\\u").append(String.format("%04X", (int) c));
            else
                b.append(c);
        }

        return b.toString();
    }

    /**
     * Writes information about token's implementation name by writer.
     * Implementation class have same modifiers as token except abstract and interface
     * (implementation does not have these modifiers).
     * Also it writes the '{' and two '\n' symbols in the end.
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException if an I/O error occurs
     */
    private void writeClassName(Class<?> token, Writer writer) throws IOException {
        writer.write(
                Modifier.toString(token.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.INTERFACE)
                        + " class "
                        + toUnicode(getImplClassName(token))
                        + (token.isInterface() ? " implements " : " extends ")
                        + toUnicode(token.getSimpleName())
                        + " {\n\n"
        );
    }

    /**
     * Writes implementation of constructors of token's implementation by writer.
     * Private constructors are ignored. Also token have to have at least one non-private
     * declared constructor or it has to be an interface. Otherwise, this method throws an ImplerException.
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException     if an I/O error occurs
     * @throws ImplerException if token is not an interface and it has only private declared constructors
     */
    private void writeConstructors(Class<?> token, Writer writer) throws IOException, ImplerException {
        boolean hasNonPrivateConstructors = false;
        for (Constructor<?> constructor : token.getDeclaredConstructors()) {
            if (Modifier.isPrivate(constructor.getModifiers())) {
                continue;
            }
            hasNonPrivateConstructors = true;
            writeExecutable(constructor, writer, token);
            writeConstructorImpl(constructor, writer);
        }
        if (!hasNonPrivateConstructors && !token.isInterface()) {
            throw new ImplerException("Has no non-private constructor");
        }
    }

    /**
     * Writes signature of implementation of executable by writer.
     * Signature includes also what executable throws. This method does not write '{' in the end however it
     * writes space after list of exceptions. Modifiers in the result signature are same with executable's modifiers
     * except abstract and transient (implementation does not have these modifiers).     *
     *
     * @param executable {@link Executable} of token which this method declares
     * @param writer     a writer
     * @param token      type token to create implementation for
     * @throws IOException if an I/O error occurs
     */
    private void writeExecutable(Executable executable, Writer writer, Class<?> token) throws IOException {
        writer.write(TAB
                + Modifier.toString(executable.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT)
                + " "
                + (executable instanceof Constructor ? "" : toUnicode(((Method) executable).getReturnType().getCanonicalName()) + " ")
                + (executable instanceof Constructor ? toUnicode(getImplClassName(token)) : toUnicode(executable.getName()))
                + "("
        );

        Parameter[] parameters = executable.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            writer.write((i == 0 ? "" : ", ") + toUnicode(parameters[i].getType().getCanonicalName()) + " " + toUnicode(parameters[i].getName()));
        }
        writer.write(")");

        Class<?> exceptions[] = executable.getExceptionTypes();
        for (int i = 0; i < exceptions.length; i++) {
            writer.write((i == 0 ? " throws " : ", ") + toUnicode(exceptions[i].getCanonicalName()));
        }
    }

    /**
     * Writes body of constructor by writer.
     * The body starts with '{' and ends with '}'. The implementation of constructor is calling super() with
     * arguments of the constructor.
     *
     * @param constructor {@link Constructor} which this method defines
     * @param writer      a writer
     * @throws IOException if an I/O error occurs
     */
    private void writeConstructorImpl(Constructor<?> constructor, Writer writer) throws IOException {
        writer.write("{\n" + TAB + TAB);
        writer.write("super(");
        Parameter[] parameters = constructor.getParameters();
        if (parameters.length == 0) {
            writer.write(");");
        }
        for (int i = 0; i < parameters.length; i++) {
            writer.write(toUnicode(parameters[i].getName()));
            writer.write((i == parameters.length - 1 ? ");" : ", "));
        }
        writer.write("\n" + TAB + "}\n\n");
    }

    /**
     * Writes implementation of methods of token's implementation by writer.
     * This method implements only abstract method of the token and its ancestors if they were not implemented earlier.
     *
     * @param token  type token to create implementation for
     * @param writer a writer
     * @throws IOException if an I/O error occurs
     */
    private void writeMethods(Class<?> token, Writer writer) throws IOException {
        Set<MethodWrapper> set = new HashSet<>();
        getAbstractMethods(token.getMethods(), set);
        Class<?> ancestor = token;
        while (ancestor != null) {
            getAbstractMethods(token.getDeclaredMethods(), set);
            ancestor = ancestor.getSuperclass();
        }
        for (MethodWrapper wrapper : set) {
            writeExecutable(wrapper.getMethod(), writer, token);
            writeMethodImpl(wrapper.getMethod(), writer);
        }
    }

    /**
     * Put abstract methods from methods to set.
     *
     * @param methods array of methods
     * @param set     contains all abstract methods of methods after calling this method
     */
    private void getAbstractMethods(Method[] methods, Set<MethodWrapper> set) {
        for (Method method : methods) {
            if (Modifier.isAbstract(method.getModifiers())) {
                set.add(new MethodWrapper(method));
            }
        }
    }

    /**
     * Wrapper of {@link Method}
     */
    private static class MethodWrapper {
        /**
         * instance of {@link Method} wrapped method
         */
        private final Method method;

        /**
         * Returns wrapped method.
         *
         * @return wrapped method
         */
        Method getMethod() {
            return method;
        }

        /**
         * Construct {@link MethodWrapper} which wraps method.
         *
         * @param method wrapped method
         */
        MethodWrapper(Method method) {
            this.method = method;
        }

        /**
         * Returns hash code of wrapped method. It depends only on its name and parameters.
         *
         * @return hash code of wrapped method
         */
        @Override
        public int hashCode() {
            Parameter[] parameters = method.getParameters();
            int hashCode = Integer.hashCode(parameters.length) * 43 + method.getName().hashCode();
            for (Parameter parameter : parameters) {
                hashCode = hashCode * 43 + parameter.toString().hashCode();
            }
            return hashCode;
        }

        /**
         * Check if {@link Object} obj is equal to this. If obj is null or it is not instance of MethodWrapper
         * then obj is not equal to this. Otherwise, if obj and this have equal name,
         * parameter types and return type then they are equal. Otherwise, they are not.
         *
         * @param obj {@link Object} comparing to this
         * @return true if obj and this are equal and false otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof MethodWrapper) {
                MethodWrapper other = (MethodWrapper) obj;
                return method.getName().equals(other.method.getName())
                        && Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes())
                        && method.getReturnType().equals(other.method.getReturnType());
            }
            return false;
        }
    }

    /**
     * Writes body of method by writer.
     * The body starts with '{' and ends with '}'. The implementation of method is returning defaultReturnType.
     *
     * @param method {@link Method} which this method defines
     * @param writer a writer
     * @throws IOException if an I/O error occurs
     * @see #getDefaultReturnType(Class)
     */
    private void writeMethodImpl(Method method, Writer writer) throws IOException {
        writer.write("{\n" + TAB + TAB);
        writer.write("return" + getDefaultReturnType(method.getReturnType()) + ";");
        writer.write("\n" + TAB + "}\n\n");
    }

    /**
     * Returns string with default value of type. If type is void it returns empty string.
     * If it is boolean then it returns " true". If it is primitive
     * method returns " 0". Otherwise, it returns " null".
     *
     * @param type type which of default value method returns
     * @return string with default value of type with space in beginning (if type is not void)
     */
    private String getDefaultReturnType(Class<?> type) {
        if (type.equals(void.class)) {
            return "";
        } else if (type.equals(boolean.class)) {
            return " true";
        } else if (type.isPrimitive()) {
            return " 0";
        } else {
            return " null";
        }
    }

    /**
     * Compile class which has {@link Path} javaFileName relatively to root.
     *
     * @param root         root directory
     * @param javaFileName name of .java file for compiling
     * @throws ImplerException if compilation can not be done
     */
    private void compileClass(Path root, Path javaFileName) throws ImplerException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        if (javaCompiler == null) {
            throw new ImplerException("Exception: compiler not found");
        }
        int returnCode = javaCompiler.run(null, null, null,
                javaFileName.toString(),
                "-cp", root + File.pathSeparator + getClassPath()
        );
        if (returnCode != 0) {
            throw new ImplerException("Exception when compiling");
        }
    }


    /**
     * Return class path
     *
     * @return string with class path
     */
    private static String getClassPath() {
        try {
            return Path.of(BaseImplementorTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Creates and writes .jar file by {@link Path} of jarFile relatively to root.
     * Resulting .jar contains {@link Manifest} with attribute MANIFEST_VERSION. Also it contains compiled
     * .class file of token.
     *
     * @param token   type token which compiled class zipping in .jar
     * @param root    root directory
     * @param jarFile target .jar file.
     * @throws ImplerException if creation .jar can not be done
     */
    private void writeJarFile(Class<?> token, Path root, Path jarFile) throws ImplerException {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            writer.putNextEntry(new ZipEntry(token.getName().replace('.', '/') + "Impl.class"));
            Files.copy(getFilePath(token, root, false), writer);
        } catch (IOException e) {
            throw new ImplerException("Unable to create JAR file", e);
        }
    }

    /**
     * Produces ".jar" file implementing class or interface specified by provided token.
     * <p>
     * Generated class full name is same as full name of the type token with "Impl" suffix
     * added.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target ".jar" file.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Path temp;
        try {
            temp = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "tmp");
        } catch (IOException e) {
            throw new ImplerException("Can't create temporary directory", e);
        }
        implement(token, temp);
        compileClass(temp, getFilePath(token, temp, true));
        writeJarFile(token, temp, jarFile);
    }

    /**
     * Entry point of the program.
     * <p>
     * Usage:
     * <ul>
     * <li>{@code java -jar Implementor.jar -jar class-to-implement path-to-jar}</li>
     * <li>{@code java -jar Implementor.jar class-to-implement path-to-class}</li>
     * </ul>
     *
     * @param args command line arguments.
     * @see Implementor
     */
    public static void main(String[] args) {
        if (args == null || (args.length != 2 && args.length != 3)) {
            System.out.println("Wrong amount of arguments. Expected 2 or 3.");
            return;
        }
        for (String arg : args) {
            if (arg == null) {
                System.out.println("Not-null arguments were expected");
                return;
            }
        }
        try {
            Implementor implementor = new Implementor();
            if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            }
        } catch (InvalidPathException e) {
            System.out.println("Incorrect path to root in input: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Incorrect class name in input: " + e.getMessage());
        } catch (ImplerException e) {
            System.out.println("Exception was thrown during the implementation: " + e.getMessage());
        }
    }
}