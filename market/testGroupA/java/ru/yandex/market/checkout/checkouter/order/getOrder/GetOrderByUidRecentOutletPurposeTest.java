package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletPurpose;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_POST_TERM_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class GetOrderByUidRecentOutletPurposeTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void postDeliveryHasOutletPurpose() throws Exception {
        var postOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.POST)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .build();

        Long uid = postOrder.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/by-uid/{uid}/recent", uid)
                        .param(CheckouterClientParams.RGB, postOrder.getRgb().name())
                        .param(CheckouterClientParams.STATUS, postOrder.getStatus().name())
                        .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*]", hasSize(1)))
                .andExpect(jsonPath("$.[0].delivery.purpose").value(OutletPurpose.POST.toString()));
    }

    @Test
    public void pickupDeliveryHasOutletPurpose() throws Exception {
        var postOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        Long uid = postOrder.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/by-uid/{uid}/recent", uid)
                        .param(CheckouterClientParams.RGB, postOrder.getRgb().name())
                        .param(CheckouterClientParams.STATUS, postOrder.getStatus().name())
                        .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*]", hasSize(1)))
                .andExpect(jsonPath("$.[0].delivery.purpose").value(OutletPurpose.PICKUP.toString()));
    }

    @Test
    public void postTermDeliveryHasOutletPurpose() throws Exception {
        var postOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(MOCK_POST_TERM_DELIVERY_SERVICE_ID)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPostTerm(MOCK_POST_TERM_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        Long uid = postOrder.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/by-uid/{uid}/recent", uid)
                        .param(CheckouterClientParams.RGB, postOrder.getRgb().name())
                        .param(CheckouterClientParams.STATUS, postOrder.getStatus().name())
                        .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*]", hasSize(1)))
                .andExpect(jsonPath("$.[0].delivery.purpose").value(OutletPurpose.POST_TERM.toString()));
    }
}
