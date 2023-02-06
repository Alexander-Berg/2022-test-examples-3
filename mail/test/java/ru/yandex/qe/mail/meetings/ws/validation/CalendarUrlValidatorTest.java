package ru.yandex.qe.mail.meetings.ws.validation;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.cron.actions.MockConfiguration;
import ru.yandex.qe.mail.meetings.services.calendar.CalendarWeb;
import ru.yandex.qe.mail.meetings.services.staff.StaffClient;
import ru.yandex.qe.mail.meetings.ws.forms.FormsProcessorTest;

import static org.junit.Assert.assertEquals;
import static ru.yandex.qe.mail.meetings.ws.validation.ValidationResult.Status.ERROR;
import static ru.yandex.qe.mail.meetings.ws.validation.ValidationResult.Status.OK;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class CalendarUrlValidatorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    private CalendarWeb calendarWeb;
    @Inject
    private StaffClient staffClient;

    @Test
    public void basicUrlMustBeCorrect() {
        var result = validate("https://calendar.yandex-team.ru/event/39801580");
        assertEquals(OK, result.getStatus());
    }

    @Test
    public void unexistingIdMustFail() {
        ValidationResult result = validate("https://calendar.yandex-team.ru/event/123312");
        assertEquals(ERROR, result.getStatus());
    }

    @Test
    public void urlWithParametersMustBeOk() {
        ValidationResult result = validate("https://calendar.yandex-team.ru/event/39801580?applyToFuture=0&event_date" +
                "=2020-01-23T20%3A30%3A00&layerId=169");
        assertEquals(OK, result.getStatus());
    }

    @Test
    public void urlWithExtraParametrsMustBeOk() {
        ValidationResult result = validate("https://calendar.yandex-team.ru/event/39801580?applyToFuture=0&event_date" +
                "=2020-01-27T10%3A30%3A00&layerId=112&show_date=2020-01-27&uid=1120000000019824");
        assertEquals(OK, result.getStatus());
    }

    @Test
    public void hostMustBeCorrect() {
        ValidationResult result = validate("https://calendar-FAKE.yandex-team.ru/event/39801580");
        assertEquals(ERROR, result.getStatus());
    }

    @Test
    public void trailingSlashIsNotAllowed() {
        ValidationResult result = validate("https://calendar.yandex-team.ru/event/39801580/");
        assertEquals(ERROR, result.getStatus());
    }

    @Test
    public void weirdUrlsAreNotAllowed() {
        ValidationResult result = validate("https://calendar.yandex-team.ru/event?attendees=baymer%40yandex-team" +
                ".ru&description=Что%20не%20забыть%20обсудить%3A%0A-%20форма%20для%20флагов%0A-%20вызов%20кода%20" +
                "рекламы%20первым%20приоритетом%20%0A-%20интерскроллер&endTs=1580391000000&isAllDay=0&name=1");
        assertEquals(ERROR, result.getStatus());
    }

    @Test
    public void successSerializationMustNotContailsErrors() throws JsonProcessingException {
        // поле errors дложно отсутствовать при сериализации
        ValidationResult result = validate("https://calendar.yandex-team.ru/event/39801580");
        String serialized = MAPPER.writeValueAsString(result);
        assertEquals("{\"status\":\"OK\"}", serialized);
    }

    @Test
    public void errorSerializationMustNotContailsErrors() throws JsonProcessingException {
        ValidationResult result = validate("https://???");
        String serialized = MAPPER.writeValueAsString(result);
        assertEquals("{\"status\":\"ERROR\",\"errors\":{\"calendarUrl\":[\"Incorrect value\"]}}", serialized);
    }

    private ValidationRequest requestFrom(@Nonnull String url) {
        return new ValidationRequest(0, "slug", "name", List.of(FormsProcessorTest.makeQuestion("calendarUrl", url)));
    }

    private ValidationResult validate(String url) {
        return CalendarUrlValidator.with(calendarWeb, staffClient, "calendarUrl").validate(requestFrom(url));
    }
}
