package ru.yandex.market.logistics.nesu.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.Nonnull;

import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeatureListItem;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerFulfillmentLinksDTO;

public final class MbiFactory {

    private MbiFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static PartnerFulfillmentLinksDTO getPartnerFulfillmentLinksDTO(long amount) {
        return new PartnerFulfillmentLinksDTO(
            LongStream.range(100500L, 100500 + amount)
                .mapToObj(l ->
                    new PartnerFulfillmentLinkDTO(
                        1L,
                        l,
                        null,
                        null
                    ))
                .collect(Collectors.toList()));
    }

    @Nonnull
    public static PartnerFulfillmentLinksDTO getSinglePartnerFulfillmentLinksDTO(long partnerId, long serviceId) {
        return new PartnerFulfillmentLinksDTO(
            List.of(new PartnerFulfillmentLinkDTO(partnerId, serviceId, null, null))
        );
    }

    @Nonnull
    public static List<ShopFeatureListItem> getShopFeatureListItems(long shopId, FeatureType type, boolean feed) {
        return List.of(new ShopFeatureListItem(shopId, type, ParamCheckStatus.SUCCESS, feed));
    }

    @Nonnull
    public static PartnerFulfillmentLinksDTO getPartnerFulfillmentLinksDTO(long partnerId, Set<Long> serviceIds) {
        return new PartnerFulfillmentLinksDTO(
            serviceIds.stream()
                .map(serviceId -> new PartnerFulfillmentLinkDTO(partnerId, serviceId, null, null))
                .collect(Collectors.toList())
        );
    }

    @Nonnull
    public static PartnerFulfillmentLinksDTO getEmptyPartnerFulfillmentLinksDTO() {
        return new PartnerFulfillmentLinksDTO(List.of());
    }
}
