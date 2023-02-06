package ru.yandex.crypta.search.util;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YabsHashTest {
    @Test
    public void testHalfMd5() {
        String longUa = "Mozilla/5.0 (Linux; Android 9; LLD-AL20 Build/HONORLLD-AL20; wv) AppleWebKit/537.36 (KHTML, like " +
                "Gecko) Version/4.0 Chrome/91.0.4472.120 Mobile Safari/537.36";

        assertEquals("1798646558533009647", YabsHash.calcHalfMd5Str(longUa.getBytes(StandardCharsets.UTF_8)));
        assertEquals("14962409547272865538", YabsHash.calcHalfMd5Str("s".getBytes(StandardCharsets.UTF_8)));
        assertEquals("7203772011789518145", YabsHash.calcHalfMd5Str("".getBytes(StandardCharsets.UTF_8)));
    }
}
