package ru.yandex.market.checkout.checkouter.client;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.TvmTicketProvider;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * @author : poluektov
 * date: 2019-10-25.
 */
public class TvmHeaderTest extends AbstractWebTestBase {

    public static final String SERVICE_TICKET_HEADER = "X-Ya-Service-Ticket";
    ClientHttpRequestFactory originalRequestFactory;
    private MockRestServiceServer server;
    @Autowired
    private RestTemplate checkouterRestTemplate;
    @Autowired
    @Qualifier("tvmTicketProviderTest")
    private TvmTicketProvider tvmTicketProvider;

    @BeforeEach
    public void setUp() throws IOException {
        originalRequestFactory = checkouterRestTemplate.getRequestFactory();
        server = MockRestServiceServer.createServer(checkouterRestTemplate);
    }

    //get requests
    @Test
    public void getOrderById() {
        server.expect(requestTo(containsString("/orders/123")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            client.getOrder(123L, ClientRole.SYSTEM, null);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @Test
    public void getOrderItems() {
        server.expect(requestTo(containsString("/orders/123/items")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            client.getOrderItems(new RequestClientInfo(ClientRole.SYSTEM, null),
                    BasicOrderRequest.builder(123L).build());
        } catch (Exception ignored) {
        }
        server.verify();
    }


    //post requests
    @Test
    public void postCart() {
        server.expect(requestTo(containsString("/cart")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            client.cart(new MultiCart(), 1234);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    //put requests
    @Test
    public void putOrderItems() {
        server.expect(requestTo(containsString("/orders/123/items")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()));
        try {
            client.putOrderItems(123L, new OrderItems(), ClientRole.SYSTEM, 1L);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @Test
    public void testWithOtherHeaders() {
        server.expect(requestTo(containsString("/orders/by-bind-key/bindKey1")))
                .andExpect(header(SERVICE_TICKET_HEADER, tvmTicketProvider.getServiceTicket().get()))
                .andExpect(header("Cookie", "muid=muidCookie"))
                .andExpect(header("X-Hit-Rate-Group", HitRateGroup.LIMIT.name()));
        try {
            client.getOrderByBindKey("bindKey1", new String[]{"88005553535"}, null, "muidCookie", true,
                    HitRateGroup.LIMIT);
        } catch (Exception ignored) {
        }
        server.verify();
    }

    @AfterEach
    public void restoreRequestFactory() {
        checkouterRestTemplate.setRequestFactory(originalRequestFactory);
    }
}
