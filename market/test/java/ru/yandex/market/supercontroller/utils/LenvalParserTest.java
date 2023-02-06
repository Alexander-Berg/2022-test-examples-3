package ru.yandex.market.supercontroller.utils;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mbo.http.OffersStorage;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author mkrasnoperov
 */
public class LenvalParserTest {

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @Test
    public void parsePicture() throws Exception {
        byte[] bytes = hexStringToByteArray(
            "b10000000a10d5a6763a3e580294da5e00a5fbe0917a10ffff8f80808080804018900320900328fea60e32830108031264600000" +
                "00c19a67cb4c7cb46ec0b3cb6d9467876f316440a199696a788d5c77a4965dc55a56986491a2b386608e6d716f7657707175" +
                "a75b6cb592a1925d416169617e7647855ebd5c946881bfb66e807d946766a98c7b78827390cb588e8a7f5d5d8da06e1a1402" +
                "0000006200000038f8e23e430000007b832f3e25000000004201534a00b10000000a103ebe0e5ec234c164c4582aae5e6e40" +
                "7910ffff8f80808080804018e80720e8072899810f3283010803126460000000c09b66c94e78b672c6aecc6b956582703064" +
                "43a19b6c6c7b945b7ca49b5fc75c569a608da3ae7b628e66796b7558737074a65a73b68da3905d3f61645e7f7848845ebe5d" +
                "926580bfb6717d7d936c67ac8b7a7a80728ac8578a8d7c575c899b6f1a140200000062000000f0a7c63e4300000017d94e3e" +
                "25000000004201534a00");
        OffersStorage.PictureMeta x = LenvalParser.parsePicture(bytes);
        assertEquals(2, x.getPictureCount());
    }

}