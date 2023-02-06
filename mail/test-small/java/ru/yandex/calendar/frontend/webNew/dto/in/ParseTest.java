package ru.yandex.calendar.frontend.webNew.dto.in;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import lombok.val;
import one.util.streamex.StreamEx;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import ru.yandex.calendar.frontend.bender.WebDateTime;
import ru.yandex.calendar.frontend.webNew.WebNewCodecs;
import ru.yandex.calendar.frontend.webNew.dto.inOut.RepetitionData;
import ru.yandex.calendar.logic.beans.generated.SettingsFields;
import ru.yandex.calendar.logic.event.avail.Availability;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.event.repetition.RegularRepetitionRule;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.notification.Channel;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.resource.ResourceFilter;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.bolts.collection.Option.x;

public class ParseTest {
    @Test
    public void bindEvent() {
        val eventJson = "{" +
                "\"type\": \"user\"," +
                "\"startTs\": \"2012-03-19T13:43:00\"," +
                "\"endTs\": \"2012-03-19T14:43:00\"," +
                "\"name\": \"Событие\"," +
                "\"location\": \"Место\"," +
                "\"description\": \"Описание\"," +
                "\"participantsCanInvite\": \"true\"," +
                "\"participantsCanEdit\": \"true\"," +
                "\"othersCanView\": \"true\"," +
                "\"availability\": \"available\"" +
                "}";

        val createEventData = parseEvent(eventJson);

        assertThat(createEventData.getType()).contains(EventType.USER);
        assertThat(createEventData.getStartTs()).contains(WebDateTime.localDateTime(new LocalDateTime(2012, 3, 19, 13, 43, 0)));
        assertThat(createEventData.getEndTs()).contains(WebDateTime.localDateTime(new LocalDateTime(2012, 3, 19, 14, 43, 0)));
        assertThat(createEventData.getName()).contains("Событие");
        assertThat(createEventData.getLocation()).contains("Место");
        assertThat(createEventData.getDescription()).contains("Описание");
        assertThat(createEventData.getEvent(MoscowTime.TZ, MoscowTime.TZ).getPermAll()).isEqualTo(EventActionClass.VIEW);
        assertThat(createEventData.getEvent(MoscowTime.TZ, MoscowTime.TZ).getPermParticipants()).isEqualTo(EventActionClass.EDIT);
        assertThat(createEventData.getEventUserData().getEventUser().getAvailability()).isEqualTo(Availability.AVAILABLE);

        assertThat(createEventData.getAllDay()).isEmpty();
        assertThat(createEventData.getMapUrl()).isEmpty();
        assertThat(createEventData.getUrl()).isEmpty();
        assertThat(createEventData.getSid()).isEmpty();
        assertThat(createEventData.getRepetition(MoscowTime.TZ, MoscowTime.TZ)).isEmpty();
        assertThat(createEventData.getLayerId()).isEmpty();
        assertThat(createEventData.getNotifications()).isEmpty();
    }

    @Test
    public void notification() {
        val eventJson = "{" +
                "\"type\": \"user\"," +
                "\"startTs\": \"2012-03-19T13:43:00\"," +
                "\"endTs\": \"2012-03-19T14:43:00\"," +
                "\"name\": \"Событие\"," +
                "\"location\": \"Место\"," +
                "\"description\": \"Описание\"," +
                "\"permAll\": \"view\"," +
                "\"permParticipants\": \"edit\"," +
                "\"participantsInvite\": true," +
                "\"notifications\": [" +
                "{\"channel\": \"sms\", \"offset\": \"10m\"}," +
                "{\"channel\": \"email\", \"offset\": \"-10m\"}," +
                "{\"channel\": \"display\", \"offset\": \"100m\"}," +
                "{\"channel\": \"audio\", \"offset\": \"-100m\"}" +
                "]" +
                "}";
        val createEventData = parseEvent(eventJson);

        assertThat(createEventData.getNotifications().toOptional())
                .get()
                .asList()
                .containsExactlyInAnyOrder(
                        new Notification(Channel.SMS, Duration.standardMinutes(10)),
                        new Notification(Channel.EMAIL, Duration.standardMinutes(-10)),
                        new Notification(Channel.DISPLAY, Duration.standardMinutes(100)),
                        new Notification(Channel.AUDIO, Duration.standardMinutes(-100)));
    }

