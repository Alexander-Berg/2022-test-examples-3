package ru.yandex.reminders.logic.callmeback;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.val;
import one.util.streamex.StreamEx;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.commune.json.JsonObject;
import ru.yandex.commune.json.JsonString;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.reminders.api.reminder.ChannelsData;
import ru.yandex.reminders.api.reminder.ReminderData;
import ru.yandex.reminders.api.reminder.ReminderDataConverter;
import ru.yandex.reminders.logic.event.EventData;
import ru.yandex.reminders.logic.event.EventId;
import ru.yandex.reminders.logic.event.SpecialClientIds;
import ru.yandex.reminders.logic.reminder.Reminder;
import ru.yandex.reminders.util.TestUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes =CallmebackDevpackContextConfiguration.class)
public class CallmebackManagerTest extends TestUtils {
    @Autowired
    private CallmebackManager manager;
    @Rule
    public WireMockRule rule;

    private static final AtomicLong UID_COUNTER = new AtomicLong(0);
    private static final String CLIENT_ID = SpecialClientIds.HOTEL;

    @Autowired
    public void setRule(DevPackConfig devPackConfig) {
        rule = new WireMockRule(wireMockConfig().port(devPackConfig.getDictitems().getReminders().getPort()));
    }

    private static DateTime tomorrow() {
        return DateTime.now().plusDays(1);
    }

    private void createReminder(PassportUid uid, int days) {
        val eventId = new EventId(uid, CLIENT_ID);
        val date = DateTime.now().plusDays(days);

        val eventData = getData(eventId.getCid(), "callmeback reminder", date,
                "This is not a spam, but a serious business offer");
        manager.createEvent(eventId, eventData, Optional.empty());
    }

