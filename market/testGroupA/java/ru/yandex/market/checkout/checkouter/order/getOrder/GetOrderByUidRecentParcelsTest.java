package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.builders.ParcelBuilder;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class GetOrderByUidRecentParcelsTest extends AbstractWebTestBase {

    private Order postOrder;

    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @BeforeEach
    void setUp() throws Exception {
        postOrder = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.POST)
                .withColor(Color.BLUE)
                .withDeliveryServiceId(RUSPOST_DELIVERY_SERVICE_ID)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .build();

        postOrder = orderStatusHelper.proceedOrderToStatus(postOrder, OrderStatus.PROCESSING);

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdateWithParcels(
                ParcelBuilder.instance()
                        .withTracks(List.of(
                                TrackProvider.createTrack()
                        ))
                        .build()
        );

        postOrder = orderDeliveryHelper.updateOrderDelivery(postOrder.getId(), ClientInfo.SYSTEM, deliveryUpdate);

        MatcherAssert.assertThat(postOrder.getDelivery().getParcels().get(0).getTracks(), Matchers.allOf(
                CoreMatchers.notNullValue(),
                hasSize(1)
        ));

    }

    @DisplayName("Проверяем, что грузим посылки при передаче соответствующего параметра")
    @Test
    public void shouldRetrieveParcelsIfOptionalPartIsSpecified() throws Exception {
        Long uid = postOrder.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/by-uid/{uid}/recent", uid)
                        .param(CheckouterClientParams.RGB, postOrder.getRgb().name())
                        .param(CheckouterClientParams.STATUS, postOrder.getStatus().name())
                        .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY_PARCELS.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[*]", hasSize(1)))
                .andExpect(jsonPath("$.[0].delivery.parcels[*].tracks[*]", hasSize(1)));
    }
}
