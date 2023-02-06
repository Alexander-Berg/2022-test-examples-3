package ru.yandex.market.core.delivery.tariff;

import java.util.Date;
import java.util.Set;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.core.delivery.tariff.model.RegionGroup;
import ru.yandex.market.core.delivery.tariff.model.RegionGroupRow;
import ru.yandex.market.core.delivery.tariff.model.TariffType;

/**
 * @author sergey-fed
 */
public final class DeliveryTariffTestHelper {

    private DeliveryTariffTestHelper() {
        throw new UnsupportedOperationException();
    }

    public static RegionGroup createTestRegionGroup(
            Long id,
            long datasourceId,
            String name,
            boolean isSelfRegion,
            TariffType tariffType,
            boolean useYml,
            Set<Long> includes,
            Set<Long> excludes,
            Long modifiedBy,
            Boolean hasDeliveryService
    ) {
        return createTestRegionGroup(id, datasourceId, name, isSelfRegion, tariffType, useYml, includes, excludes, modifiedBy, Currency.RUR, hasDeliveryService);
    }

    public static RegionGroup createTestRegionGroup(
            Long id,
            long datasourceId,
            String name,
            boolean isSelfRegion,
            TariffType tariffType,
            boolean useYml,
            Set<Long> includes,
            Set<Long> excludes,
            Long modifiedBy,
            Currency currency,
            Boolean hasDeliveryService
    ) {
        return new RegionGroup(id, datasourceId, name, isSelfRegion, tariffType, currency, null, modifiedBy, includes,
                excludes, null, useYml, hasDeliveryService, null);
    }

    public static RegionGroupRow createTestRegionGroupRow(
            Long id,
            long datasourceId,
            String name,
            boolean isSelfRegion,
            TariffType tariffType,
            Date modifiedAt,
            Long modifiedBy,
            Boolean hasDeliveryService
    ) {
        return new RegionGroupRow(id, datasourceId, name, isSelfRegion, tariffType, Currency.RUR, modifiedAt, modifiedBy, null, hasDeliveryService, null);
    }

}
