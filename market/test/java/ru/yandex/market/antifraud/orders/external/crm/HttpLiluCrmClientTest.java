package ru.yandex.market.antifraud.orders.external.crm;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.crm.platform.commons.Response;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.AntifraudBlockingEvent;
import ru.yandex.market.crm.platform.profiles.Facts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author dzvyagin
 */
@RunWith(SpringRunner.class)
@RestClientTest
@ContextConfiguration(classes = {LiluCrmTestConfig.class})
public class HttpLiluCrmClientTest {

    @Autowired
    private RestTemplate crmRestTemplate;
    @Autowired
    private HttpLiluCrmClient httpLiluCrmClient;
    @Autowired
    private MockRestServiceServer mockServer;

    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(crmRestTemplate);
        mockServer.reset();
    }

    @Test
    public void saveFact() {
        mockServer.expect(requestTo("http://test.ya.ru/facts/AntifraudBlockingEvent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(this::checkParseableProtobuf)
                .andRespond(withSuccess());
        AntifraudBlockingEvent abe = AntifraudBlockingEvent.newBuilder()
                .setKeyUid(Uid.newBuilder()
                        .setType(UidType.PUID)
                        .setStringValue(String.valueOf(123L))
                        .build())
                .setTimestamp(LocalDate.of(2020, 1, 1)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                        .toEpochMilli())
                .setBlockingType("LOYALTY")
                .addTriggeredRules(
                        AntifraudBlockingEvent.AntifraudRuleTriggerEvent.newBuilder()
                                .setRuleName("test_rule")
                                .setDescription("test_rule_description")
                                .build()
                )
                .build();
        httpLiluCrmClient.saveFact("AntifraudBlockingEvent", abe);
    }

    @Test
    public void getFacts() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", "application/x-protobuf");
        Response response = Response.newBuilder()
                .setFacts(Facts.newBuilder().build())
                .build();
        mockServer.expect(requestTo("http://test.ya.ru/facts/strong?puid=123&facts=fukkkt&ts_min=0&ts_max=999"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("puid", "123"))
                .andExpect(queryParam("facts", "fukkkt"))
                .andExpect(queryParam("ts_min", "0"))
                .andExpect(queryParam("ts_max", "999"))
                .andRespond(withSuccess().body(response.toByteArray()).headers(headers));
        Facts facts = httpLiluCrmClient.getFacts("fukkkt", 123L, 0L, 999L);
        assertThat(facts).isNotNull();
    }

    private void checkParseableProtobuf(ClientHttpRequest request) {
        try {
            OutputStream stream = request.getBody();
            byte[] data = ((ByteArrayOutputStream) stream).toByteArray();
            System.out.println(AntifraudBlockingEvent.parseFrom(data));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void testProtobufParsing() throws Exception {
        AntifraudBlockingEvent abe = AntifraudBlockingEvent.newBuilder()
                .setKeyUid(Uid.newBuilder()
                        .setType(UidType.PUID)
                        .setStringValue(String.valueOf(123L))
                        .build())
                .setTimestamp(Instant.now().getEpochSecond())
                .setBlockingType("ORDER")
                .build();
        byte[] bytes = abe.toByteArray();
        System.out.println(AntifraudBlockingEvent.parseFrom(bytes));
    }
}
