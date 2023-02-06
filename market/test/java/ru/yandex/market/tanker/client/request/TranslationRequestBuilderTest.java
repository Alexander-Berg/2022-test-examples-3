package ru.yandex.market.tanker.client.request;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.tanker.client.model.Language;

import static org.junit.Assert.assertEquals;

/**
 * @author Vadim Lyalin
 */
public class TranslationRequestBuilderTest {
    private TranslationRequestBuilder builder;

    @Before
    public void setUp() {
        this.builder = new TranslationRequestBuilder();
    }

    @Test
    public void testBuild() {
        final String query = builder.withBranch("master")
                .withUnapproved()
                .withLanguage(Language.RU)
                .build();

        assertEquals("?branch-id=master&status=unapproved&language=ru", query);
    }

}
