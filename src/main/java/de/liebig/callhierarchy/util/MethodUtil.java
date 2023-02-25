package de.liebig.callhierarchy.util;

public class MethodUtil {

    public static String buildMethodKey(final String name, final String desc) {
        return String.format("%s#%s", name, desc);
    }
}
