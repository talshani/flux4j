package io.tals.flux4j.shared;

import java.lang.annotation.*;

/**
 * @author Tal Shani
 */
@Retention(RetentionPolicy.SOURCE)
@Documented
@Target(ElementType.TYPE)
public @interface AppDispatcher {
    Class<?>[] stores() default {};
}
