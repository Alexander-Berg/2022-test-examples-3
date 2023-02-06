package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller.monitoring;

import java.io.IOException;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.servicebus.api.internal.wms.server.controller.VendorApiBaseTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SchaeferMonitoringControllerTest extends VendorApiBaseTest {
    private static final int MOCK_WEB_SERVER_PORT_PING = 9210;

    @BeforeAll
    static void setUpMockBackEnd() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start(MOCK_WEB_SERVER_PORT_PING);
    }

    @Test
    public void shouldSuccessfullyPingSchaefer() throws Exception {

        mockVendorBackEndResponse(HttpStatus.OK,
                "0;OK",
                WITHOUT_RETRY_COUNT);

        mockMvc.perform(get("/schaefer/monitoring/ping")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("0;OK"))
                .andReturn();
    }

    @Test
    public void shouldWarnIfSchaeferPartiallyDown() throws Exception {

        mockVendorBackEndResponse(HttpStatus.OK,
                "1;WARN",
                WITHOUT_RETRY_COUNT);

        mockMvc.perform(get("/schaefer/monitoring/ping")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("2;Конвейер Schaefer частично не работает"))
                .andReturn();
    }

    @Test
    public void shouldWarnIfSchaeferFullyDown() throws Exception {

        mockVendorBackEndResponse(HttpStatus.OK,
                "2;CRIT",
                WITHOUT_RETRY_COUNT);

        mockMvc.perform(get("/schaefer/monitoring/ping")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("2;Конвейер Schaefer полностью не работает"))
                .andReturn();
    }
}
