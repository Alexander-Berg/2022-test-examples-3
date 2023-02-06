package ru.yandex.market.yt.matchers;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Matcher;

import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчеры для {@link JsonNode}.
 *
 * @author vbudnev
 */
public class JsonNodeMatchers {
    private JsonNodeMatchers() {
    }

    public static Matcher<JsonNode> hasStrValue(String key, String expectedValue) {
        return MbiMatchers.<JsonNode>newAllOfBuilder()
                .add(x -> x.get(key).asText(), expectedValue, "value for key=" + key)
                .build();
    }

    public static Matcher<JsonNode> hasIntValue(String key, Integer expectedValue) {
        return MbiMatchers.<JsonNode>newAllOfBuilder()
                .add(x -> x.get(key).asInt(), expectedValue, "value for key=" + key)
                .build();
    }
}
