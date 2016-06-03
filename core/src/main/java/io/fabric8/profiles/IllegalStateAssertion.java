package io.fabric8.profiles;

/**
 * Legal state assertions
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public final class IllegalStateAssertion {

    // hide ctor
    private IllegalStateAssertion() {
    }

    /**
     * Throws an IllegalStateException when the given value is not null.
     * @return the value
     */
    public static <T> T assertNull(T value, String message) {
        if (value != null)
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is null.
     * @return the value
     */
    public static <T> T assertNotNull(T value, String message) {
        if (value == null)
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is not true.
     */
    public static Boolean assertTrue(Boolean value, String message) {
        if (!Boolean.valueOf(value))
            throw new IllegalStateException(message);

        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is not false.
     */
    public static Boolean assertFalse(Boolean value, String message) {
        if (Boolean.valueOf(value))
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     */
    public static <T> T assertEquals(T exp, T was, String message) {
        assertNotNull(exp, message);
        assertNotNull(was, message);
        assertTrue(exp.equals(was), message);
        return was;
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     */
    public static <T> T assertSame(T exp, T was, String message) {
        assertNotNull(exp, message);
        assertNotNull(was, message);
        assertTrue(exp == was, message);
        return was;
    }
}