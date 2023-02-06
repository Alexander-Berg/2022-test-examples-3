package ru.yandex.market.delivery.mdbapp.components.email.sender;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.delivery.mdbapp.AbstractContextualTest;
import ru.yandex.market.delivery.mdbapp.components.email.Mail;
import ru.yandex.market.delivery.mdbapp.configuration.MailSenderConfiguration;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.sc.internal.client.ScIntClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class MailSenderContextTest extends AbstractContextualTest {

    private MockRestServiceServer restServer;

    @MockBean
    @SuppressWarnings("unused")
    private PechkinHttpClient pechkinHttpClient;

    @MockBean
    @SuppressWarnings("unused")
    private LMSClient lmsClient;

    @MockBean
    @SuppressWarnings("unused")
    private ScIntClient scIntClient;

    @Autowired
    private MailSender sender;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MailSenderConfiguration config;

    @Before
    public void setUp() {
        restServer = MockRestServiceServer.createServer(config.getEmailSenderRestTemplate());
    }

    @Test
    public void testSenderLoads() {
        MailSender mailSender = context.getBean(MailSender.class);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(mailSender).as("Bean loaded").isNotNull();
        softly.assertThat(mailSender.getUrl()).as("Correctly parsed sender's url")
            .isEqualTo("http://sender-url");
        softly.assertThat(mailSender.getEncodedAuthToken()).as("Correctly parsed user token")
            .isEqualTo(getEncodedUserToken());
        softly.assertAll();
    }

    @Test
    public void testMessageSent() {
        restServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo("http://sender-url"))
            .andRespond(withSuccess(getResponse(), MediaType.APPLICATION_JSON));
        sender.send(buildMail());
    }

    private String getEncodedUserToken() {
        byte[] bytes = ("user-token" + ":").getBytes();
        byte[] encode = Base64.getEncoder().encode(bytes);
        return new String(encode);
    }

    private Mail buildMail() {
        return new Mail(StandardCharsets.UTF_8, "sender", "receiver", "sender",
            "mail subject", "body");
    }

    private static String getResponse() {
        return "{\n" +
            "  \"params\": {\n" +
            "    \"control\": {\n" +
            "      \"async\": false,\n" +
            "      \"countdown\": null,\n" +
            "      \"expires\": 86400,\n" +
            "      \"for_testing\": false\n" +
            "    },\n" +
            "    \"source\": {\n" +
            "      \"to_email\": \"s@lavr.me\",\n" +
            "      \"header\": [],\n" +
            "      \"ignore_empty_email\": false\n" +
            "    }\n" +
            "  },\n" +
            "  \"result\": {\n" +
            "    \"status\": \"ERROR\",\n" +
            "    \"message\": \"TemplateRuntimeError: \"\n" +
            "  }\n" +
            "}";
    }
}
