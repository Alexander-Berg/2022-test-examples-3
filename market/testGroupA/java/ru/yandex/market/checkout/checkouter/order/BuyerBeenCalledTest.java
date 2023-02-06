package ru.yandex.market.checkout.checkouter.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * https://testpalm.yandex-team.ru/testcase/checkouter-68
 *
 * @author asafev
 */
public class BuyerBeenCalledTest extends AbstractWebTestBase {

    @Autowired
    private OrderPayHelper payHelper;
    private Order order;

    @BeforeEach
    public void setUp() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.YANDEX);

        order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);

        order = orderService.getOrder(order.getId());
        assertThat(order.isFulfilment(), is(true));
    }

    @Test
    @DisplayName("Чекаутер должен запрещать менять флаг beenCalled для заказов ВНЕ статуса PENDING " +
            "(т.к. только в этом статусе может идти комуникация с пользователем)")
    public void testDeclineBeenCalledInNonPendingStatus() throws Exception {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        mockMvc.perform(
                post("/orders/" + order.getId() + "/buyer/been-called").param("clientRole", "SYSTEM")
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("При изменении статуса в PROCESSING чекаутер НЕ должен автоматически выставлять флаг beenCalled = " +
            "true")
    public void testDoNotSetFlagBeenCalled() throws Exception {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        mockMvc.perform(
                get("/orders/" + order.getId() + "/buyer/been-called")
        ).andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
