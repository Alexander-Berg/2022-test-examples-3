package ru.yandex.market.antifraud.orders.client;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
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
import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.UserMarkerDto;
import ru.yandex.market.antifraud.orders.entity.UserMarkerType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlacklistType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingEvent;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingEventGroup;
import ru.yandex.market.antifraud.orders.web.dto.crm.BlockingType;
import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerBlockingGroupsDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerBlockingsDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.BuyerInfoDto;
import ru.yandex.market.antifraud.orders.web.dto.crm.RefundPolicy;
import ru.yandex.market.antifraud.orders.web.dto.crm.RuleTriggerEvent;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author dzvyagin
 */
@RunWith(SpringRunner.class)
@RestClientTest(MstatAntifraudCrmClient.class)
@ContextConfiguration(classes = {TestConfig.class})
public class MstatAntifraudCrmClientTest {

    @Autowired
    private MstatAntifraudCrmClient testMstatAntifraudCrmClient;

    @Autowired
    private RestTemplate mstatAntifraudClientRestTemplate;

    private MockRestServiceServer mockServer;

    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(mstatAntifraudClientRestTemplate);
        mockServer.reset();
    }

    @Test
    public void checkBuyerInfo() throws Exception {
        BuyerInfoDto response = BuyerInfoDto.builder()
                .uid(123L)
                .blacklist(false)
                .blacklistType(BlacklistType.NOT_BLACKLISTED)
                .vip(false)
                .roleName("default")
                .refundPolicy(RefundPolicy.SIMPLE)
                .roleDescription("default role")
                .userMarkers(new HashSet<>(Arrays.asList(
                        UserMarkerDto.builder()
                                .name("bad_acc")
                                .showName("Подозрительный аккаунт")
                                .description("Массово созданный аккаунт")
                                .type(UserMarkerType.BAD)
                                .build(),
                        UserMarkerDto.builder()
                                .name("reseller")
                                .showName("Реселлер")
                                .description("Реселллер")
                                .type(UserMarkerType.BAD)
                                .build()
                )))
                .build();
        String responseJson =
                "{\"uid\":123,\"roleName\":\"default\",\"roleDescription\":\"default role\",\"vip\":false,\"blacklist\":false,\"blacklistType\":\"NOT_BLACKLISTED\",\"refundPolicy\":\"SIMPLE\",\"userMarkers\":[{\"name\":\"reseller\",\"showName\":\"Реселлер\",\"description\":\"Реселллер\",\"type\":\"BAD\"},{\"name\":\"bad_acc\",\"showName\":\"Подозрительный аккаунт\",\"description\":\"Массово созданный аккаунт\",\"type\":\"BAD\"}]}\n";
        mockServer.expect(requestTo("/crm/buyer/info?uid=123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("uid", "123"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        BuyerInfoDto result = testMstatAntifraudCrmClient.getInfo(123L);
        assertEquals(response, result);
        mockServer.verify();
    }

    @Test
    public void checkBlocking() {
        BuyerBlockingsDto response = BuyerBlockingsDto.builder()
                .uid(123L)
                .page(0)
                .pageSize(10)
                .total(1L)
                .blockingType(BlockingType.LOYALTY)
                .blockings(
                        Arrays.asList(
                                new BlockingEvent(123L, null, BlockingType.LOYALTY, Arrays.asList(
                                        new RuleTriggerEvent("test_rule", "test_description")
                                ))
                        )
                )
                .build();
        String responseJson = "{\"uid\":123," +
                "\"total\":1,\"page\":0," +
                "\"pageSize\":10," +
                "\"blockingType\":\"LOYALTY\"," +
                "\"blockings\":[{" +
                "\"uid\": 123," +
                "\"type\": \"LOYALTY\"," +
                "\"ruleTriggerEvents\":[{" +
                "\"ruleName\":\"test_rule\"," +
                "\"description\":\"test_description\"}]}]}\n";
        mockServer.expect(requestTo("/crm/buyer/blockings?uid=123&type=LOYALTY&page=0&pageSize=10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("uid", "123"))
                .andExpect(queryParam("type", "LOYALTY"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        BuyerBlockingsDto result = testMstatAntifraudCrmClient.getBlockings(123L, BlockingType.LOYALTY, 0, 10);
        assertEquals(response, result);
        mockServer.verify();
    }

    @Test
    public void checkBlockingGroups() {
        Instant ts1 = LocalDate.of(2019, 11, 2).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant ts2 = LocalDate.of(2019, 11, 2).atStartOfDay().toInstant(ZoneOffset.UTC)
                .plus(1L, ChronoUnit.HOURS);
        BuyerBlockingGroupsDto response = BuyerBlockingGroupsDto.builder()
                .uid(123L)
                .page(0)
                .pageSize(10)
                .total(2L)
                .blockingType(BlockingType.ORDER)
                .blockingGroups(
                        Arrays.asList(
                                BlockingEventGroup.builder()
                                        .uid(123L)
                                        .from(ts1)
                                        .to(ts1)
                                        .count(1)
                                        .restrictions(ImmutableSet.of(AntifraudAction.CANCEL_ORDER))
                                        .blockings(Arrays.asList(new BlockingEvent(123L, ts1, BlockingType.ORDER, Arrays.asList(
                                                new RuleTriggerEvent("r1", "d1"),
                                                new RuleTriggerEvent("r2", "d2")
                                        ))))
                                        .build(),
                                BlockingEventGroup.builder()
                                        .uid(123L)
                                        .from(ts2)
                                        .to(ts2)
                                        .count(1)
                                        .restrictions(ImmutableSet.of(AntifraudAction.CANCEL_ORDER))
                                        .blockings(Arrays.asList(new BlockingEvent(123L, ts2, BlockingType.ORDER, Arrays.asList(
                                                new RuleTriggerEvent("r1", "d1"),
                                                new RuleTriggerEvent("r2", "d2")
                                        )))).build()
                        )
                )
                .build();
        String responseJson = "{\"uid\":123,\"total\":2,\"page\":0,\"pageSize\":10,\"blockingType\":\"ORDER\"," +
                "\"blockingGroups\":[{\"uid\":123,\"from\":\"2019-11-02T00:00:00Z\"," +
                "\"to\":\"2019-11-02T00:00:00Z\",\"count\":1,\"restrictions\":[\"CANCEL_ORDER\"]," +
                "\"blockings\":[{\"uid\":123,\"timestamp\":\"2019-11-02T00:00:00Z\",\"type\":\"ORDER\"," +
                "\"ruleTriggerEvents\":[{\"ruleName\":\"r1\",\"description\":\"d1\"},{\"ruleName\":\"r2\"," +
                "\"description\":\"d2\"}]}]}," +
                "{\"uid\":123,\"from\":\"2019-11-02T01:00:00Z\"," +
                "\"to\":\"2019-11-02T01:00:00Z\",\"count\":1,\"restrictions\":[\"CANCEL_ORDER\"]," +
                "\"blockings\":[{\"uid\":123,\"timestamp\":\"2019-11-02T01:00:00Z\",\"type\":\"ORDER\"," +
                "\"ruleTriggerEvents\":[{\"ruleName\":\"r1\",\"description\":\"d1\"},{\"ruleName\":\"r2\"," +
                "\"description\":\"d2\"}]}]}]}\n";
        mockServer.expect(requestTo("/crm/buyer/blockings/grouped?uid=123&type=ORDER&from=1572566400000&to=1572739200000&page=0&pageSize=10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("uid", "123"))
                .andExpect(queryParam("type", "ORDER"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));
        Instant from = LocalDate.of(2019, 11, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = LocalDate.of(2019, 11, 3).atStartOfDay().toInstant(ZoneOffset.UTC);
        BuyerBlockingGroupsDto result = testMstatAntifraudCrmClient.getBlockingGroups(123L, BlockingType.ORDER, from, to, 0, 10);
        assertEquals(response, result);
        mockServer.verify();
    }

    @Test
    public void removeFromBlacklist() {
        mockServer.expect(requestTo("/metadata/blacklist"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json("{\"type\": \"UID\", \"value\": \"123\"," +
                        "\"reason\": \"Ocrm request: test\",\"authorUid\": -1}"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON_UTF8));
        testMstatAntifraudCrmClient.removeFromBlacklist(123L, "test");
        mockServer.verify();
    }
}
