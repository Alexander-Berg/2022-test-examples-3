package ru.yandex.direct.telephony.client;

import java.io.IOException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.telephony.client.model.GetTicketResponse;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.inside.passport.tvm2.TvmHeaders;

import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public class TelephonyClientTest {
    private final static Logger logger = LoggerFactory.getLogger(TelephonyClientTest.class);

    private String mockResponse = "{\n" +
            "  \"ticketId\": \"ae0d3159-2345-c9d9-c9c2-e39cb30bb5d9\",\n" +
            "  \"apiPort\": 3000,\n" +
            "  \"apiHost\": \"[2a02:6b8:c0c:740c:0:4d88:54b1:0]\",\n" +
            "  \"ttl\": 30000,\n" +
            "  \"apiKind\": \"admin-api\"\n" +
            "}";

    private MockResponse response = new MockResponse().setBody(mockResponse);

    static final String TICKET_BODY = "ticketBody";

    private MockWebServer mockWebServer;
    @Rule
    public JUnitSoftAssertions softAssertions = new JUnitSoftAssertions();

    @Autowired
    public ParallelFetcherFactory parallelFetcherFactory;

    TelephonyClient telephonyClient;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();

        TvmIntegration tvmIntegration = mock(TvmIntegration.class);
        Mockito.when(tvmIntegration.isEnabled()).thenReturn(true);
        Mockito.when(tvmIntegration.getTicket(TvmService.DUMMY)).thenReturn(TICKET_BODY);
        telephonyClient =
                new TelephonyClient(url(), tvmIntegration, TvmService.DUMMY, parallelFetcherFactory, () -> "");
    }

    protected String url() {
        return "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();
    }

    @After
    public void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }

    @Test
    public void getTicket() {
        GetTicketResponse tickets = telephonyClient.getTicket();
        GetTicketResponse expected = new GetTicketResponse()
                .withApiHost("[2a02:6b8:c0c:740c:0:4d88:54b1:0]")
                .withApiPort(3000)
                .withTicketId("ae0d3159-2345-c9d9-c9c2-e39cb30bb5d9");
        softAssertions.assertThat(tickets.getApiHost()).isEqualTo(expected.getApiHost());
        softAssertions.assertThat(tickets.getApiPort()).isEqualTo(expected.getApiPort());
        softAssertions.assertThat(tickets.getTicketId()).isEqualTo(expected.getTicketId());
    }

    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getHeader(TvmHeaders.SERVICE_TICKET)).isEqualTo(TICKET_BODY);
                softAssertions.assertThat(request.getHeader("Content-type")).isEqualTo("application/json");
                softAssertions.assertThat(request.getPath()).isEqualTo("/tickets");
                return response;
            }
        };
    }
}
