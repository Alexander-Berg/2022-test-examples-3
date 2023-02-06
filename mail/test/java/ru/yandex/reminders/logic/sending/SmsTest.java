package ru.yandex.reminders.logic.sending;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.val;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.sms.PassportSmsService;
import ru.yandex.reminders.boot.InitContextConfiguration;
import ru.yandex.reminders.util.TestUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes =
        {
                SmsTestContextConfiguration.class,
                InitContextConfiguration.class
        })
public class SmsTest extends TestUtils {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());
    @Autowired
    private SmsSender sender;
    @Autowired
    private PassportSmsService service;

    @Before
    public void setUp() {
        val body = "<?xml version=\"1.0\" encoding=\"windows-1251\"?><doc><message-sent id=\"42\" /></doc>";
        stubFor(get(urlPathEqualTo("/sendsms")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(body)));

        val host = String.format("localhost:%d", wireMockRule.port());
        service.setSmsPassportUrlHost(host);
    }

    @Test
    public void sendToUid() {
        assertThat(sender.sendToUser(PassportUid.cons(42l), Option.empty(), "Robb Stark", "Westeros")).isEqualTo("42");
    }
}
