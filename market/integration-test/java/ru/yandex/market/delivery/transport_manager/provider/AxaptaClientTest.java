package ru.yandex.market.delivery.transport_manager.provider;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.dto.Stock;
import ru.yandex.market.delivery.transport_manager.dto.axapta.GetPhysicalAvailQtyRequest;
import ru.yandex.market.delivery.transport_manager.dto.axapta.GetPhysicalAvailQtyResponse;
import ru.yandex.market.delivery.transport_manager.dto.axapta.GetPhysicalAvailQtyResult;
import ru.yandex.market.delivery.transport_manager.dto.axapta.ResultStatus;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

class AxaptaClientTest extends AbstractContextualTest {
    public static final String REQUEST_URL = "https://axapta-request-url";
    public static final String POLLING_URL = "https://axapta-polling-url";
    public static final long TASK_ID = 5637145330L;

    protected MockRestServiceServer mockServer;

    @Autowired
    @Qualifier("axaptaApiRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private AxaptaClient axaptaClient;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void getPhysicalAvailQty() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(REQUEST_URL + "/InventoryServices/GetPhysicalAvailQty"))
            .andExpect(jsonRequestContent("axapta/InventoryServices/GetPhysicalAvailQty/request.json"))
            .andRespond(
                withSuccess(
                    extractFileContent("axapta/InventoryServices/GetPhysicalAvailQty/response.json"),
                    MediaType.APPLICATION_JSON
                ));

        final GetPhysicalAvailQtyResponse expectedResp = new GetPhysicalAvailQtyResponse(true, null, TASK_ID);

        final List<GetPhysicalAvailQtyRequest> request = List.of(
            new GetPhysicalAvailQtyRequest(
                "000044.JBLCHARGE3BLKEU",
                10264169,
                "000044",
                new Stock(1L, CountType.FIT),
                50
            ),
            new GetPhysicalAvailQtyRequest(
                "000044.JBLCHARGE3BLKRU",
                10264169,
                "000044",
                new Stock(1L, CountType.DEFECT),
                48
            )
        );

        final GetPhysicalAvailQtyResponse actualResp = axaptaClient.getPhysicalAvailQty(request);

        softly.assertThat(actualResp).isEqualTo(expectedResp);
    }

    @Test
    void getResult() {
        mockServer.expect(method(HttpMethod.GET))
            .andExpect(requestTo(POLLING_URL + "/Task/Result/5637145330"))
            .andRespond(
                withSuccess(
                    extractFileContent("axapta/Task/Result/5637145330.json"),
                    MediaType.APPLICATION_JSON
                ));

        final Optional<GetPhysicalAvailQtyResult.Result> result = axaptaClient.getResult(TASK_ID);

        final GetPhysicalAvailQtyResult.Result expected = new GetPhysicalAvailQtyResult.Result()
            .setResultStatus(new ResultStatus(true, ""))
            .setResults(List.of(
                new GetPhysicalAvailQtyResult.ResultUnit()
                    .setResultStatus(new ResultStatus(true, ""))
                    .setResultUnitKey(new GetPhysicalAvailQtyResult.ResultUnitKey(
                        "000044.JBLCHARGE3BLKEU",
                        10264169L,
                        "000044",
                        new Stock(1L, CountType.FIT)
                    ))
                    .setRequestedQty(50)
                    .setAvailPhysicalQty(82),
                new GetPhysicalAvailQtyResult.ResultUnit()
                    .setResultStatus(new ResultStatus(true, ""))
                    .setResultUnitKey(new GetPhysicalAvailQtyResult.ResultUnitKey(
                        "000044.JBLCHARGE3BLKRU",
                        10264169L,
                        "000044",
                        new Stock(1L, CountType.DEFECT)
                    ))
                    .setRequestedQty(48)
                    .setAvailPhysicalQty(84)
            ));
        softly.assertThat(result).contains(expected);
    }
}
