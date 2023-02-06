package ru.yandex.market.yql_test.proxy;

import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class YqlResponseExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private YqlResponseExtractor() {
    }

    public static YqlResponseWrapper extractResponse(byte[] buffer) {
        try {
            JsonNode jsonNode = MAPPER.readTree(buffer);
            JsonNode dataNode = jsonNode.get("data");
            return new YqlResponseWrapper(new String(buffer),
                    jsonNode.get("id").asText(),
                    jsonNode.get("status").asText(),
                    dataNode != null
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to extract data from " + new String(buffer), e);
        }
    }

    public static Optional<String> extractRequestQuery(RepeatableReadRequest request) {
        return request.getBody()
                .flatMap(body -> {
                    try {
                        MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
                        JsonNode jsonNode = MAPPER.readTree(body);
                        JsonNode contentNode = jsonNode.get("content");
                        if (contentNode == null) {
                            return Optional.empty();
                        }
                        return Optional.of(contentNode.asText());
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to extract data from " + new String(body));
                    }
                });
    }
}
