package ru.yandex.market.antifraud.orders.client;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.client.config.TestConfig;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRule;
import ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.ANY;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.PHONE;
import static ru.yandex.market.antifraud.orders.entity.AntifraudBlacklistRuleType.UID;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 20.06.19
 */
@RunWith(SpringRunner.class)
@RestClientTest(MstatAntifraudOrdersClient.class)
@ContextConfiguration(classes = {TestConfig.class})
public class MstatAntifraudOrdersClientTest {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss z");
    private static final Long AUTHOR_UID = 55555555L;

    @Autowired
    private MstatAntifraudOrdersClient mstatAntifraudOrdersClient;

    @Autowired
    private RestTemplate mstatAntifraudClientRestTemplate;

    private MockRestServiceServer mockServer;

    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(mstatAntifraudClientRestTemplate);
        mockServer.reset();
    }


    @Test
    public void getBlacklist() throws ParseException {
        List<AntifraudBlacklistRule> expectedRules = new ArrayList<>();
        expectedRules.add(new AntifraudBlacklistRule(UID, "123", "AntifraudAction_CANCEL_ORDER", "some_reason_1",
                        DATE_FORMAT.parse("30-05-2045 00:00:00 +0300"), AUTHOR_UID
                )
        );

        String str =
                "[{\"type\":\"UID\",\"value\":\"123\",\"action\":\"AntifraudAction_CANCEL_ORDER\"," +
                        "\"reason\":\"some_reason_1\",\"expiryAt\":\"30-05-2045 00:00:00\",\"authorUid\":55555555}]";

        mockServer.expect(requestTo("/metadata/blacklist"))
                .andRespond(withSuccess(str, MediaType.APPLICATION_JSON));

        List<AntifraudBlacklistRule> rules = mstatAntifraudOrdersClient.getBlacklistRules();

        assertEquals(expectedRules, rules);
        mockServer.verify();
    }

    @Test
    public void postBlacklist() throws ParseException, JsonProcessingException {
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(UID, "123", "AntifraudAction_CANCEL_ORDER",
                "some_reason_1", DATE_FORMAT.parse("30-05-2045 00:00:00 +0000"), AUTHOR_UID);

        String str = new ObjectMapper().writeValueAsString(rule);

        mockServer.expect(requestTo("/metadata/blacklist"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(str, MediaType.APPLICATION_JSON));

        mstatAntifraudOrdersClient.postBlacklistRule(rule);
        mockServer.verify();
    }

    @Test
    @SneakyThrows
    public void getBlackListPage() {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        List<AntifraudBlacklistRule> expectedRules1 = Arrays.asList(
                new AntifraudBlacklistRule(AntifraudBlacklistRuleType.UID, "123", "AntifraudAction_CANCEL_ORDER",
                        "some_reason_1", ft.parse("30-05-2045 00:00:00 +0300"), AUTHOR_UID),
                new AntifraudBlacklistRule(AntifraudBlacklistRuleType.PHONE, "+7111111111", "AntifraudAction_CANCEL_ORDER",
                        "some_reason_2", ft.parse("30-05-2049 00:00:00 +0300"), AUTHOR_UID)
        );
        List<AntifraudBlacklistRule> expectedRules2 = Collections.emptyList();
        String response1 = "[" +
                "{\"type\":\"UID\",\"value\":\"123\",\"action\":\"AntifraudAction_CANCEL_ORDER\",\"reason\":\"some_reason_1\"," +
                "\"expiryAt\":\"30-05-2045 00:00:00\",\"authorUid\":55555555}," +
                "{\"type\":\"PHONE\",\"value\":\"+7111111111\",\"action\":\"AntifraudAction_CANCEL_ORDER\"," +
                "\"reason\":\"some_reason_2\",\"expiryAt\":\"30-05-2049 00:00:00\",\"authorUid\":55555555}" +
                "]";
        String response2 = "[]";
        mockServer.expect(requestTo("/metadata/blacklist?page=1&size=2"))
                .andRespond(withSuccess(response1, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("/metadata/blacklist?page=2&size=2&value=1&reason=2&uid=3&type=UID&type=PHONE"))
                .andRespond(withSuccess(response2, MediaType.APPLICATION_JSON));
        List<AntifraudBlacklistRule> rules1 = mstatAntifraudOrdersClient.getBlacklistRules(1, 2, null, null, null, null);
        List<AntifraudBlacklistRule> rules2 = mstatAntifraudOrdersClient
                .getBlacklistRules(2, 2, Lists.newArrayList(UID, PHONE), "1", "2", 3L);
        assertEquals(expectedRules1, rules1);
        assertEquals(expectedRules2, rules2);
        mockServer.verify();
    }

    @Test
    @SneakyThrows
    public void removeBlacklist() {
        AntifraudBlacklistRule rule = new AntifraudBlacklistRule(UID, "123", "AntifraudAction_CANCEL_ORDER", "some_reason_1",
                DATE_FORMAT.parse("30-05-2045 00:00:00 +0000"), AUTHOR_UID);

        String str = new ObjectMapper().writeValueAsString(rule);

        mockServer.expect(requestTo("/metadata/blacklist"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess(str, MediaType.APPLICATION_JSON));

        mstatAntifraudOrdersClient.removeBlacklistRule(rule);
        mockServer.verify();
    }

    @Test
    @SneakyThrows
    public void findBlacklists() {
        SimpleDateFormat ft = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss z");
        List<AntifraudBlacklistRule> expectedRules1 = Arrays.asList(
                new AntifraudBlacklistRule(AntifraudBlacklistRuleType.UID, "123", "AntifraudAction_CANCEL_ORDER", "some_reason_1",
                        ft.parse("30-05-2045 00:00:00 +0300"), AUTHOR_UID),
                new AntifraudBlacklistRule(AntifraudBlacklistRuleType.PHONE, "+7111111111", "AntifraudAction_CANCEL_ORDER",
                        "some_reason_2", ft.parse("30-05-2049 00:00:00 +0300"), AUTHOR_UID)
        );
        List<AntifraudBlacklistRule> expectedRules2 = Collections.emptyList();
        String response1 = "[" +
                "{\"type\":\"UID\",\"value\":\"123\",\"action\":\"AntifraudAction_CANCEL_ORDER\"," +
                "\"reason\":\"some_reason_1\",\"expiryAt\":\"30-05-2045 00:00:00\",\"authorUid\":55555555}," +
                "{\"type\":\"PHONE\",\"action\":\"AntifraudAction_CANCEL_ORDER\",\"value\":\"+7111111111\"," +
                "\"reason\":\"some_reason_2\",\"expiryAt\":\"30-05-2049 00:00:00\",\"authorUid\":55555555}" +
                "]";
        String response2 = "[]";
        mockServer.expect(requestTo("/metadata/blacklist/search?type=ANY&value=1"))
                .andRespond(withSuccess(response1, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("/metadata/blacklist/search?type=ANY&value=22"))
                .andRespond(withSuccess(response2, MediaType.APPLICATION_JSON));

        List<AntifraudBlacklistRule> rules1 = mstatAntifraudOrdersClient.findBlacklistRule(ANY, "1");
        List<AntifraudBlacklistRule> rules2 = mstatAntifraudOrdersClient.findBlacklistRule(ANY, "22");
        assertEquals(expectedRules1, rules1);
        assertEquals(expectedRules2, rules2);
        mockServer.verify();
    }
}
