package ru.yandex.market.antifraud.orders.external.volva;

import java.util.LinkedHashSet;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.volva.dto.GetGluesResponseDto;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.serializer.VolvaJsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest
@ContextConfiguration(classes = {VolvaTestConfig.class})
public class HttpVolvaClientTest {

    @Autowired
    private AsyncRestTemplate volvaRestTemplate;
    @Autowired
    private HttpVolvaClient httpVolvaClient;
    @Autowired
    private MockRestServiceServer mockServer;

    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(volvaRestTemplate);
        mockServer.reset();
    }

    @Test
    @SneakyThrows
    public void getGluedIds() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("content-type", "application/json");
        var responseDto = GetGluesResponseDto.builder()
                .gluedNodes(List.of(
                        new Node("123", IdType.PUID),
                        new Node("u2345", IdType.UUID),
                        new Node("c4535", IdType.CARD)
                ))
                .build();
        mockServer.expect(requestTo("/glues?uuid=u2345&uuid=u9876&card=c4535&accept=PUID&accept=UUID&accept=CARD"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess().body(VolvaJsonUtils.OBJECT_MAPPER.writeValueAsBytes(responseDto)).headers(headers));
        var volvaGluedIds = httpVolvaClient.getGluedIds(List.of(
                MarketUserId.fromUuid("u2345"),
                MarketUserId.fromUuid("u9876"),
                new MarketUserId(null, "c4535", "card", null)
        ), new LinkedHashSet<>(List.of(MarketUserId.UID_STR, MarketUserId.UUID_STR, MarketUserId.CARD))).get();
        assertThat(volvaGluedIds)
                .containsExactlyInAnyOrder(
                        MarketUserId.fromUid(123L),
                        MarketUserId.fromUuid("u2345"),
                        MarketUserId.builder().userId("c4535").idType("card").build()
                );
    }
}
