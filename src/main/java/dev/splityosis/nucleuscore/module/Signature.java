package dev.splityosis.nucleuscore.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Signature {

    String name();
    String[] authors();
    String description();
    String[] requiredPlugins() default {};
    String[] requiredModules() default {};
    String[] enableAfterModule() default {};
}