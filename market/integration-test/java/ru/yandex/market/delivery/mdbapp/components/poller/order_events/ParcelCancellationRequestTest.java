package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.curator.managers.OrderEventManager;
import ru.yandex.market.delivery.mdbapp.components.queue.parcel.CancelParcelDto;
import ru.yandex.market.delivery.mdbapp.integration.service.CancelParcelEnqueueService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.PARCEL_CANCELLATION_REQUESTED;

/**
 * Тест постановки задачи отмены посылки в очередь.
 */
public class ParcelCancellationRequestTest extends AllMockContextualTest {

    private static final Long ORDER_ID = 2106833L;
    private static final Long PARCEL_ID = 1012374L;

    @Autowired
    @Qualifier("orderEventsPoller48")
    private OrderEventsPoller poller;

    @MockBean
    @Qualifier("eventManager8")
    private OrderEventManager eventManager;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    private MockRestServiceServer checkouterMockServer;

    @Autowired
    private CancelParcelEnqueueService cancelParcelEnqueueService;

    @Before
    public void setUp() throws Exception {
        when(eventManager.getId()).thenReturn(1L);
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);
    }

    @Test
    public void eventWasProcessedCorrectly() throws Exception {
        InputStream inputStream = this.getClass().getResourceAsStream("/data/events/parcel-cancellation-request.json");
        checkouterMockServer.expect(requestTo(StringContains.containsString("/orders/events")))
            .andExpect(queryParamContainsValue("eventTypes", PARCEL_CANCELLATION_REQUESTED.name()))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                MockRestResponseCreators.withSuccess(
                    IOUtils.toString(inputStream, UTF_8),
                    MediaType.APPLICATION_JSON_UTF8
                )
            );

        poller.poll();

        checkouterMockServer.verify();
        Mockito.verify(cancelParcelEnqueueService).enqueue(new CancelParcelDto(ORDER_ID, PARCEL_ID));
    }

    private RequestMatcher queryParamContainsValue(String queryParam, String value) {
        return (request) -> {
            URI uri = request.getURI();
            List<String> strings = UriComponentsBuilder.fromUri(uri).build().getQueryParams().get(queryParam);
            assertTrue("Query param [" + queryParam + "]", strings.contains(value));
        };
    }
}
