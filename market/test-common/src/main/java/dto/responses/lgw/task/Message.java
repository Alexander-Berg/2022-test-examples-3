package dto.responses.lgw.task;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dto.responses.lgw.LgwTaskFlow;

public class Message {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @JsonProperty("text")
    private String text;

    public <T> T getText(LgwTaskFlow lgwTaskFlow) {
        try {
            return (T) OBJECT_MAPPER.readValue(text, lgwTaskFlow.getClazz());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
