package io.tals.flux4j.apt;

import com.google.common.base.Joiner;

/**
 * @author Tal Shani
 */
final class StringUtils {
    public static String joinParams(String... params) {
        return Joiner.on(", ").join(params);
    }
}