    @Test
    public void eventWithDailyRepetition() {
        val eventJson = "{" +
                "\"type\": \"user\"," +
                "\"startTs\": \"2012-03-19T13:43:00\"," +
                "\"endTs\": \"2012-03-19T14:43:00\"," +
                "\"name\": \"Событие\"," +
                "\"location\": \"Место\"," +
                "\"description\": \"Описание\"," +
                "\"permAll\": \"view\"," +
                "\"permParticipants\": \"edit\"," +
                "\"participantsInvite\": true," +
                "\"repetition\": {" +
                "\"type\":\"daily\"," +
                "\"each\":5," +
                "\"dueDate\":\"2012-03-19\"" +
                "}" +
                "}";
        val createEventData = parseEvent(eventJson);
        val repetition = createEventData.getRepetition(MoscowTime.TZ, MoscowTime.TZ).get();

        assertThat(repetition.getType()).isEqualTo(RegularRepetitionRule.DAILY);
        assertThat(repetition.getREach()).contains(5);
        assertThat(repetition.getDueTs()).contains(TestDateTimes.moscow(2012, 3, 20, 13, 43));
    }

    private void testRepetition(RegularRepetitionRule type, Optional<LocalDate> dueDate, Optional<Boolean> monthlyLastWeek, int each, Optional<String> weeklyDays, String repetitionJson) {
        val expectedRepetitionData = constructRepetitionData(type, dueDate, monthlyLastWeek, each, weeklyDays);
        val repetitionData = parse(repetitionJson, RepetitionData.class);

        assertThat(repetitionData).isEqualTo(expectedRepetitionData);
    }

    @Test
    public void monthlyDayWeekno() {
        testRepetition(RegularRepetitionRule.MONTHLY_DAY_WEEKNO, Optional.empty(), Optional.of(true), 3, Optional.empty(),
                "{" +
                        "\"type\": \"monthlyDayWeekno\"," +
                        "\"monthlyLastweek\": true," +
                        "\"each\": 3" +
                        "}");
    }

    @Test
    public void monthlyNumber() {
        testRepetition(RegularRepetitionRule.MONTHLY_NUMBER, Optional.empty(), Optional.empty(), 7, Optional.empty(),
                "{" +
                        "\"type\": \"monthlyNumber\"," +
                        "\"each\": 7" +
                        "}");
    }

    @Test
    public void weekly() {
        testRepetition(RegularRepetitionRule.WEEKLY, Optional.empty(), Optional.empty(), 3, Optional.of("MON,FRI"),
                "{" +
                        "\"type\": \"weekly\"," +
                        "\"each\": 3," +
                        "\"weeklyDays\": \"MON,FRI\"" +
                        "}");
    }

    @Test
    public void weeklyWithStart() {
        val expectedRepetitionData = constructRepetitionData(RegularRepetitionRule.WEEKLY, Optional.empty(),
                Optional.empty(), 3, Optional.of("TUE,WED"));
        val repetitionJson = "{" +
                "\"startTs\": \"2012-05-18T21:48\"," +
                "\"repetition\": {" +
                "\"type\": \"weekly\"," +
                "\"each\": 3," +
                "\"weeklyDays\": \"TUE,WED\"" +
                "}" +
                "}";
        val repetitionDataWithStart = parse(repetitionJson, RepetitionWithStartData.class);

        assertThat(repetitionDataWithStart.getStartTs()).isEqualTo(new LocalDateTime(2012, 5, 18, 21, 48));
        assertThat(repetitionDataWithStart.getRepetition()).isEqualTo(expectedRepetitionData);
    }

    public static WebEventData parseEvent(String eventJson) {
        return parse(eventJson, WebEventData.class);
    }

