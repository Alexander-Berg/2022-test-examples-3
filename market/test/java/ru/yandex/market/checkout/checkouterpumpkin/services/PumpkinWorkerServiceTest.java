package ru.yandex.market.checkout.checkouterpumpkin.services;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.yandex.ydb.table.values.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouterpumpkin.BasePumpkinTest;
import ru.yandex.market.ydb.integration.model.Field;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PumpkinWorkerServiceTest extends BasePumpkinTest {

    @Autowired
    private PumpkinWorkerService pumpkinWorkerService;

    @Autowired
    private WireMockServer checkouterMock;

    @BeforeEach
    public void setup() {
        testableClock.setFixed(Instant.parse("2021-10-08T12:00:00Z"), ZoneId.of("Europe/Moscow"));
    }

    @AfterEach
    public void resetWireMock() {
        checkouterMock.resetAll();
    }

    @Disabled("Broken in trunk")
    @ParameterizedTest
    @ValueSource(strings = {
            "1_initial"
    })
    public void processRequestSuccess(String name) throws IOException {
        addCheckoutRequest(UUID.fromString("ca6bdd2c-41b5-4d00-9e42-0c0935522d79"),
                Map.of(),
                Map.of("uid", List.of("1824192071627455019")),
                loadResourceAsString(String.format("worker/%s_stored_req.json", name)));

        checkouterMock.stubFor(post(urlPathEqualTo("/cart"))
                .withQueryParam("uid", equalTo("1824192071627455019"))
                .willReturn(okJson(loadResourceAsString(String.format("worker/%s_cart_resp.json", name)))));

        checkouterMock.stubFor(post(urlPathEqualTo("/checkout"))
                .withQueryParam("uid", equalTo("1824192071627455019"))
                .willReturn(okJson(loadResourceAsString(String.format("worker/%s_checkout_resp.json", name)))));

        testableClock.setFixed(Instant.parse("2021-10-08T13:00:00Z"), ZoneId.of("Europe/Moscow"));

        pumpkinWorkerService.processRequest();

        checkouterMock.verify(1, postRequestedFor(urlPathEqualTo("/cart"))
                .withRequestBody(equalToJson(loadResourceAsString(String.format("worker/%s_cart_req.json", name)),
                        true, true)));

        checkouterMock.verify(1, postRequestedFor(urlPathEqualTo("/checkout"))
                .withRequestBody(equalToJson(loadResourceAsString(String.format("worker/%s_checkout_req.json", name)),
                        true, true)));

        List<Map<Field<?, ?>, Value<?>>> queueData = readTableData(queueTable);
        assertTrue(queueData.isEmpty());

        List<Map<Field<?, ?>, Value<?>>> requestData = readTableData(requestTable);
        assertEquals(1, requestData.size());
        Map<Field<?, ?>, Value<?>> requestRow = requestData.get(0);
        assertEquals("ca6bdd2c-41b5-4d00-9e42-0c0935522d79", requestRow.get(requestTable.getId()).asData().getUtf8());
        assertEquals("success", requestRow.get(requestTable.getStatus()).asData().getUtf8());
        assertEquals(testableClock.instant(), requestRow.get(requestTable.getProcessedAt()).asData().getTimestamp());
        assertNotNull(requestRow.get(requestTable.getTraceId()));
    }

    @Test
    @Disabled("Broken in trunk")
    public void processRequestWhenCheckouterIsUnavailable() throws IOException {
        addCheckoutRequest(UUID.fromString("ca6bdd2c-41b5-4d00-9e42-0c0935522d79"),
                Map.of(),
                Map.of("uid", List.of("1824192071627455019")),
                loadResourceAsString("worker/1_initial_stored_req.json"));

        checkouterMock.stubFor(post(urlPathEqualTo("/cart"))
                .withQueryParam("uid", equalTo("1824192071627455019"))
                .willReturn(WireMock.serviceUnavailable()));

        pumpkinWorkerService.processRequest();

        checkouterMock.verify(1, postRequestedFor(urlPathEqualTo("/cart")));

        List<Map<Field<?, ?>, Value<?>>> queueData = readTableData(queueTable);
        assertEquals(1, queueData.size());
        Map<Field<?, ?>, Value<?>> queueRow = queueData.get(0);
        assertEquals("ca6bdd2c-41b5-4d00-9e42-0c0935522d79",
                queueRow.get(queueTable.getRequestId()).asData().getUtf8());
        assertNull(queueRow.get(queueTable.getProcessingBy()));
        assertNull(queueRow.get(queueTable.getProcessingStartedAt()));

        List<Map<Field<?, ?>, Value<?>>> requestData = readTableData(requestTable);
        assertEquals(1, requestData.size());
        Map<Field<?, ?>, Value<?>> requestRow = requestData.get(0);
        assertEquals("ca6bdd2c-41b5-4d00-9e42-0c0935522d79", requestRow.get(requestTable.getId()).asData().getUtf8());
        assertEquals("in_queue", requestRow.get(requestTable.getStatus()).asData().getUtf8());
    }

    @Test
    @Disabled("Broken in trunk")
    public void processRequestError() throws IOException {
        addCheckoutRequest(UUID.fromString("ca6bdd2c-41b5-4d00-9e42-0c0935522d79"),
                Map.of(),
                Map.of("uid", List.of("1824192071627455019")),
                loadResourceAsString("worker/invalid_stored_req.json"));

        checkouterMock.stubFor(post(urlPathEqualTo("/cart"))
                .withQueryParam("uid", equalTo("1824192071627455019"))
                .willReturn(okJson(loadResourceAsString("worker/invalid_cart_resp.json"))));

        testableClock.setFixed(Instant.parse("2021-10-08T13:00:00Z"), ZoneId.of("Europe/Moscow"));

        pumpkinWorkerService.processRequest();

        checkouterMock.verify(1, postRequestedFor(urlPathEqualTo("/cart"))
                .withRequestBody(equalToJson(loadResourceAsString("worker/invalid_cart_req.json"),
                        true, true)));

        List<Map<Field<?, ?>, Value<?>>> queueData = readTableData(queueTable);
        assertTrue(queueData.isEmpty());

        List<Map<Field<?, ?>, Value<?>>> requestData = readTableData(requestTable);
        assertEquals(1, requestData.size());
        Map<Field<?, ?>, Value<?>> requestRow = requestData.get(0);
        assertEquals("ca6bdd2c-41b5-4d00-9e42-0c0935522d79", requestRow.get(requestTable.getId()).asData().getUtf8());
        assertEquals("error", requestRow.get(requestTable.getStatus()).asData().getUtf8());
        assertEquals(testableClock.instant(), requestRow.get(requestTable.getProcessedAt()).asData().getTimestamp());
        assertNotNull(requestRow.get(requestTable.getTraceId()));
    }
}
