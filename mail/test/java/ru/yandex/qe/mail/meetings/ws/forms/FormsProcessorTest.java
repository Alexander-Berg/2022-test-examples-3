package ru.yandex.qe.mail.meetings.ws.forms;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.cron.actions.MockConfiguration;
import ru.yandex.qe.mail.meetings.utils.FormConverters;
import ru.yandex.qe.mail.meetings.ws.FormProcessor;
import ru.yandex.qe.mail.meetings.ws.validation.ValidationRequest;
import ru.yandex.qe.mail.meetings.ws.validation.ValidationResult;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Galyamichev
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class FormsProcessorTest {
    @Inject
    private FormProcessor formProcessor;

    @Test
    public void testEmptySwapRequestValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.SWAP_LABEL)
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.Status.ERROR, formProcessor.validate(request).getStatus());
    }

    @Test
    public void testSwapRequestValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.SWAP_LABEL),
                makeQuestion("sourceCalendarUrl", "https://calendar.yandex-team.ru/event/43500584"),
                makeQuestion("targetCalendarUrl", "https://calendar.yandex-team.ru/event/43500585"),
                makeQuestion("offices", "Санкт-Петербург, БЦ Бенуа")
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.success(), formProcessor.validate(request));
    }

    @Test
    public void testMoveRequestValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.SWAP_LABEL),
                makeQuestion("sourceCalendarUrl", "https://calendar.yandex-team.ru/event/43500583"),
                makeQuestion("targetCalendarUrl", "https://calendar.yandex-team.ru/event/43500585"),
                makeQuestion("moveResource", "Да"),
                makeQuestion("offices", "Санкт-Петербург, БЦ Бенуа")
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.success(), formProcessor.validate(request));
    }

    @Test
    public void testCreateOneRequestValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.BOOK_LABEL),
                makeQuestion("meeting_type", "Разовой"),
                makeQuestion("ttl_booking", "120")
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.success(), formProcessor.validate(request));
    }

    @Test
    public void testCreateRegularRequestValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.BOOK_LABEL),
                makeQuestion("meeting_type", "Регулярной")
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.success(), formProcessor.validate(request));
    }

    @Test
    public void testCreateOneIncorrectRequestValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.BOOK_LABEL),
                makeQuestion("meeting_type", "Разовой")
        );

        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.error("ttl_booking"), formProcessor.validate(request));
    }

    @Test
    public void testTtlValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.BOOK_LABEL),
                makeQuestion("book_now", "Да"),
                makeQuestion("meeting_type", "Разовой")
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.success(), formProcessor.validate(request));
    }

    @Test
    public void testWrongOfficeInSwapRequestValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.SWAP_LABEL),
                makeQuestion("sourceCalendarUrl", "https://calendar.yandex-team.ru/event/43500584"),
                makeQuestion("targetCalendarUrl", "https://calendar.yandex-team.ru/event/43500585"),
                makeQuestion("offices", "Москва, БЦ Морозов")
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.error("offices", "No resources in office"), formProcessor.validate(request));
    }

    @Test
    public void testSameEventSwapRequestValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.SWAP_LABEL),
                makeQuestion("sourceCalendarUrl", "https://calendar.yandex-team.ru/event/43500584"),
                makeQuestion("targetCalendarUrl", "https://calendar.yandex-team.ru/event/43500584"),
                makeQuestion("offices", "Москва, БЦ Морозов")
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.error("targetCalendarUrl", "Same events"), formProcessor.validate(request));
    }

    @Test
    public void testSearchRequestValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.SEARCH_LABEL),
                makeQuestion("calendarUrl", "https://calendar.yandex-team.ru/event/37730976"),
                makeQuestion("ttl", "120")
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.success(), formProcessor.validate(request));
    }

    @Test
    public void testWrongTtlValidation() {
        List<JsonNode> questions = List.of(
                makeQuestion("action_type", FormConverters.SEARCH_LABEL),
                makeQuestion("calendarUrl", "https://calendar.yandex-team.ru/event/37730976"),
                makeQuestion("ttl", "1 hour")
        );
        ValidationRequest request = new ValidationRequest(1, "Slug", "Name", questions);
        assertEquals(ValidationResult.error("ttl", "Value should be numeric"), formProcessor.validate(request));
    }

    public static ObjectNode makeQuestion(String key, String value) {
        return new ObjectNode(JsonNodeFactory.instance,
                Map.of(
                        "slug", new TextNode(key),
                        "value", new TextNode(value)
                )
        );
    }
}
