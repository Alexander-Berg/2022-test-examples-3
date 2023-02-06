package ru.yandex.market.antifraud.orders.service;

import java.util.Set;

import gumi.builders.UrlBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.config.CheckouterClientConfiguration;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.BulkOrderCancellationResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.NullifyCashbackEmitResponseDto;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author dzvyagin
 */
@RunWith(SpringRunner.class)
@RestClientTest
public class CheckouterClientTest {


    @Autowired
    private MockRestServiceServer mockServer;
    private CheckouterClient checkouterClient;

    @Before
    public void init() {
        RestTemplate template = CheckouterClientConfiguration.createTemplate(tvmClient(), 2);
        mockServer = MockRestServiceServer.createServer(template);
        mockServer.reset();
        checkouterClient = new CheckouterClient(UrlBuilder.fromString("http://localhost:8080"), template);
    }

    public TvmClient tvmClient() {
        TvmClient client = Mockito.mock(TvmClient.class);
        when(client.getServiceTicketFor(anyInt())).thenReturn("ticket");
        return client;
    }

    @Test
    public void cancelOrders() {
        mockServer.expect(requestTo("http://localhost:8080/orders/cancellation-request?clientRole=ANTIFRAUD_ROBOT&clientId=0&rgb=BLUE"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Ya-Service-Ticket", "ticket"))
                .andExpect(queryParam("clientRole", "ANTIFRAUD_ROBOT"))
                .andExpect(queryParam("clientId", "0"))
                .andExpect(queryParam("rgb", "BLUE"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json("{\"cancellationRequest\":{\"substatus\":\"USER_FRAUD\",\"notes\":\"Считаем фродом\",\"substatusText\":\"USER_FRAUD\"},\"orderIds\":[123,124,125]}\n"))
                .andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON_UTF8).body("{\"cancelledOrders\": [123,124,125], \"failedOrders\": []}"));
        BulkOrderCancellationResponseDto response = checkouterClient.cancelOrders(Set.of(123L, 124L, 125L));
        assertThat(response.getSucceededOrders()).contains(123L, 124L, 125L);
        assertThat(response.getFailedOrders()).isEmpty();
    }

    @Test
    public void nullifyCashbackEmit() {
        mockServer.expect(requestTo("http://localhost:8080/orders/nullify-cashback-emit?clientRole=ANTIFRAUD_ROBOT&clientId=0"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Ya-Service-Ticket", "ticket"))
                .andExpect(queryParam("clientRole", "ANTIFRAUD_ROBOT"))
                .andExpect(queryParam("clientId", "0"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json("{\"orderIds\":[123,124,125]}\n"))
                .andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON_UTF8).body("{\"succeededOrderIds\": [123,124,125], \"failedOrderIds\": []}"));
        NullifyCashbackEmitResponseDto response = checkouterClient.nullifyCashbackEmit(Set.of(123L, 124L, 125L));
        assertThat(response.getSucceededOrders()).contains(123L, 124L, 125L);
        assertThat(response.getFailedOrders()).isEmpty();
    }

}
