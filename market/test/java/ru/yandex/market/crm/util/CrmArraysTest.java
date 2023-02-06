package ru.yandex.market.crm.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CrmArraysTest {

    @Test
    public void isEmpty_empty() {
        boolean result = CrmArrays.isEmpty(new Object[0]);
        Assertions.assertTrue(result);
    }

    @Test
    public void isEmpty_notEmpty() {
        boolean result = CrmArrays.isEmpty(new Object[1]);
        Assertions.assertFalse(result);
    }

    @Test
    public void isEmpty_null() {
        boolean result = CrmArrays.isEmpty(null);
        Assertions.assertTrue(result);
    }

    @Test
    public void nonEmpty_empty() {
        boolean result = CrmArrays.nonEmpty(new Object[0]);
        Assertions.assertFalse(result);
    }

    @Test
    public void nonEmpty_notEmpty() {
        boolean result = CrmArrays.nonEmpty(new Object[1]);
        Assertions.assertTrue(result);
    }

    @Test
    public void nonEmpty_null() {
        boolean result = CrmArrays.nonEmpty(null);
        Assertions.assertFalse(result);
    }

    @Test
    public void transform() {
        String[] value = new String[]{"5", "7"};
        Long[] result = CrmArrays.transform(value, Long.class, Long::valueOf);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(5l, (long) result[0]);
        Assertions.assertEquals(7l, (long) result[1]);
    }

    @Test
    public void transformLong() {
        String[] value = new String[]{"5", "7"};
        long[] result = CrmArrays.transform(value, Long::parseLong);
        Assertions.assertEquals(2, result.length);
        Assertions.assertEquals(5, result[0]);
        Assertions.assertEquals(7, result[1]);
    }

    @Test
    public void transformLong_null() {
        long[] result = CrmArrays.transform((String[]) null, Long::parseLong);
        Assertions.assertNull(result);
    }

    @Test
    public void transform_null() {
        Long[] result = CrmArrays.transform((String[]) null, Long.class, Long::valueOf);
        Assertions.assertNull(result);
    }

    @Test
    public void concat() {
        String[] a = new String[]{"a", "b"};
        String[] b = new String[]{"b", "c", "d"};

        String[] result = CrmArrays.concat(String.class, a, b);

        Assertions.assertArrayEquals(new String[]{"a", "b", "b", "c", "d"}, result);
    }

    @Test
    public void concat_null_null() {
        String[] a = null;
        String[] b = null;

        String[] result = CrmArrays.concat(String.class, a, b);

        Assertions.assertArrayEquals(new String[0], result);
    }

    @Test
    public void concat_null_notNull() {
        String[] a = null;
        String[] b = new String[]{"a", "b"};

        String[] result = CrmArrays.concat(String.class, a, b);

        Assertions.assertArrayEquals(new String[]{"a", "b"}, result);
    }

    @Test
    public void concat_notNull_null() {
        String[] a = new String[]{"a", "b"};
        String[] b = null;

        String[] result = CrmArrays.concat(String.class, a, b);

        Assertions.assertArrayEquals(new String[]{"a", "b"}, result);
    }
}
