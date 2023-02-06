package ru.yandex.market.logshatter.parser.front.errorBooster.redirlog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlockstatDecoderTest {

    @Test
    public void decode() {
        BlockstatDecoder blockstatDecoder = new BlockstatDecoder();

        assertEquals("tech.client_error", blockstatDecoder.decode("690.2354"));
        assertEquals("tech", blockstatDecoder.decode("690"));
        assertEquals("ad.tab.news.market.adresa.slovari.blogs.image.active.on",
            blockstatDecoder.decode("1.2.3.4.5.6.7.8.9.10"));
        assertEquals("tech", blockstatDecoder.decode("tech"));
        assertEquals("tech690", blockstatDecoder.decode("tech690"));
        assertEquals("tech.tech", blockstatDecoder.decode("tech.690"));
    }
}
