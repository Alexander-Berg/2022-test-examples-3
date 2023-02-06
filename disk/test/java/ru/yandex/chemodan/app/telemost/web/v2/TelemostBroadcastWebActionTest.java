package ru.yandex.chemodan.app.telemost.web.v2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.calendar.CalendarClient;
import ru.yandex.chemodan.app.telemost.calendar.CalendarClientStub;
import ru.yandex.chemodan.app.telemost.calendar.model.CalendarEvent;
import ru.yandex.chemodan.app.telemost.chat.ChatClient;
import ru.yandex.chemodan.app.telemost.mock.blackbox.UserData;
import ru.yandex.chemodan.app.telemost.services.ParticipantType;
import ru.yandex.chemodan.app.telemost.services.StreamService;
import ru.yandex.chemodan.app.telemost.services.model.BroadcastAndConferenceUris;
import ru.yandex.chemodan.app.telemost.services.model.PassportOrYaTeamUid;
import ru.yandex.chemodan.app.telemost.services.model.Stream;
import ru.yandex.chemodan.app.telemost.web.v2.model.BroadcastStatus;
import ru.yandex.chemodan.test.A3TestHelper;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.io.http.UrlUtils;
import ru.yandex.misc.test.Assert;

public class TelemostBroadcastWebActionTest extends AbstractConferenceWebActionTest {
    private static final PassportOrYaTeamUid TEST_UID = PassportOrYaTeamUid.passportUid(PassportUid.cons(123));
    private static final PassportOrYaTeamUid JOIN_UID = PassportOrYaTeamUid.passportUid(PassportUid.cons(567));

    private A3TestHelper helper;

    @Autowired
    private StreamService streamService;

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private CalendarClient calendarClient;

    @Before
    public void init() {
        super.before();
        helper = getA3TestHelper();

        addUsers(Cf.map(TEST_UID.getPassportUid().getUid(),
                UserData.defaultUser("test", Option.of("test"), Option.empty(), Cf.map())));
        addUsers(Cf.map(JOIN_UID.getPassportUid().getUid(),
                UserData.defaultUser("join", Option.of("join"), Option.empty(), Cf.map())));
    }

    private CalendarEvent addCalendarEvent(String eventId, Instant startEvent) {
        CalendarEvent calendarEvent = new CalendarEvent(eventId, "Event caption", "Event description",
                startEvent, startEvent.plus(Duration.standardHours(1)));
        ((CalendarClientStub)calendarClient).addEvent(calendarEvent);
        return calendarEvent;
    }

    private String createConferenceWithBroadcastFeature() throws IOException {
        Assert.equals(userService.upsert(PassportOrYaTeamUid.parseUid(TEST_UID.asString()), true).isBroadcastEnabled(),
                true);

        return testCreateConferenceByLink("/v2/conferences?uid=" + TEST_UID.asString(),
                ru.yandex.chemodan.app.telemost.web.v1.TelemostCreateConferenceWebActionTest.URL_PATTERN,
                true);
    }

    private void checkBroadcastData(Map<String, Object> broadcastData, boolean chatIsNeed) {
        Assert.isTrue(broadcastData.containsKey("broadcast_uri"));
        Object broadcastUri = broadcastData.get("broadcast_uri");
        Assert.isTrue(broadcastUri instanceof String);

        if (chatIsNeed) {
            Assert.isTrue(broadcastData.containsKey("broadcast_chat_path"));
            Object broadcastChatPath = broadcastData.get("broadcast_chat_path");
            Assert.isTrue(broadcastChatPath instanceof String);
            Assert.isTrue(((String) broadcastChatPath).startsWith("0/32/"));
        } else {
            Assert.isFalse(broadcastData.containsKey("broadcast_chat_path"));
        }
    }

