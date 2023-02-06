package ru.yandex.market.pers.notify.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author semin-serg
 */
public class PictureUtilTestNoMockedDb {

    private static final String PICTURE_URL_NO_PROTOCOL = "//yastatic.net/lego/_/La6qi18Z8LwgnZdsAr1qy1GwCwo.jpg";
    private static final String PICTURE_URL_HTTP_PROTOCOL = "http:" + PICTURE_URL_NO_PROTOCOL;
    private static final String PICTURE_URL_HTTPS_PROTOCOL = "https:" + PICTURE_URL_NO_PROTOCOL;

    @Test
    public void ensureProtoExistsInUrlTest1() {
        ensureProtoExistsInUrlTest(PICTURE_URL_HTTP_PROTOCOL, PICTURE_URL_HTTP_PROTOCOL);
    }

    @Test
    public void ensureProtoExistsInUrlTest2() {
        ensureProtoExistsInUrlTest(PICTURE_URL_HTTPS_PROTOCOL, PICTURE_URL_HTTPS_PROTOCOL);
    }

    @Test
    public void ensureProtoExistsInUrlTest3() {
        ensureProtoExistsInUrlTest(PICTURE_URL_HTTPS_PROTOCOL, PICTURE_URL_NO_PROTOCOL);
    }

    protected static void ensureProtoExistsInUrlTest(String expectedResultUrl, String sourceUrl) {
        String resultUrl = PictureUtil.ensureProtoExistsInUrl(sourceUrl);
        assertEquals(expectedResultUrl, resultUrl);
    }

}
