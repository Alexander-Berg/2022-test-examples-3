package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class GetOrderByUidRecentPostOutletTest extends AbstractWebTestBase {

    private Order postOrder;

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @BeforeEach
    void setUp() {
        postOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.POST)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .build();

        MatcherAssert.assertThat(postOrder, notNullValue());
    }

    @DisplayName("Проверяем, что заказ с DeliveryType=POST возвращается с заполненным postOutlet")
    @Test
    public void postDeliveryHasPostOutlet() throws Exception {
        Long uid = postOrder.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/by-uid/{uid}/recent", uid)
                        .param(CheckouterClientParams.RGB, postOrder.getRgb().name())
                        .param(CheckouterClientParams.STATUS, postOrder.getStatus().name())
                        .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*]", hasSize(1)))
                .andExpect(jsonPath("$.[0].delivery.postOutletId").exists())
                .andExpect(jsonPath("$.[0].delivery.postOutlet").isMap());
    }
}