    private static <T> T parse(String eventJson, Class<T> clazz) {
        return WebNewCodecs.getParser(clazz).parseJson(eventJson);
    }

    private static RepetitionData constructRepetitionData(RegularRepetitionRule type, Optional<LocalDate> dueDate, Optional<Boolean> monthlyLastWeek, int each, Optional<String> weeklyDays) {
        val repetitionData = new RepetitionData();
        repetitionData.setType(type);
        repetitionData.setDueDate(x(dueDate));
        repetitionData.setMonthlyLastweek(x(monthlyLastWeek));
        repetitionData.setEach(each);
        repetitionData.setWeeklyDays(x(weeklyDays));
        return repetitionData;
    }

    @Test
    public void participants() {
        val createEventJson = "{" +
                "\"type\": \"user\"," +
                "\"startTs\": \"2012-03-19T13:43:00\"," +
                "\"endTs\": \"2012-03-19T14:43:00\"," +
                "\"name\": \"Событие\"," +
                "\"location\": \"Место\"," +
                "\"description\": \"Описание\"," +
                "\"permAll\": \"view\"," +
                "\"permParticipants\": \"edit\"," +
                "\"participantsInvite\": true," +
                "\"attendees\": [\"user1@yandex.ru\", \"user2@yandex.ru\", \"user3@yandex.ru\"]" +
                "}";
        val attendeeEmails = parseEvent(createEventJson).getAttendeeEmails().get();

        assertThat(unwrapEmails(attendeeEmails))
                .containsExactlyInAnyOrder("user1@yandex.ru", "user2@yandex.ru", "user3@yandex.ru");
    }

