/**
 * Implementation of {@link info.kgeorgiy.java.advanced.implementor.Impler}
 * and {@link info.kgeorgiy.java.advanced.implementor.JarImpler} interfaces.
 *
 * @author Frak
 * @version 1.0
 */

module ru.ifmo.rain.zhuvertcev.implementor {
    requires java.compiler;
    requires info.kgeorgiy.java.advanced.implementor;

    opens ru.ifmo.rain.zhuvertcev.implementor;
    exports ru.ifmo.rain.zhuvertcev.implementor;
}