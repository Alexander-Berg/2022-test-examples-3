package ru.yandex.market.delivery.transport_manager.controller.transportation_unit;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/repository/transportation/transportation_with_multiple_partner_info_and_register.xml")
class TransportationUnitControllerTest extends AbstractContextualTest {
    @DisplayName("Контроллер transportation_unit-ов: успешное получение одного transportation_unit-а")
    @Test
    void getTransportationSuccess() throws Exception {
        mockMvc.perform(get("/transportation-unit/2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/transportation_unit/response/success.json"));
    }

}
