package ru.yandex.crypta.graph2.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HashUtilsTest {


    public static final String VALUE = "fdsfsdfsdfsdf";

    @Test
    public void arcadiaCompatibleMurmur64() {
        long hash = HashUtils.arcadiaCompatibleMurmurInt64(VALUE);
        assertEquals(-4167849143107451681L, hash);
    }

    @Test
    public void arcadiaCompatibleMurmurUint64String() {
        String hash = HashUtils.arcadiaCompatibleMurmurUint64String(VALUE);
        assertEquals("14278894930602099935", hash);
    }
}
