package com.github.talshani.flux4j.shared;

import java.lang.annotation.*;

/**
 * @author Tal Shani
 */
@Retention(RetentionPolicy.SOURCE)
@Documented
@Target(ElementType.METHOD)
public @interface ActionHandler {
    Class<?>[] dependencies() default {};
}
