package ru.yandex.market.fulfillment.stockstorage.client;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FFIntervalDto;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageFFIntervalRestClient.SYNC_JOB_INTERVAL_PATH;
import static ru.yandex.market.fulfillment.stockstorage.client.TestContextConfiguration.SERVICE_TICKET;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;

@RunWith(SpringRunner.class)
@Import({StockStorageClientConfiguration.class, TestContextConfiguration.class})
@TestPropertySource(properties = {"fulfillment.stockstorage.api.host=http://rkn.gov.ru",
        "fulfillment.stockstorage.tvm.client.id=" + SERVICE_TICKET})
public class StockStorageFFIntervalClientTest {

    public static final String FIXTURE_FF_INTERVAL_DIR = "fixture/ff-interval/";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Value("${fulfillment.stockstorage.api.host:}")
    private String host;

    @Autowired
    private StockStorageClientConfiguration configuration;

    private MockRestServiceServer mockServer;

    @Autowired
    private StockStorageFFIntervalClient client;

    @Before
    public void init() {
        mockServer = MockRestServiceServer.createServer(configuration.restTemplate());
    }

    @Test
    public void getSyncJobIntervals() {
        mockServer.expect(requestTo(buildPath(SYNC_JOB_INTERVAL_PATH)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent(FIXTURE_FF_INTERVAL_DIR + "find_all.json")));
        List<FFIntervalDto> response = client.getSyncJobIntervals();
        mockServer.verify();
        Assert.assertEquals(getAllIntervals(), response);
    }

    @Test
    public void getSyncJobIntervalById() {
        mockServer.expect(requestTo(buildPath(SYNC_JOB_INTERVAL_PATH, 1)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent(FIXTURE_FF_INTERVAL_DIR + "find_one.json")));
        FFIntervalDto response = client.getSyncJobInterval(1);
        mockServer.verify();
        Assert.assertEquals(getFirstInterval(), response);
    }

    @Test
    public void getSyncJobIntervalByJobNameAndWarehouseId() {
        mockServer.expect(requestTo(buildPath(SYNC_JOB_INTERVAL_PATH, "job1", 1)))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK)
                        .contentType(APPLICATION_JSON)
                        .body(extractFileContent(FIXTURE_FF_INTERVAL_DIR + "find_one.json")));
        FFIntervalDto response = client.getSyncJobInterval("job1", 1);
        mockServer.verify();
        Assert.assertEquals(getFirstInterval(), response);
    }

    @Test
    public void deleteSyncJobInterval() {
        mockServer.expect(requestTo(buildPath(SYNC_JOB_INTERVAL_PATH, 1)))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andRespond(withStatus(OK));
        client.deleteSyncJobInterval(1);
        mockServer.verify();
    }

    @Test
    public void updateSyncJobInterval() {
        mockServer.expect(requestTo(buildPath(SYNC_JOB_INTERVAL_PATH, 1)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_FF_INTERVAL_DIR + "update_insert.json")))
                .andRespond(withStatus(OK));
        client.updateSyncJobInterval(1, getIntervalToUpdateOrInsert());
        mockServer.verify();
    }

    @Test
    public void createSyncJobInterval() {
        mockServer.expect(requestTo(buildPath(SYNC_JOB_INTERVAL_PATH)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(SERVICE_TICKET_HEADER, SERVICE_TICKET))
                .andExpect(checkBody(extractFileContent(FIXTURE_FF_INTERVAL_DIR + "update_insert.json")))
                .andRespond(withStatus(OK));
        client.createSyncJobInterval(getIntervalToUpdateOrInsert());
        mockServer.verify();
    }

    private RequestMatcher checkBody(String expectedJson) {
        return content().string(new JsonMatcher(expectedJson));
    }

    private String buildPath(Object... parts) {
        StringBuilder path = new StringBuilder(host);
        for (Object part : parts) {
            if (!part.toString().startsWith("/")) {
                path.append("/");
            }
            path.append(part);
        }
        return path.toString();
    }

    private FFIntervalDto getFirstInterval() {
        return new FFIntervalDto(1L, 1, "job1", 15, true, 250);
    }

    private FFIntervalDto getSecondInterval() {
        return new FFIntervalDto(2L, 1, "job2", 15, false, 300);
    }

    private FFIntervalDto getIntervalToUpdateOrInsert() {
        return new FFIntervalDto(null, 1, "job3", 16, true, 300);
    }

    private List<FFIntervalDto> getAllIntervals() {
        return Arrays.asList(getFirstInterval(), getSecondInterval());
    }
}
