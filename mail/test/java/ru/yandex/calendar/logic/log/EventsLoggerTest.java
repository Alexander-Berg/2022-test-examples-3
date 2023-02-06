package ru.yandex.calendar.logic.log;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class EventsLoggerTest {
    private final static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGenJsonLineForExternalIdChangeLogEvent() throws IOException {
        val actionInfo = EventMocks.webActionInfo();
        val logEvent = EventMocks.externalIdChangeEvent();
        val res = EventsLogger.genJsonLine(logEvent, actionInfo);

        val map = mapper.readValue(res, new TypeReference<Map<String, Object>>(){});
        assertThat(map)
            .hasSize(8)
            .containsKeys("host", "event_info")
            .contains(
                entry("datetime", "2021-11-11 10:00:00.000"),
                entry("action", "somename"),
                entry("action_source", "WEB"),
                entry("rid", "111111"),
                entry("unixtime", 1636614000000L)
            );

        @SuppressWarnings("unchecked")
        val evInfo = (Map<String, Object>) map.get("event_info");
        assertThat(evInfo).isNotEmpty().containsOnly(entry("external_id_old", "111"), entry("external_id_new", "222"), entry("change_type", "update"), entry("type", "EVENT_CHANGE"));
    }

    @Test
    public void testGenJsonLineForEventChangeLogEvent() throws IOException {
        val actionInfo = EventMocks.caldavActionInfo();
        val logEvent = EventMocks.eventChangeEvent();
        val res = EventsLogger.genJsonLine(logEvent, actionInfo);

        val map = mapper.readValue(res, new TypeReference<Map<String, Object>>() {});
        assertThat(map)
            .hasSize(8)
            .containsKeys("host")
            .contains(
                entry("datetime", "2021-11-11 10:00:00.000"),
                entry("action", "somename"),
                entry("action_source", "CALDAV"),
                entry("rid", "111111"),
                entry("unixtime", 1636614000000L)
            );

        @SuppressWarnings("unchecked")
        val evInfo = (Map<String, Object>) map.get("event_info");
        assertThat(evInfo).hasSize(14).contains(entry("event_id", 179), entry("main_event_id", 133),
                entry("external_id", "111"), entry("recurrence_id", "2021-11-21T10:00:00.000Z"),
                entry("name", "NEWNAME"), entry("description", "NEWDESCRIPTION"), entry("location", "NEWLOCATION"),
                entry("type", "EVENT_CHANGE"), entry("actor", "111111"), entry("change_type", "CREATE")).containsKeys("repetition", "resources", "users", "layers");

        @SuppressWarnings("unchecked")
        val repetition = (Map<String, Object>) evInfo.get("repetition");
        assertThat(repetition).hasSize(2).containsKeys("rule", "exdates");

        @SuppressWarnings("unchecked")
        val repetitionRule = (Map<String, Object>) repetition.get("rule");
        assertThat(repetitionRule).containsOnly(entry("type", "DAILY"), entry("each", 3), entry("due", "2021-12-11T07:00:00.000Z"), entry("weekly_days_old", "tuesday"),
                entry("weekly_days", "mon"), entry("monthly_lastweek", false));

        @SuppressWarnings("unchecked")
        val exdates = (Map<String, Object>) repetition.get("exdates");
        assertThat(exdates).containsOnly(entry("removed", List.of("2021-12-10T07:00:00.000Z")),
                entry("added", List.of("2021-12-12T07:00:00.000Z")));

        @SuppressWarnings("unchecked")
        val resources = (Map<String, Object>) evInfo.get("resources");
        assertThat(resources).hasSize(1).containsKeys("removed");

        @SuppressWarnings("unchecked")
        val removedResources = (List<Map<String, Object>>) resources.get("removed");
        assertThat(removedResources.get(0)).containsOnly(entry("id", 133), entry("email", "a@yandex-team.ru"));

        @SuppressWarnings("unchecked")
        val users = (Map<String, Object>) evInfo.get("users");
        assertThat(users).hasSize(1).containsKeys("removed");

        @SuppressWarnings("unchecked")
        val delUsers = (List<Map<String, Object>>) users.get("removed");
        assertThat(delUsers.get(0)).containsOnly(entry("decision_old", "YES"), entry("decision", "NO"),
                entry("availability", "AVAILABLE"), entry("participation_old", "ORGANIZER"), entry("participation", "ATTENDEE"),
                entry("uid", 11), entry("email", "newuser@yandex-team.ru"));

        @SuppressWarnings("unchecked")
        val layers = (Map<String, Object>) evInfo.get("layers");
        assertThat(layers).hasSize(1).containsKeys("removed");

        @SuppressWarnings("unchecked")
        val delLayers = (List<Map<String, Object>>) layers.get("removed");
        Map<String, Object> layerJson1 = Map.of(
            "id", 1113,
            "uid", 333
        );
        Map<String, Object> layerJson2 = Map.of(
            "id", 1112,
            "uid", 333
        );
        assertThat(delLayers).contains(layerJson1, layerJson2);
    }

    @Test
    public void testGenJsonLineForEventMailLogEvent() throws IOException {
        val actionInfo = EventMocks.mailActionInfo();

        val mailLogEvent = EventMocks.mailLogEvent();
        val res = EventsLogger.genJsonLine(mailLogEvent, actionInfo);

        Map<String, Object> map = mapper.readValue(res, new TypeReference<Map<String, Object>>(){});
        assertThat(map).hasSize(8).contains(entry("datetime", "2021-11-11 10:00:00.000"),
                entry("action", "somename"), entry("action_source", "MAIL"), entry("rid", "111111"), entry("unixtime", 1636614000000L)).containsKeys("host");

        @SuppressWarnings("unchecked")
        val evInfo = (Map<String, Object>) map.get("event_info");
        assertThat(evInfo).containsOnly(entry("type", "EVENT_MAIL"), entry("message_id", "199393"),
                entry("to", "to@yandex.ru"), entry("from", "ad@yandex-team.ru"), entry("mail_type", "APARTMENT_CANCEL"),
                entry("decision", "MAYBE"), entry("not_for_exchange", "yes"), entry("event_id", 11), entry("main_event_id", 22),
                entry("external_id", "13"), entry("recurrence_id", null),
                entry("status", "SUCCESSFUL"), entry("response", "250 2.0.0 Ok: queued as 1E91620002"));
    }

    @Test
    public void testGenJsonLineForEwsCallLogEvent() throws IOException {
        val actionInfo = EventMocks.exchangeActionInfo();
        val ev = EventMocks.ewsCallEvent();
        val res = EventsLogger.genJsonLine(ev, actionInfo);

        val map = mapper.readValue(res, new TypeReference<Map<String, Object>>(){});
        assertThat(map)
            .hasSize(8)
            .containsKeys("host")
            .contains(
                entry("datetime", "2021-11-11 10:00:00.000"),
                entry("action", "somename"),
                entry("action_source", "EXCHANGE"),
                entry("rid", "111111"),
                entry("unixtime", 1636614000000L)
            );

        @SuppressWarnings("unchecked")
        val evInfo = (Map<String, Object>) map.get("event_info");
        assertThat(evInfo).containsOnly(entry("operation", "ACCEPT_MEETING"), entry("subject", "122"), entry("event_id", 1388),
                entry("main_event_id", 3333), entry("external_id", "188929"), entry("recurrence_id", "2021-11-11T08:00:00.000Z"),
                entry("type", "EVENT_EWS_CALL"), entry("disposition", "SEND_AND_SAVE_COPY"), entry("invitations", "SEND_ONLY_TO_ALL"));
    }

    @Test
    public void testGenJsonLineForNotificationLogEvent() throws IOException {
        val actionInfo = EventMocks.xivaActionInfo();
        val notificationLogEvent = EventMocks.notificationEvent();
        val res = EventsLogger.genJsonLine(notificationLogEvent, actionInfo);
        Map<String, Object> map = mapper.readValue(res, new TypeReference<Map<String, Object>>(){});
        assertThat(map)
            .hasSize(8)
            .containsKeys("host")
            .contains(
                entry("datetime", "2021-11-11 10:00:00.000"),
                entry("action", "somename"),
                entry("action_source", "XIVA"),
                entry("rid", "111111"),
                entry("unixtime", 1636614000000L)
            );

        @SuppressWarnings("unchecked")
        val evInfo = (Map<String, Object>) map.get("event_info");
        assertThat(evInfo).containsOnly(entry("event_id", 2992), entry("main_event_id", 29920), entry("external_id", "002020293"),
                entry("recurrence_id", "2021-11-12T07:00:00.000Z"), entry("type", "EVENT_REMINDER"), entry("uid", "122"),
                entry("channel", "EMAIL"), entry("send_ts", "2021-11-11T07:00:00.000Z"), entry("event_user_id", 23923),
                entry("event_start_ts", "2021-11-11T06:57:00.000Z"), entry("event_notification_id", 33),
                entry("status", "FATAL_ERROR"));
    }

    @Test
    public void testGenJsonLineForTodoMailLogEvent() throws IOException {
        val actionInfo = EventMocks.mailActionInfo();
        val todoMailLogEv = EventMocks.todoMailEvent();
        val res = EventsLogger.genJsonLine(todoMailLogEv, actionInfo);

        val map = mapper.readValue(res, new TypeReference<Map<String, Object>>(){});
        assertThat(map)
            .hasSize(8)
            .containsKeys("host")
            .contains(
                entry("datetime", "2021-11-11 10:00:00.000"),
                entry("action", "somename"),
                entry("action_source", "MAIL"),
                entry("rid", "111111"),
                entry("unixtime", 1636614000000L)
            );

        @SuppressWarnings("unchecked")
        val evInfo = (Map<String, Object>) map.get("event_info");
        assertThat(evInfo).containsOnly(entry("message_id", "19327382"), entry("mail_type", "PLANNED"),
                entry("to", "ab@yandex-team.ru"), entry("uid", 122), entry("items_count", 1), entry("lists_count", 1),
                entry("type", "TODO_MAIL"),
                entry("status", "SUCCESSFUL"), entry("response", "250 2.0.0 Ok: queued as 1E91620002"));
    }

    @Test
    public void testGenJsonLineForMailerHandlingLogEvent() throws IOException {
        val actionInfo = EventMocks.mailActionInfo();
        val ev = EventMocks.mailerHandlingEvent();
        val res = EventsLogger.genJsonLine(ev, actionInfo);

        val map = mapper.readValue(res, new TypeReference<Map<String, Object>>(){});
        assertThat(map)
            .hasSize(8)
            .containsKeys("host")
            .contains(
                entry("datetime", "2021-11-11 10:00:00.000"),
                entry("action", "somename"),
                entry("action_source", "MAIL"),
                entry("rid", "111111"),
                entry("unixtime", 1636614000000L)
            );

        @SuppressWarnings("unchecked")
        val evInfo = (Map<String, Object>) map.get("event_info");

        assertThat(evInfo).containsOnly(entry("mid", 1812891), entry("uid", 122),
                entry("email", "ad@yandex-team.ru"), entry("message_date", "2021-11-11T07:00:00.000Z"),
                entry("from", "ad@yandex-team.ru"), entry("message_id", "91983"), entry("status", "SENT"),
                entry("external_id", "12891291"), entry("recurrence_id", "2021-11-11T07:00:00.000Z"),
                entry("ics_method", "publish"), entry("type", "MAILER_HANDLING"));
    }
}
