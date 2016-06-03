package io.fabric8.profiles;

/**
 * Legal argument assertions
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 */
public final class IllegalArgumentAssertion {

    // hide ctor
    private IllegalArgumentAssertion() {
    }

    /**
     * Throws an IllegalArgumentException when the given value is null.
     */
    public static <T> T assertNotNull(T value, String name) {
        if (value == null)
            throw new IllegalArgumentException("Null " + name);

        return value;
    }

    /**
     * Throws an IllegalArgumentException when the given value is not true.
     */
    public static Boolean assertTrue(Boolean value, String message) {
        if (!Boolean.valueOf(value))
            throw new IllegalArgumentException(message);
        return value;
    }

    /**
     * Throws an IllegalArgumentException when the given value is not false.
     */
    public static Boolean assertFalse(Boolean value, String message) {
        if (Boolean.valueOf(value))
            throw new IllegalArgumentException(message);
        return value;
    }
}