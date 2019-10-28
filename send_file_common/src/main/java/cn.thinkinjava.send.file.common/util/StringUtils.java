package cn.thinkinjava.send.file.common.util;

/**
 * 避免 apache common lang 3.
 */
public class StringUtils {

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
}
