package ru.yandex.market.tanker.client.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Vadim Lyalin
 */
public class KeySetTranslationTest {

    @Test
    public void testStandardSerialization() {
        final KeySetTranslation origin = new KeySetTranslation(new HashMap<>());
        final KeySetTranslation clone = SerializationUtils.clone(origin);

        assertThat(clone, notNullValue());
    }

    @Test
    public void testJsonDeserialization() throws IOException {
        String json = "{ru: {" +
                "       title: \"Заголовки\"," +
                "       письмо:[\"письмо\", \"письма\", \"писем\"]" +
                "}" +
                "}";
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        KeySetTranslation translation = objectMapper.readValue(json, KeySetTranslation.class);
        assertEquals("Заголовки", translation.getKeySet(Language.RU).getText("title"));
        assertEquals(Arrays.asList("письмо", "письма", "писем"), translation.getKeySet(Language.RU).getTexts("письмо"));
    }

}
