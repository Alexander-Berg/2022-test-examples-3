package ru.yandex.market.logistics.lom.controller.order;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.checker.QueueTaskChecker;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Отклонение заявки на преобразование заказа в заказ с доставкой по клику")
@DatabaseSetup("/controller/order/change_order_to_on_demand/setup.xml")
class DenyChangeOrderToOnDemandRequestControllerTest extends AbstractContextualTest {

    @Autowired
    private QueueTaskChecker queueTaskChecker;

    @Test
    @DisplayName("Успешное отклонение заявки на преобразование заказа в заказ с доставкой по клику")
    @DatabaseSetup("/controller/order/change_order_to_on_demand/deny/before/setup.xml")
    @ExpectedDatabase(
        value = "/controller/order/change_order_to_on_demand/deny/after/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDenySuccess() throws Exception {
        mockMvc.perform(
            post("/orders/changeRequests/1/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/request/deny_change_to_on_demand_order_request.json"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("type").value("DENY_CHANGE_ORDER_TO_ON_DEMAND"));

        queueTaskChecker.assertNoQueueTasksCreated();
    }
}
