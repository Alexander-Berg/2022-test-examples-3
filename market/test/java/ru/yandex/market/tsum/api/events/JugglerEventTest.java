package ru.yandex.market.tsum.api.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;
import ru.yandex.market.tsum.api.juggler.JugglerEvent;

import java.io.IOException;

public class JugglerEventTest {

    @Test
    public void testJsonParsing() {
        try {
            String testJson = readFile("test_json.json");

            ObjectMapper mapper = new ObjectMapper();
            mapper.readValue(testJson, JugglerEvent.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String readFile(String path) throws IOException {
        return Resources.toString(Resources.getResource(path), Charsets.UTF_8);
    }
}
