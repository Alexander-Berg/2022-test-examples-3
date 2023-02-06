package ru.yandex.market.crm.external.contentapi;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.crm.json.serialization.JsonDeserializerImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author apershukov
 */
public class RegionSuggestListParserTest {

    @Test
    public void testParseSuggests() throws IOException {
        // TODO Непонятно что именно будет проверяться таким образом. Логика по большей части задается прямо в тесте
        var deserializer = new JsonDeserializerImpl(
                new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                        .registerModule(new JavaTimeModule())
                        .disable(MapperFeature.AUTO_DETECT_FIELDS)
                        .disable(MapperFeature.AUTO_DETECT_GETTERS)
                        .disable(MapperFeature.AUTO_DETECT_SETTERS)
                        .disable(MapperFeature.AUTO_DETECT_CREATORS)
                        .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        );

        var parser = new RegionSuggestListParser(deserializer);
        var data = IOUtils.readInputStreamToBytes(getClass().getResourceAsStream("geo-suggests.json"));
        var suggests = parser.parse(data).getRegions();

        assertThat(suggests, hasSize(2));

        assertEquals(54, suggests.get(0).getId());
        assertEquals("Екатеринбург", suggests.get(0).getName());
        assertEquals("Екатеринбург (Свердловская область)", suggests.get(0).getFullName());

        assertEquals(104955, suggests.get(1).getId());
        assertEquals("Екабпилс", suggests.get(1).getName());
        assertEquals("Екабпилс (Латвия)", suggests.get(1).getFullName());
    }
}
