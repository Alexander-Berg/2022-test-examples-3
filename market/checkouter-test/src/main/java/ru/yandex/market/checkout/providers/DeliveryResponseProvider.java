package ru.yandex.market.checkout.providers;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

@Deprecated
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class DeliveryResponseProvider {

    @Deprecated
    @Nonnull
    public static DeliveryResponse buildDeliveryResponse() {
        return DeliveryProvider.buildShopDeliveryResponse(DeliveryResponse::new);
    }

    @Deprecated
    @Nonnull
    public static DeliveryResponse buildPickupDeliveryResponse() {
        return DeliveryProvider.buildPickupDeliveryResponseWithOutletCode(DeliveryResponse::new);
    }

    @Deprecated
    @Nonnull
    public static DeliveryResponse buildPostDeliveryResponse() {
        return DeliveryProvider.buildShopPostDeliveryResponse(DeliveryResponse::new);
    }

    @Deprecated
    @Nonnull
    public static DeliveryResponse buildPickupDeliveryResponseWithOutletCode() {
        return DeliveryProvider.buildPickupDeliveryResponseWithOutletCode(DeliveryResponse::new);
    }

    @Deprecated
    @Nonnull
    public static DeliveryResponse buildDigitalDeliveryResponse() {
        return DeliveryProvider.buildDigitalDeliveryResponse(DeliveryResponse::new);
    }

    @Deprecated
    public static DeliveryResponse buildPostpaidDeliveryResponse() {
        return DeliveryProvider.buildPostpaidDeliveryResponse(DeliveryResponse::new);
    }

    @Deprecated
    public static DeliveryResponse buildDeliveryResponseWithIntervals() {
        return DeliveryProvider.buildDeliveryResponseWithIntervals(DeliveryResponse::new);
    }

    @Deprecated
    public static DeliveryResponse createFFDeliveryDelivery(boolean free) {
        return DeliveryProvider.createYandexDeliveryResponse(free, DeliveryResponse::new);
    }
}
