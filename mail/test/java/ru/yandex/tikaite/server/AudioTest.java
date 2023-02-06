package ru.yandex.tikaite.server;

import org.junit.Test;

import ru.yandex.tikaite.util.Json;

public class AudioTest {
    private static final String AUDIO_WMA = "audio/x-ms-wma";

    @Test
    public void testUnparsedAsf() throws Exception {
        String filename = "unparsed.asf";
        DiskHandlerTest.testJson(
            filename,
            new Json(
                AUDIO_WMA,
                null,
                false,
                null,
                null));
        DiskHandlerTest.testJson(
            filename,
            new Json(
                AUDIO_WMA,
                null,
                false,
                null,
                null),
            "&mimetype=video/x-ms-asf");
    }
}

