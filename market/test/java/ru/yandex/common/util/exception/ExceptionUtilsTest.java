package ru.yandex.common.util.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class ExceptionUtilsTest  {
    @Test
    public void testGetNestedException() {
        String m1 = "Test exception level 1";
        String m2 = "Test exception level 2";
        String m3 = "Test exception level 3";

        Exception e = new Exception(m3, new RuntimeException(m2, new IllegalArgumentException(m1)));

        IllegalArgumentException nested = ExceptionUtils.getNestedException(e, IllegalArgumentException.class);
        assertNotNull(nested);
        assertEquals(m1, nested.getMessage());

        ArrayIndexOutOfBoundsException nested2 = ExceptionUtils.getNestedException(e, ArrayIndexOutOfBoundsException.class);
        assertNull(nested2);
    }
}
