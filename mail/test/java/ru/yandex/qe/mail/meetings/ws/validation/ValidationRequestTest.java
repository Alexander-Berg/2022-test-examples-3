package ru.yandex.qe.mail.meetings.ws.validation;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.qe.mail.meetings.api.resource.dto.SwapResourceRequest;
import ru.yandex.qe.mail.meetings.utils.FormConverters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.qe.mail.meetings.utils.StringUtils.trim;

public class ValidationRequestTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    public void serializationCreateEvent() throws IOException {
        String requestString = trim(IOUtils.readLines(
                ValidationRequestTest.class.getResourceAsStream("/forms/create-event.json"), StandardCharsets.UTF_8));

        final ValidationRequest validationRequest = MAPPER.readValue(requestString, ValidationRequest.class);
        assertEquals("Value_1", validationRequest.getAnswer("action_type"));
        assertEquals("Value_2_0,Value_2_1,Value_2_2", validationRequest.getAnswer("meeting_type"));
        assertEquals("False", validationRequest.getAnswer("book_now"));
        assertEquals("2", validationRequest.getAnswer("ttl_booking"));
        assertNull(validationRequest.getAnswer("Name_2"));
    }

    @Test
    public void serializationSwap() throws IOException {
        String requestString = trim(IOUtils.readLines(
                ValidationRequestTest.class.getResourceAsStream("/forms/swap-resource.json"), StandardCharsets.UTF_8));
        final ValidationRequest validationRequest = MAPPER.readValue(requestString, ValidationRequest.class);

        assertEquals("Обмен переговорками", validationRequest.getAnswer("action_type"));
        assertEquals("https://calendar.yandex-team.ru/event/52132905", validationRequest.getAnswer("targetCalendarUrl"
        ));
        assertEquals("https://calendar.yandex-team.ru/event/52132895", validationRequest.getAnswer("sourceCalendarUrl"
        ));
        assertEquals("Санкт-Петербург, БЦ Бенуа,Москва, Морозов", validationRequest.getAnswer("offices"));
        assertEquals("True", validationRequest.getAnswer("moveResource"));

        final Map<String, Object> fromValue = validationRequest.toQuestionsMap();
        SwapResourceRequest swapResourceRequest = MAPPER.convertValue(fromValue, SwapResourceRequest.class);
        assertEquals("https://calendar.yandex-team.ru/event/52132895", swapResourceRequest.getSourceCalendarUrl());
        assertEquals("https://calendar.yandex-team.ru/event/52132905", swapResourceRequest.getTargetCalendarUrl());
        assertEquals("True", swapResourceRequest.getMoveResource());
        assertEquals("Санкт-Петербург, БЦ Бенуа,Москва, Морозов", swapResourceRequest.getOffices());
    }

    @Test
    public void serialization_one() throws IOException {
        final Map<String, JsonNode> slug = Map.of(
                "slug", new TextNode("action_type"),
                "value", new TextNode(FormConverters.SWAP_LABEL));
        List<JsonNode> questions = List.of(
                new ObjectNode(JsonNodeFactory.instance, slug)
        );
        ValidationRequest requestStart = new ValidationRequest(1, "Slug", "Name", questions);
        final StringWriter stringWriter = new StringWriter();
        MAPPER.writeValue(stringWriter, requestStart);
        final String content = stringWriter.toString();
        final ValidationRequest requestEnd = MAPPER.readValue(content, ValidationRequest.class);
        assertEquals(requestStart, requestEnd);

    }

    @Test
    public void serialization_two() throws IOException {
        List<JsonNode> questions = List.of(
                new ObjectNode(JsonNodeFactory.instance,
                        Map.of(
                                "slug", new TextNode("action_type"),
                                "value", new TextNode(FormConverters.SWAP_LABEL))),
                new ObjectNode(JsonNodeFactory.instance,
                        Map.of(
                                "slug", new TextNode("offices"),
                                "value", new ArrayNode(JsonNodeFactory.instance,
                                        List.of(new TextNode("test1"), new TextNode("test2"))))
                )
        );
        ValidationRequest requestStart = new ValidationRequest(1, "Slug", "Name", questions);
        final StringWriter stringWriter = new StringWriter();
        MAPPER.writeValue(stringWriter, requestStart);
        final String content = stringWriter.toString();
        final ValidationRequest requestEnd = MAPPER.readValue(content, ValidationRequest.class);
        assertEquals(requestStart, requestEnd);

    }

}
