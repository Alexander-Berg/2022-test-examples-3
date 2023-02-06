package ru.yandex.market.logistics.lom.utils.lgw;

import java.util.Set;

import javax.annotation.Nonnull;

import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;

public final class CreateLgwCommonEntitiesUtils {

    private CreateLgwCommonEntitiesUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Partner createPartner() {
        return createPartner(20L);
    }

    @Nonnull
    public static CreateOrderRestrictedData createOrderRestrictedDataWithPromise(String promise) {
        return CreateOrderRestrictedData.builder().setPromise(promise).build();
    }

    @Nonnull
    public static Partner createPartner(long id) {
        return new Partner(id);
    }

    @Nonnull
    public static PartnerRelationFilter createPartnerRelationFilter(long fromPartnerId, long toPartnerId) {
        return PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(fromPartnerId))
            .toPartnersIds(Set.of(toPartnerId))
            .build();
    }
}
