package ru.yandex.market.checkout.helpers;

import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Различные вспомогательные действия, типа звонка покупателю.
 *
 * @author sergeykoles
 * Created on: 08.02.18
 */
@WebTestHelper
public class OrderMiscActionsHelper extends MockMvcAware {

    public OrderMiscActionsHelper(WebApplicationContext webApplicationContext,
                                  TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    /**
     * Поставить заказу отметку, о том, что покупателю позвонили.
     *
     * @param order целевой заказ
     */
    public void callBuyer(Order order) {
        try {
            mockMvc.perform(
                    post("/orders/" + order.getId() + "/buyer/been-called")
                            .param("clientRole", "SYSTEM")
            ).andExpect(status().isOk());
        } catch (Exception e) {
            // для тестов норм
            throw new RuntimeException(e);
        }
    }
}
