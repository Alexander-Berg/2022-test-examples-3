package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletPurpose;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;

public class CreateOrderOutletYandexMapPermalinkTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Test
    public void testYandexMapPermalinkForPickupOrder() throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        Assertions.assertEquals(DeliveryType.PICKUP, order.getDelivery().getType());
        Assertions.assertNotNull(order.getDelivery().getOutlet());
        Assertions.assertEquals(OutletPurpose.PICKUP, order.getDelivery().getOutletPurpose());
        order = client.getOrder(
                new RequestClientInfo(
                        ClientRole.USER,
                        order.getBuyer().getUid()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.DELIVERY, OptionalOrderPart.DELIVERY_PARCELS))
                        .build());
        Assertions.assertEquals("yandexMapPermalink", order.getDelivery().getOutlet().getYandexMapPermalink());
    }

    @Test
    public void testYandexMapPermalinkForPostDeliveryOrder() throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.POST)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPost(1, RUSPOST_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        Assertions.assertEquals(DeliveryType.POST, order.getDelivery().getType());
        Assertions.assertNotNull(order.getDelivery().getPostOutlet());
        Assertions.assertEquals(OutletPurpose.POST, order.getDelivery().getOutletPurpose());
        order = client.getOrder(
                new RequestClientInfo(
                        ClientRole.USER,
                        order.getBuyer().getUid()),
                OrderRequest.builder(order.getId())
                        .withPartials(Set.of(OptionalOrderPart.DELIVERY, OptionalOrderPart.DELIVERY_PARCELS))
                        .build());
        Assertions.assertEquals("yandexMapPermalink", order.getDelivery().getPostOutlet().getYandexMapPermalink());
    }
}
