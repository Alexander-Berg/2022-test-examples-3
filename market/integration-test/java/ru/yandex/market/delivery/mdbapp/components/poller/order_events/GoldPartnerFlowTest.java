package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import steps.utils.TestableClock;

import ru.yandex.market.delivery.mdbapp.MockContextualTest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class GoldPartnerFlowTest extends MockContextualTest {

    @Autowired
    private TestableClock clock;

    @Autowired
    @Qualifier("orderEventsPoller0")
    private OrderEventsPoller poller;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    private MockRestServiceServer checkouterMockServer;

    @Before
    public void setUp() {
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);
        clock.setFixed(Instant.parse("2019-07-20T00:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    public void createFakeTrackFlow() throws Exception {
        mockCheckouterGetOrderHistoryEventsMethod("/data/events/gold_partner_without_track_event.json");
        mockCheckouterUpdateOrderDeliveryMethod(
            "/data/controller/request/update_gold_partner_order_request.json"
        );

        poller.poll();

        checkouterMockServer.verify();
    }

    @Test
    public void ignoreEventWhenActualOrderHasTrack() throws Exception {
        mockCheckouterGetOrderHistoryEventsMethod("/data/events/gold_partner_with_track_event.json");

        poller.poll();

        checkouterMockServer.verify();
    }

    private void mockCheckouterGetOrderHistoryEventsMethod(String responseFilePath) throws Exception {
        checkouterMockServer.expect(requestTo(StringContains.containsString("/orders/events")))
            .andRespond(withSuccess(
                getBody(responseFilePath),
                MediaType.APPLICATION_JSON_UTF8
            ));
    }

    private void mockCheckouterUpdateOrderDeliveryMethod(String requestFilePath) throws Exception {
        checkouterMockServer
                .expect(requestTo(StringContains.containsString("/orders/7535763/delivery/parcels")))
            .andExpect(content().json(getBody(requestFilePath)))
            .andRespond(withSuccess());
    }

    private String getBody(String filePath) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(filePath);
        return IOUtils.toString(inputStream, UTF_8);
    }
}
