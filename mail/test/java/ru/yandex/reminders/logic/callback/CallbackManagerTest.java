package ru.yandex.reminders.logic.callback;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.apache.http.HttpStatus;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.test.Assert;
import ru.yandex.reminders.logic.event.Event;
import ru.yandex.reminders.logic.event.EventId;
import ru.yandex.reminders.logic.reminder.Reminder;
import ru.yandex.reminders.logic.reminder.SendStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


public class CallbackManagerTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort(), false);
    private final CallbackManager callbackManager = new CallbackManager(10, Timeout.seconds(20), Cf.list());

    @Before
    public void setUp() {
        stubFor(get("/").willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
    }

    @Test
    public void invokeOk() {
        SendStatus sendStatus = callbackManager.invoke(createEventId(), createReminder(getAddress()));
        Assert.isTrue(sendStatus.isSent());
    }

    @Test
    public void invokeFailsWith404() {
        SendStatus sendStatus = callbackManager.invoke(createEventId(), createReminder(String.format("%s/test-404", getAddress())));
        Assert.isTrue(sendStatus.isTryAgain());
        Assert.isTrue(sendStatus.asTryAgain().getMessage().contains("404"));
    }

    private String getAddress() {
        return String.format("http://localhost:%d", wireMockRule.port());
    }

    private EventId createEventId() {
        return new EventId(PassportUid.cons(1), "doesn't matter");
    }

    private Reminder createReminder(String url) {
        return Reminder.callback(DateTime.now(), url);
    }
}
