package ru.yandex.market.delivery.transport_manager.controller.logistic_point;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;

@DatabaseSetup("/repository/logistic_point/simple_points.xml")
public class LogisticPointControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Контроллер лог-точек: успешное получение инфы по логточке")
    void getLogisticPointsSuccess() throws Exception {
        mockMvc.perform(get("/logisticPoint?ids=101"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/logistic_point/response/success.json"));
    }
}