    @Test
    public void testCreateBroadcast() throws IOException {
        String conferenceUri = createConferenceWithBroadcastFeature();

        chatClient.getUser(TEST_UID.asString());

        HttpResponse response = helper.put("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri)
                + "/broadcast?uid=" + TEST_UID.asString(), "{}");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });

        checkBroadcastData(result, true);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJoinToConferenceWithBroadcast() throws IOException {
        String conferenceUri = createConferenceWithBroadcastFeature();

        HttpResponse response = helper.put("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) + "/broadcast?uid=" + TEST_UID.asString(),
                "{}");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result1 = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });

        checkBroadcastData(result1, true);

        Assert.isFalse(result1.containsKey("streams"));

        Map<String, Object> result2 = joinToConferenceAndGetState("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri)
                        + "/connection?uid=" + JOIN_UID.asString(), ParticipantType.USER);

        Assert.isTrue(result2.containsKey("broadcast_data"));

        Map<String, Object> broadcastData = (Map<String, Object>)result2.get("broadcast_data");

        checkBroadcastData(broadcastData, true);

        Assert.isTrue(broadcastData.containsKey("created_by"));
        Assert.isTrue(broadcastData.containsKey("status"));

        Assert.equals(BroadcastStatus.CREATED, BroadcastStatus.valueOf(broadcastData.get("status").toString()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStreamStateData() throws IOException {
        String conferenceUri = createConferenceWithBroadcastFeature();

        HttpResponse response = helper.put("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                "/broadcast?uid=" + TEST_UID.asString(), "{}");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result1 = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });
        String broadcastUi = (String) result1.get("broadcast_uri");
        Stream stream = streamService.startStream(TEST_UID, new BroadcastAndConferenceUris(broadcastUi, conferenceUri));

        Map<String, Object> translatorResult = joinToConferenceAndGetState("/v2/conferences/" +
                        UrlUtils.urlEncode(conferenceUri) + "/connection" +
                        "?translator_token=" + stream.getStream().getTranslatorToken().get(),
                ParticipantType.TRANSLATOR);

        Map<String, Object>
                result2 = joinToConferenceAndGetState("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                        "/connection?uid=" + JOIN_UID.asString(), ParticipantType.USER);

        Assert.isTrue(result2.containsKey("broadcast_data"));

        Map<String, Object> broadcastData = (Map<String, Object>)result2.get("broadcast_data");

        Assert.isTrue(broadcastData.containsKey("started_at"));
        Assert.isTrue(broadcastData.containsKey("started_by"));
        Assert.isTrue(broadcastData.containsKey("translator_peer_id"));
        Assert.isTrue(broadcastData.containsKey("stream_uri"));

        Assert.equals(TEST_UID.asString(), broadcastData.get("started_by"));

        Map<String, Object> translatorBroadcastData = (Map<String, Object>) translatorResult.get("broadcast_data");

        Assert.equals(translatorBroadcastData.get("started_by"), broadcastData.get("started_by"));
        Assert.equals(translatorBroadcastData.get("translator_peer_id"), broadcastData.get("translator_peer_id"));
        Assert.equals(translatorBroadcastData.get("stream_uri"), broadcastData.get("stream_uri"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStreamConnectionData() throws IOException {
        String conferenceUri = createConferenceWithBroadcastFeature();

        HttpResponse response = helper.put("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                        "/broadcast?uid=" + TEST_UID.asString(),
                "{\"caption\": \"caption1\", \"description\": \"description1\"}");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result1 = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });

        String broadcastUri = (String)result1.get("broadcast_uri");

        response = helper.get("/v2/broadcast/" + UrlUtils.urlEncode(broadcastUri) + "/connection");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result2 = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });

        Assert.isTrue(result2.containsKey("caption"));
        Assert.isTrue(result2.containsKey("description"));

        Assert.equals("caption1", (String)result2.get("caption"));
        Assert.equals("description1", (String)result2.get("description"));
    }

    @Test
    public void testCreateChatWhenBroadcastCreationInTbl() throws IOException {
        CalendarEvent calendarEvent = addCalendarEvent("1", Instant.now().plus(Duration.standardMinutes(30)));

        String conferenceUri = createConferenceWithBroadcastFeature();

        HttpResponse response = helper.post("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                "/link_calendar_event?uid=" + TEST_UID.asString() + "&calendar_event_id=" + calendarEvent.getEventId());

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Создание трансляции (чат должен быть)
        response = helper.put("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                        "/broadcast?uid=" + TEST_UID.asString(), "{}");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });

        checkBroadcastData(result, true);
    }

    @Test
    public void testCreateChatWhenConnectionViewer() throws IOException {
        CalendarEvent calendarEvent = addCalendarEvent("1", Instant.now().plus(Duration.standardMinutes(70)));

        String conferenceUri = createConferenceWithBroadcastFeature();

        HttpResponse response = helper.post("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                "/link_calendar_event?uid=" + TEST_UID.asString() + "&calendar_event_id=" + calendarEvent.getEventId());

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Создание трансляции (чата быть не должно)
        response = helper.put("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                "/broadcast?uid=" + TEST_UID.asString(), "{}");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result1 = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });

        checkBroadcastData(result1, false);

        // Подключение вьювера задолго до встречи (чата быть не должно)
        String broadcastUri = (String)result1.get("broadcast_uri");

        response = helper.get("/v2/broadcast/" + UrlUtils.urlEncode(broadcastUri) + "/connection");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result2 = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });

        Assert.isFalse(result2.containsKey("stream_uri"));
        Assert.isTrue(result2.containsKey("start_event_time"));
        Assert.isTrue(result2.containsKey("caption"));
        Assert.isFalse(result2.containsKey("description"));
        Assert.isFalse(result2.containsKey("broadcast_chat_path"));
        Assert.isTrue(result2.containsKey("status"));
        Assert.equals("CREATED", result2.get("status"));

        // Подключение вьювера, когда до встречи осталось немного (чат должен быть создан)
        addCalendarEvent("1", Instant.now().plus(Duration.standardMinutes(30)));

        response = helper.get("/v2/broadcast/" + UrlUtils.urlEncode(broadcastUri) + "/connection");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result3 = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });

        Assert.isFalse(result3.containsKey("stream_uri"));
        Assert.isTrue(result3.containsKey("start_event_time"));
        Assert.isTrue(result3.containsKey("caption"));
        Assert.isFalse(result3.containsKey("description"));
        Assert.isTrue(result3.containsKey("broadcast_chat_path"));
        Assert.isTrue(result3.containsKey("status"));
        Assert.equals("CREATED", result2.get("status"));
    }

    @Test
    public void testCreateChatWhenConnectionUser() throws IOException {
        CalendarEvent calendarEvent = addCalendarEvent("1", Instant.now().plus(Duration.standardMinutes(70)));

        String conferenceUri = createConferenceWithBroadcastFeature();

        HttpResponse response = helper.post("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                "/link_calendar_event?uid=" + TEST_UID.asString() + "&calendar_event_id=" + calendarEvent.getEventId());

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Создание трансляции (чата быть не должно)
        response = helper.put("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                "/broadcast?uid=" + TEST_UID.asString(), "{}");

        Assert.equals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        Map<String, Object>
                result1 = mapper.readValue(helper.getResult(response), new TypeReference<HashMap<String, Object>>() {
        });

        checkBroadcastData(result1, false);

        // Подключение докладчика, когда до встречи осталось немного (чат должен быть создан)
        addCalendarEvent("1", Instant.now().plus(Duration.standardMinutes(4)));

        Map<String, Object>
                result2 = joinToConferenceAndGetState("/v2/conferences/" + UrlUtils.urlEncode(conferenceUri) +
                "/connection?uid=" + JOIN_UID.asString(), ParticipantType.USER);

        Assert.isTrue(result2.containsKey("broadcast_data"));

        Map<String, Object> broadcastData = (Map<String, Object>) result2.get("broadcast_data");

        checkBroadcastData(broadcastData, true);
    }

}