    private ChannelsData getSmsAndEmail(String text) {
        val sms = new ChannelsData.Sms(Option.of(text));
        val email = new ChannelsData.Mail(Option.empty(), Option.empty(), Option.empty(), Option.of(text));

        return new ChannelsData(Optional.of(sms), Optional.of(email),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    private EventData getData(String clientId, String name, DateTime date, String text) {
        return getData(clientId, name, date, text, Option.empty());
    }

    private EventData getData(String clientId, String name, DateTime date, String text, Option<JsonObject> data) {
        return ReminderDataConverter.convertToEventData(new ReminderData(
                        Option.of(name), Option.empty(), Option.of(date),
                        getSmsAndEmail(text), data),
                Option.empty(), clientId);
    }

    private PassportUid getUid() {
        val uid = PassportUid.cons(UID_COUNTER.incrementAndGet());
        return uid;
    }

    @Test
    public void callToRandomGroupShouldReturnEmptyList() {
        val result = manager.listEvents(getUid(), CLIENT_ID);
        assertThat(result.getIds()).isEmpty();
    }

    @Test
    public void callToRandomEventShouldReturnEmptyList() {
        val result = manager.findEvent(new EventId(getUid(), CLIENT_ID));
        assertThat(result).isEmpty();
    }

    @Test
    public void deleteOfRandomEventShouldPass() {
        manager.removeEvent(new EventId(getUid(), CLIENT_ID));
    }

    @Test
    public void createAndFind() {
        val eventId = new EventId(getUid(), CLIENT_ID);
        val date = tomorrow();

        val eventData = getData(eventId.getCid(), "callmeback reminder", date,
                "This is not a spam, but a serious business offer");
        manager.createEvent(eventId, eventData, Optional.empty());
        val callmebackResponse = manager.findEvent(eventId);

        assertThat(callmebackResponse).isPresent();
        val callmebackData = callmebackResponse.get().getData();
        assertThat(callmebackData.getName()).isEqualTo(eventData.getName());
        assertThat(callmebackData.getDescription()).isEmpty();
        assertThat(callmebackData.getChannels().getSms()).isNotEmpty();
        assertThat(callmebackData.getChannels().getEmail()).isNotEmpty();
        assertThat(callmebackData.getReminderDate()).isEqualTo(Option.of(date));
    }

    @Test
    public void createDeleteAndFind() {
        val eventId = new EventId(getUid(), CLIENT_ID);
        val date = tomorrow();

        val eventData = getData(eventId.getCid(), "callmeback reminder", date,
                "This is not a spam, but a serious business offer");
        manager.createEvent(eventId, eventData, Optional.empty());
        manager.removeEvent(eventId);
        assertThat(manager.findEvent(eventId)).isEmpty();
    }

    @Test
    public void createAndList() {
        val firstUid = getUid();
        val secondUid = getUid();

        StreamEx.of(firstUid, secondUid)
                .cross(1,2,3)
                .forEach(entry -> createReminder(entry.getKey(), entry.getValue()));

        val list = manager.listEvents(firstUid, CLIENT_ID);
        assertThat(list.getIds()).hasSize(3);
    }

    @Test
    public void createAndGetCallback() throws InterruptedException {
        stubFor(post("/callmeback/send").willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
        val eventId = new EventId(getUid(), CLIENT_ID);
        val date = DateTime.now().plusSeconds(1);

        val eventData = getData(eventId.getCid(), "callmeback reminder", date,
                "This is not a spam, but a serious business offer");
        manager.createEvent(eventId, eventData, Optional.empty());
        Thread.sleep(2000);
        verify(postRequestedFor(urlEqualTo("/callmeback/send")).withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void createAndUpdate() {
        val eventId = new EventId(getUid(), CLIENT_ID);
        val date = tomorrow();
        val eventData = getData(CLIENT_ID, "callmeback_reminder", date, "Not a spam");
        manager.createEvent(eventId, eventData, Optional.empty());

        val updatedData = getData(CLIENT_ID, "after update", date, "Not a spam");
        manager.createOrUpdateEvent(eventId, updatedData, Optional.empty());

        val callmebackData = manager.findEvent(eventId);
        assertThat(callmebackData).isPresent();
        assertThat(callmebackData.get().getData().getName()).isEqualTo(updatedData.getName());
    }

    @Test
    public void createWithJsonObjectAndFind() {
        val eventId = new EventId(getUid(), CLIENT_ID);
        val date = tomorrow();
        val jsonObject = new JsonObject(Collections.singletonMap("test", JsonString.valueOf("hi")));
        val eventData = getData(CLIENT_ID, "callmeback_reminder", date, "Not a spam",
                Option.of(jsonObject));
        manager.createEvent(eventId, eventData, Optional.empty());

        val callmebackData = manager.findEvent(eventId);
        assertThat(callmebackData).isPresent();
        assertThat(callmebackData.get().getData().getData()).isEqualTo(Option.of(jsonObject));
    }

    @Test
    public void createAndFindRussian() {
        val eventId = new EventId(getUid(), CLIENT_ID);
        val date = tomorrow();
        val text = "Здесь русский дух, здесь Русью пахнет";

        val eventData = getData(eventId.getCid(), "callmeback reminder", date,
                text);
        manager.createEvent(eventId, eventData, Optional.empty());
        val callmebackResponse = manager.findEvent(eventId);

        assertThat(callmebackResponse).isPresent();
        val callmebackData = callmebackResponse.get().getData();
        assertThat(callmebackData.getName()).isEqualTo(eventData.getName());
        assertThat(callmebackData.getDescription()).isEmpty();
        assertThat(callmebackData.getChannels().getSms()).isNotEmpty();
        assertThat(callmebackData.getChannels().getEmail()).isNotEmpty();
        assertThat(callmebackData.getChannels().getSms()
                .map(sms -> sms.toReminder(DateTime.now()))
                .flatMapO(Reminder::getText)).isEqualTo(Option.of(text));
        assertThat(callmebackData.getReminderDate()).isEqualTo(Option.of(date));
    }

    @Test
    public void createAndGetRussianCallback() throws InterruptedException {
        stubFor(post("/callmeback/send").willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
        val eventId = new EventId(getUid(), CLIENT_ID);
        val date = DateTime.now().plusSeconds(1);
        val text = "Здесь русский дух, здесь Русью пахнет";

        val eventData = getData(eventId.getCid(), text, date, text);
        manager.createEvent(eventId, eventData, Optional.empty());
        Thread.sleep(2000);
        // We expect 3 requests from callmeback - panel (hotel specific), email and sms
        verify(3, postRequestedFor(
                urlEqualTo("/callmeback/send"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(matchingJsonPath("$.data.name", containing(text)))
        );
    }
}
