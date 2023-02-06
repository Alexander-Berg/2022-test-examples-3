package ru.yandex.market.fulfillment.wrap.marschroute.model.base;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import static java.lang.ClassLoader.getSystemResourceAsStream;

class MarschrouteDimensionsTest extends ParsingTest<MarschrouteDimensions> {

    MarschrouteDimensionsTest() {
        super(new ObjectMapper(), MarschrouteDimensions.class, "dimensions/good.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.of(
                "height", 10,
                "width", 12,
                "depth", 13
        );
    }

    @Test
    void testWrongArrayParsingTest() throws Exception {
        softly.assertThatThrownBy(() -> mapper.readValue(
                getSystemResourceAsStream("dimensions/bad.json"),
                MarschrouteDimensions.class
        )).isInstanceOf(JsonMappingException.class);
    }

    @Test
    void testWrongTypeParsingTest() throws Exception {
        softly.assertThatThrownBy(() -> mapper.readValue(
                getSystemResourceAsStream("dimensions/ugly.json"),
                MarschrouteDimensions.class
        )).isInstanceOf(JsonMappingException.class);
    }


}
