package ru.yandex.market.api.partner.context;

import java.util.function.BiConsumer;

import org.springframework.http.MediaType;

import ru.yandex.market.mbi.util.MbiAsserts;

/**
 * @author fbokovikov
 */
public enum Format {
    XML(MbiAsserts::assertXmlEquals, MediaType.APPLICATION_XML),
    JSON(MbiAsserts::assertJsonEquals, MediaType.APPLICATION_JSON);

    private final BiConsumer<String, String> matcher;
    private final MediaType contentType;

    Format(BiConsumer<String, String> matcher, MediaType contentType) {
        this.matcher = matcher;
        this.contentType = contentType;
    }

    public void assertResult(String expected, String actual) {
        matcher.accept(expected, actual);
    }

    public MediaType getContentType() {
        return contentType;
    }

    public String formatName() {
        return name().toLowerCase();
    }
}
