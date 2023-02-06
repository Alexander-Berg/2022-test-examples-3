package ru.yandex.market.sc.core.util;

import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;

public class CompareFactory {

    public static RegistryOrderValue registryOrder(RegistryOrder order) {
        return new RegistryOrderValue(order.getRegistryId(), order.getExternalId(), order.getPlaceId(), order.getPalletId());
    }

    public static RegistryOrderValue registryOrder(long registryId, String orderExtId, String placeId, String palletId) {
        return new RegistryOrderValue(registryId, orderExtId, placeId, palletId);
    }

    public static record RegistryOrderValue(
            long registryId,
            String orderExtId,
            String placeId,
            String palletId
    ) {
    }

}
