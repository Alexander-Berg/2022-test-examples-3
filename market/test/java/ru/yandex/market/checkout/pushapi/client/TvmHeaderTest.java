package ru.yandex.market.checkout.pushapi.client;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.common.rest.TvmTicketProvider;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * @author ifilippov5
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"classpath:clientTests.xml", "classpath:WEB-INF/push-api-client.xml"})
public class TvmHeaderTest {

    public static final String SERVICE_TICKET_HEADER = "X-Ya-Service-Ticket";

    @Autowired
    private RestPushApiClient pushApiClient;
    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer server;
    private TvmTicketProvider tvmTicketProvider = new TvmTicketProviderTestImpl();

    @BeforeEach
    public void setUp() throws IOException {
        server = MockRestServiceServer.createServer(restTemplate);
        pushApiClient.setTvmTicketProvider(tvmTicketProvider);
    }

    @Test
    public void cart() {
        server.expect(requestTo(containsString("/cart")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            pushApiClient.cart(123L, 123L, null, false, null, null, null);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @Test
    public void orderAccept() {
        server.expect(requestTo(containsString("/order/accept")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            pushApiClient.orderAccept(123L, null, false, null, null, null);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @Test
    public void orderStatus() {
        server.expect(requestTo(containsString("/order/status")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            pushApiClient.orderStatus(123L, null, false, null, null, null);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @Test
    public void shipmentStatus() {
        server.expect(requestTo(containsString("/order/shipment/status")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            pushApiClient.shipmentStatus(123L, null, false, null, null, null);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @Test
    public void itemsChange() {
        server.expect(requestTo(containsString("/order/items")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            pushApiClient.itemsChange(123L, null, false, null, null, null);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @Test
    public void settings() {
        server.expect(requestTo(containsString("/settings")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            pushApiClient.settings(123L, null, false);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @Test
    public void wrongTokenCart() {
        server.expect(requestTo(containsString("/cart/wrong-token")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            pushApiClient.wrongTokenCart(123L, 123L, null, false, null, null, null);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @Test
    public void queryStocks() {
        server.expect(requestTo(containsString("/stocks")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            pushApiClient.queryStocks(123L, null, false, null, null, false);
        } catch (Exception ignored) {
        }
        server.verify();
    }

}
