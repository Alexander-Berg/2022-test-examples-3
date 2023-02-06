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
public class ProjectTranslationTest {

    @Test
    public void testStandardSerialization() {
        final ProjectTranslation origin = new ProjectTranslation(new HashMap<>());
        final ProjectTranslation clone = SerializationUtils.clone(origin);

        assertThat(clone, notNullValue());
    }

    @Test
    public void testJsonDeserialization() throws IOException {
        String json = "{ru: {" +
                "   keySet: {" +
                "       title: \"Заголовки\"," +
                "       письмо:[\"письмо\", \"письма\", \"писем\"]" +
                "}" +
                "}" +
                "}";
        ObjectMapper objectMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        ProjectTranslation translation = objectMapper.readValue(json, ProjectTranslation.class);
        assertEquals("Заголовки", translation.getKeysets(Language.RU).getKeySet("keySet").getText("title"));
        assertEquals(Arrays.asList("письмо", "письма", "писем"),
                translation.getKeysets(Language.RU).getKeySet("keySet").getTexts("письмо"));
    }

}
