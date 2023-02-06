package ru.yandex.market.deepmind.common.assertions;

import java.util.List;

import ru.yandex.market.deepmind.common.assertions.custom.IterableGeneralServiceOffersAssert;
import ru.yandex.market.deepmind.common.assertions.custom.IterableShopSkuAvailabilityAssertions;
import ru.yandex.market.deepmind.common.assertions.custom.ListGeneralOfferAssertionsService;
import ru.yandex.market.deepmind.common.assertions.custom.ServiceOfferAssertions;
import ru.yandex.market.deepmind.common.assertions.custom.ShopSkuAvailabilityAssertions;
import ru.yandex.market.deepmind.common.assertions.custom.SskuMskuStatusResultAssertions;
import ru.yandex.market.deepmind.common.availability.ssku.ShopSkuAvailability;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusResult;
import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileAssertions;

public class DeepmindAssertions {

    private DeepmindAssertions() {
        throw new IllegalStateException("Accessing private constructor of the utility class");
    }

    public static ExcelFileAssertions assertThat(ExcelFile actual) {
        return ExcelFileAssertions.assertThat(actual);
    }

    public static ServiceOfferAssertions assertThat(ServiceOfferReplica actual) {
        return ServiceOfferAssertions.assertThat(actual);
    }

    public static IterableGeneralServiceOffersAssert assertThatServiceOffers(
        Iterable<? extends ServiceOfferReplica> actual) {
        return IterableGeneralServiceOffersAssert.assertThat(actual);
    }

    public static ListGeneralOfferAssertionsService assertThatServiceOffers(
        List<? extends ServiceOfferReplica> actual) {
        return ListGeneralOfferAssertionsService.assertThat(actual);
    }

    public static ShopSkuAvailabilityAssertions assertThat(ShopSkuAvailability actual) {
        return new ShopSkuAvailabilityAssertions(actual);
    }

    public static IterableShopSkuAvailabilityAssertions assertAvailability(
        Iterable<? extends ShopSkuAvailability> actual
    ) {
        return new IterableShopSkuAvailabilityAssertions(actual);
    }

    public static SskuMskuStatusResultAssertions assertThat(SskuMskuStatusResult actual) {
        return new SskuMskuStatusResultAssertions(actual);
    }
}