    @Test
    public void suggestData() {
        val suggestionJson = "{" +
                "\"users\": [\"user1@yandex.ru\", \"user2@yandex.ru\"]," +
                "\"offices\": [{" +
                "\"id\": 1," +
                "\"filter\": \"\"," +
                "\"selectedResourceEmails\": []," +
                "\"numberOfResources\": 3" +
                "}," +
                "{" +
                "\"id\":2," +
                "\"filter\": \"\"," +
                "\"selectedResourceEmails\": [\"resource1@yandex.ru\", \"resource2@yandex.ru\"]" +
                "}" +
                "]," +
                "\"searchStart\": \"2012-03-19T00:00\"," +
                "\"searchBackward\": true," +
                "\"eventStart\": \"2012-03-19T14:43\"," +
                "\"eventEnd\": \"2012-03-19T15:43\"" +
                "}";
        val suggestData = parse(suggestionJson, SuggestData.class);

        assertThat(unwrapEmails(suggestData.getUsers())).containsExactlyInAnyOrder("user1@yandex.ru", "user2@yandex.ru");

        assertThat(suggestData.getOfficeIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(suggestData.getSearchStart())
                .contains(new LocalDateTime(2012, 3, 19, 0, 0));
        assertThat(suggestData.getSearchBackward()).contains(true);

        assertThat(suggestData.getEventStart()).isEqualTo( new LocalDateTime(2012, 3, 19, 14, 43));
        assertThat(suggestData.getDuration()).isEqualTo(Duration.standardHours(1));

        val numbersOfResources = suggestData.getNumbersOfResources();
        assertThat(numbersOfResources).hasSize(2);
        assertThat(numbersOfResources.getTs(1L)).isEqualTo(3);
        assertThat(numbersOfResources.getTs(2L)).isEqualTo(1);

        assertThat(unwrapEmails(suggestData.getOffices().last().getSelectedResourceEmails()))
                .containsExactlyInAnyOrder("resource1@yandex.ru", "resource2@yandex.ru");
    }

    @Test
    public void suggestDataWithResourceFilters() {
        val eventJson = "{" +
                "\"users\": [\"user1@yandex.ru\", \"user2@yandex.ru\"]," +
                "\"offices\": [{" +
                "\"id\": 1," +
                "\"filter\": \"video,large\"," +
                "\"resourceEmails\": []" +
                "}," +
                "{" +
                "\"id\":2," +
                "\"filter\": \"projector,medium\"," +
                "\"resourceEmails\": []" +
                "}" +
                "]," +
                "\"searchStart\": \"2012-03-19T00:00\"," +
                "\"searchEnd\": \"2012-03-20T00:00\"," +
                "\"eventStart\": \"2012-03-19T14:43\"," +
                "\"eventEnd\": \"2012-03-19T15:43\"" +
                "}";
        val suggestData = parse(eventJson, SuggestData.class);

        assertThat(suggestData.getResourceFilters().getTs(1L)
                .sameAs(ResourceFilter.any().withFilter("video,large"))).isTrue();
        assertThat(suggestData.getResourceFilters().getTs(2L)
                .sameAs(ResourceFilter.any().withFilter("projector,medium"))).isTrue();
    }

    @Test
    public void replyData() {
        val replyDataJson = "{" +
                "\"eventId\":15," +
                "\"decision\":\"yes\"," +
                "\"reason\":\"111\"," +
                "\"availability\":\"busy\"," +
                "\"notifications\": [" +
                "{\"channel\": \"sms\", \"offset\": \"10m\"}," +
                "{\"channel\": \"email\", \"offset\": \"-10m\"}" +
                "]" +
                "}";
        val replyData = parse(replyDataJson, ReplyData.class);

        assertThat(replyData.getDecision()).isEqualTo(Decision.YES);
        assertThat(replyData.getEventId()).isEqualTo(15L);
        assertThat(replyData.getReason()).contains("111");
        assertThat(replyData.getEventUser().getAvailability()).isEqualTo(Availability.BUSY);
        assertThat(replyData.getNotificationsData().asCreateWithData().getNotifications())
                .containsExactlyInAnyOrder(
                        new Notification(Channel.SMS, Duration.standardMinutes(10)),
                        new Notification(Channel.EMAIL, Duration.standardMinutes(-10)));
    }

    @Test
    public void replyDataWithDefaultNotifications() {
        val replyDataJson = "{" +
                "\"eventId\":15," +
                "\"decision\":\"yes\"" +
                "}";
        val replyData = parse(replyDataJson, ReplyData.class);

        assertThat(replyData.getNotificationsData().isUseLayerDefaultIfCreate()).isTrue();
    }

    @Test
    public void replyDataWithEmptyNotifications() {
        val replyDataJson = "{" +
                "\"eventId\":15," +
                "\"decision\":\"yes\"," +
                "\"notifications\":[]" +
                "}";
        val replyData = parse(replyDataJson, ReplyData.class);

        assertThat(replyData.getNotificationsData().asCreateWithData().getNotifications()).isEmpty();
    }

    @Test
    public void settingsNoNotificationsRange() {
        assertThat(parse("{}", UserSettingsData.class).getSettings(MoscowTime.TZ)
                .isFieldSet(SettingsFields.NO_NTF_START_TS)).isFalse();
        assertThat(parse("{\"noNotificationsRange\":{}}", UserSettingsData.class).getSettings(MoscowTime.TZ)
                .isFieldSet(SettingsFields.NO_NTF_START_TS)).isTrue();
    }

    @Test
    public void settingsTodoEmails() {
        assertThat(parse("{}", UserSettingsData.class).getSettings(MoscowTime.TZ)
                .isFieldSet(SettingsFields.TODO_PLANNED_EMAIL_TM)).isFalse();
        assertThat(parse("{\"todoEmailTimes\":{}}", UserSettingsData.class).getSettings(MoscowTime.TZ)
                .getFieldValue(SettingsFields.TODO_PLANNED_EMAIL_TM)).isNull();
        assertThat(parse("{\"todoEmailTimes\":{\"planned\":\"00:05\"}}", UserSettingsData.class).getSettings(MoscowTime.TZ)
                .getFieldValue(SettingsFields.TODO_PLANNED_EMAIL_TM)).isEqualTo(300000);
    }

    private static List<String> unwrapEmails(Collection<Email> emails) {
        return StreamEx.of(emails).map(Email::getEmail).toImmutableList();
    }
}
