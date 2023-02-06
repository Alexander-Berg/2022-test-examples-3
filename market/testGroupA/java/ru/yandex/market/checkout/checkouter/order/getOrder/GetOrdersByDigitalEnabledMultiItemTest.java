package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetOrdersByDigitalEnabledMultiItemTest extends AbstractWebTestBase {

    private Order usualOrder;

    @BeforeEach
    public void init() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.addAnotherItem();

        usualOrder = orderCreateHelper.createOrder(parameters);
        MatcherAssert.assertThat(usualOrder.getBuyer().getUid(), CoreMatchers.is(BuyerProvider.UID));
        MatcherAssert.assertThat(usualOrder.getItems(), hasSize(2));
    }

    @DisplayName("Не должно быть дублей")
    @Test
    void shouldReturnNoDigitalOrderWhenDigitalEnabledIsFalse() throws Exception {
        mockMvc.perform(

                MockMvcRequestBuilders.get("/orders/by-uid/{uid}/recent", BuyerProvider.UID)
                        .param(CheckouterClientParams.STATUS, OrderStatus.UNPAID.name())
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*]", Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id", Matchers.equalTo(usualOrder.getId().intValue())));
    }
}
