package ru.yandex.market.delivery.transport_manager.controller.lgw;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

class TaskCallbackShipmentControllerTest extends AbstractContextualTest {

    @BeforeEach
    void before() {
        clock.setFixed(Instant.parse("2020-07-09T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/setup/register_1.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createIntakeError() throws Exception {
        mockMvc.perform(put("/shipment/createIntakeError")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/shipment/create_shipment_error.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/setup/register_1.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/error.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createSelfExportError() throws Exception {
        mockMvc.perform(put("/shipment/createSelfExportError")
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent("controller/shipment/create_shipment_error.json"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
