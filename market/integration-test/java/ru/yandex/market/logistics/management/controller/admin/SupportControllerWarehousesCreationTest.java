package ru.yandex.market.logistics.management.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.management.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class SupportControllerWarehousesCreationTest extends AbstractContextualTest {
    @Test
    @DatabaseSetup("/data/controller/health/warehouses_creation_failures.xml")
    @ExpectedDatabase(
        value = "/data/controller/health/after_warehouses_creation_failures.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testRestartWarehouseInDeliveryCreation() throws Exception {
        mockMvc.perform(
            post("/support/lms/partner/3/restartWarehouseCreation/2")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
