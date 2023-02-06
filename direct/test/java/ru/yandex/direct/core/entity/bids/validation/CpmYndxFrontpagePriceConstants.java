package ru.yandex.direct.core.entity.bids.validation;

import java.math.BigDecimal;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.CpmYndxFrontpageRegionPriceRestrictions;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.currency.CurrencyCode;

import static ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

/**
 * Класс с константами для юнит-тестирования цен для главной
 */
public class CpmYndxFrontpagePriceConstants {
    private static final Map<FrontpageCampaignShowType, Map<Long, CpmYndxFrontpageRegionPriceRestrictions>>
            DEFAULT_RESTRICTIONS;

    static {
        Map<Long, CpmYndxFrontpageRegionPriceRestrictions> desktopPricesInfo = ImmutableMap.of(
                RUSSIA_REGION_ID, fromChfPrices(1., 2000.),
                UKRAINE_REGION_ID, fromChfPrices(.9, 2000.),
                MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, fromChfPrices(1.5, 2500.),
                MOSCOW_REGION_ID, fromChfPrices(2.5, 3000.),
                SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, fromChfPrices(1.2, 2200.));

        Map<Long, CpmYndxFrontpageRegionPriceRestrictions> mobilePricesInfo = ImmutableMap.of(
                RUSSIA_REGION_ID, fromChfPrices(.7, 1800.),
                UKRAINE_REGION_ID, fromChfPrices(.7, 1800.),
                MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, fromChfPrices(1.3, 2250.),
                MOSCOW_REGION_ID, fromChfPrices(.0001, 1800.),
                SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, fromChfPrices(1.1, 2000.));

        DEFAULT_RESTRICTIONS = ImmutableMap.of(FrontpageCampaignShowType.FRONTPAGE, desktopPricesInfo,
                FrontpageCampaignShowType.FRONTPAGE_MOBILE, mobilePricesInfo);
    }

    private static CpmYndxFrontpageRegionPriceRestrictions fromChfPrices(Double minPrice, Double maxPrice) {
        return new CpmYndxFrontpageRegionPriceRestrictions(
                ImmutableMap.of(CurrencyCode.CHF, BigDecimal.valueOf(minPrice)),
                ImmutableMap.of(CurrencyCode.CHF, BigDecimal.valueOf(maxPrice)));
    }

    public static Map<FrontpageCampaignShowType, Map<Long, CpmYndxFrontpageRegionPriceRestrictions>> getDefaultRestrictions() {
        return DEFAULT_RESTRICTIONS;
    }
}
