package ru.yandex.market.wms.autostart.controller;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.common.spring.enums.WmsErrorCode;
import ru.yandex.market.wms.shared.libs.utils.StringUtil;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class WavesControllerShortOrdersTest extends AutostartIntegrationTest {

    private MockWebServer server;

    @BeforeEach
    protected void initOrderManagementServer() throws IOException {
        server = new MockWebServer();
        server.start(8765);
    }


    @AfterEach
    protected void shutdownOrderManagementServer() throws IOException {
        server.shutdown();
    }

    @Test
    @DatabaseSetup("/controller/waves/db/short-order/withdrawal-immutable-state.xml")
    @ExpectedDatabase(
            value = "/controller/waves/db/short-order/withdrawal-immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shortWithdrawalHappyPath() throws Exception {
        server.enqueue(
                new MockResponse()
                        .setStatus("HTTP/1.1 " + HttpStatus.OK)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .setBody(StringUtil.resourceAsString("/controller/waves/response/" +
                                "short-orders-order-management-mock.json"))
        );

        mockMvc.perform(post("/waves/WAVE-001/shortOrder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/waves/request/short-withdrawal-orders.json")))
                .andExpect(status().isOk());

        RecordedRequest recordedRequest = server.takeRequest();
        assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertions.assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"details\":[" +
                "{\"orderKey\":\"ORDER-004\",\"items\":[{\"orderLineNumber\":\"1\",\"qty\":2}]}]}");
        assertions.assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/controller/waves/db/short-order/big-withdrawal-immutable-state.xml")
    @ExpectedDatabase(
            value = "/controller/waves/db/short-order/big-withdrawal-immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shortBigWithdrawalHappyPath() throws Exception {
        server.enqueue(
                new MockResponse()
                        .setStatus("HTTP/1.1 " + HttpStatus.OK)
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .setBody(StringUtil.resourceAsString("/controller/waves/response/" +
                                "short-orders-order-management-mock.json"))
        );

        ResultActions result = mockMvc.perform(post("/waves/WAVE-001/shortOrder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent(
                        "controller/waves/request/short-big-withdrawal-orders.json")));
        result.andExpect(status().isOk());

        RecordedRequest recordedRequest = server.takeRequest();
        assertions.assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        assertions.assertThat(recordedRequest.getBody().readUtf8()).isEqualTo("{\"details\":[{\"orderKey\":\"ORDER" +
                "-002\",\"items\":[{\"orderLineNumber\":\"1\",\"qty\":1},{\"orderLineNumber\":\"2\",\"qty\":2}]}]}");
        assertions.assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/controller/waves/db/short-order/withdrawal-immutable-state.xml")
    @ExpectedDatabase(
            value = "/controller/waves/db/short-order/withdrawal-immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shortWaveDoesNotExist() throws Exception {
        mockMvc.perform(post("/waves/WAVE-123/shortOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/waves/request/short-withdrawal-orders.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(StringContains.containsString("\"status\":\"NOT_FOUND\"")));
        assertions.assertThat(server.getRequestCount()).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/controller/waves/db/short-order/wave-has-wrong-type.xml")
    @ExpectedDatabase(
            value = "/controller/waves/db/short-order/wave-has-wrong-type.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shortWaveHasWrongType() throws Exception {
        mockMvc.perform(post("/waves/WAVE-001/shortOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/waves/request/short-withdrawal-orders.json")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        StringContains.containsString(WmsErrorCode.AUTOSTART_WAVE_HAS_WRONG_TYPE.name()))
                );
        assertions.assertThat(server.getRequestCount()).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/controller/waves/db/short-order/order-from-multiple-waves.xml")
    @ExpectedDatabase(
            value = "/controller/waves/db/short-order/order-from-multiple-waves.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void shortOrdersFromMultipleWaves() throws Exception {
        mockMvc.perform(post("/waves/WAVE-001/shortOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/waves/request/short-withdrawal-orders.json")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(
                        StringContains.containsString(WmsErrorCode.AUTOSTART_ORDERS_IN_MULTIPLE_WAVES.name()))
                );
        assertions.assertThat(server.getRequestCount()).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/controller/waves/db/short-order/withdrawal-wave-has-nothing-to-short.xml")
    @ExpectedDatabase(
            value = "/controller/waves/db/short-order/withdrawal-wave-has-nothing-to-short.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void waveHasNothingToShort() throws Exception {
        mockMvc.perform(post("/waves/WAVE-001/shortOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/waves/request/short-withdrawal-orders.json")))
                .andExpect(status().isOk());
        assertions.assertThat(server.getRequestCount()).isEqualTo(0);
    }
}
