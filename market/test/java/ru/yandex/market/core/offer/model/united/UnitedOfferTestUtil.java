package ru.yandex.market.core.offer.model.united;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

import javax.annotation.Nonnull;

import ru.yandex.market.core.datacamp.DataCampUtil;
import ru.yandex.market.core.feed.offer.united.OfferBid;
import ru.yandex.market.core.feed.offer.united.OfferCategoryInfo;
import ru.yandex.market.core.feed.offer.united.OfferContentInfo;
import ru.yandex.market.core.feed.offer.united.OfferDeliveryInfo;
import ru.yandex.market.core.feed.offer.united.OfferDimensions;
import ru.yandex.market.core.feed.offer.united.OfferIdentifier;
import ru.yandex.market.core.feed.offer.united.OfferLifeCycleInfo;
import ru.yandex.market.core.feed.offer.united.OfferManufactureInfo;
import ru.yandex.market.core.feed.offer.united.OfferModelDescription;
import ru.yandex.market.core.feed.offer.united.OfferPartnerInfo;
import ru.yandex.market.core.feed.offer.united.OfferPartnerSpecification;
import ru.yandex.market.core.feed.offer.united.OfferPeriod;
import ru.yandex.market.core.feed.offer.united.OfferPeriodInfo;
import ru.yandex.market.core.feed.offer.united.OfferPrice;
import ru.yandex.market.core.feed.offer.united.OfferProductParam;
import ru.yandex.market.core.feed.offer.united.OfferQuantityInfo;
import ru.yandex.market.core.feed.offer.united.OfferReceivingCondition;
import ru.yandex.market.core.feed.offer.united.OfferReceivingInfo;
import ru.yandex.market.core.feed.offer.united.OfferSpecification;
import ru.yandex.market.core.feed.offer.united.OfferSuggestedInfo;
import ru.yandex.market.core.feed.offer.united.OfferSupplyInfo;
import ru.yandex.market.core.feed.offer.united.OfferSupplyStatus;
import ru.yandex.market.core.feed.offer.united.OfferSupplyTimeInfo;
import ru.yandex.market.core.feed.offer.united.OfferSupplyUnitInfo;
import ru.yandex.market.core.feed.offer.united.OfferSystemInfo;
import ru.yandex.market.core.feed.offer.united.OfferTradeInfo;
import ru.yandex.market.core.feed.offer.united.OfferVendorInfo;
import ru.yandex.market.core.feed.offer.united.OfferWeightDimensions;
import ru.yandex.market.core.feed.offer.united.UnitedOffer;
import ru.yandex.market.core.price.DynamicPricingStrategy;
import ru.yandex.market.core.price.DynamicPricingThresholdType;
import ru.yandex.market.core.price.SkuDynamicPricingStrategy;
import ru.yandex.market.core.tax.model.VatRate;

/**
 * Date: 21.01.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
public class UnitedOfferTestUtil {

    private UnitedOfferTestUtil() {
    }

    @Nonnull
    public static UnitedOffer buildEmptyUnitedOffer(Integer partnerId, String offerId) {
        return UnitedOffer.builder()
                .withContentInfo(OfferContentInfo.builder()
                        .withPartnerSpecification(OfferPartnerSpecification.builder()
                                .withModelDescription(OfferModelDescription.builder()
                                        .withVendorInfo(OfferVendorInfo.builder()
                                                .build())
                                        .build())
                                .build())
                        .withSpecification(OfferSpecification.builder()
                                .withManufactureInfo(OfferManufactureInfo.builder()
                                        .build())
                                .withWeightDimensions(OfferWeightDimensions.builder()
                                        .withDimensions(OfferDimensions.builder()
                                                .build())
                                        .build())
                                .build())
                        .withLifeCycleInfo(OfferLifeCycleInfo.builder()
                                .withWarranty(OfferPeriodInfo.builder()
                                        .build())
                                .withShelfLife(OfferPeriodInfo.builder()
                                        .build())
                                .withServiceLife(OfferPeriodInfo.builder()
                                        .build())
                                .build())
                        .build())
                .withSystemInfo(OfferSystemInfo.builder()
                        .withIdentifier(OfferIdentifier.builder()
                                .withShopId(partnerId)
                                .withShopSku(offerId)
                                .build())
                        .build())
                .withTradeInfo(OfferTradeInfo.builder()
                        .withSupplyInfo(OfferSupplyInfo.builder()
                                .withSupplyUnitInfo(OfferSupplyUnitInfo.builder()
                                        .build())
                                .withSupplyTimeInfo(OfferSupplyTimeInfo.builder()
                                        .build())
                                .build())
                        .withQuantityInfo(OfferQuantityInfo.builder()
                                .build())
                        .withPrice(OfferPrice.builder()
                                .build())
                        .withDeliveryInfo(OfferDeliveryInfo.builder()
                                .build())
                        .build())
                .withPartnerInfo(OfferPartnerInfo.builder()
                        .build())
                .withSuggestedInfo(OfferSuggestedInfo.builder()
                        .build())
                .build();
    }

    @Nonnull
    public static UnitedOffer buildUnitedOffer(Integer partnerId, String offerId) {
        return buildUnitedOffer(partnerId, offerId, true);
    }

    @Nonnull
    public static UnitedOffer buildUnitedOffer(Integer partnerId, String offerId, boolean withValidPicUrl) {
        return UnitedOffer.builder()
                .withContentInfo(OfferContentInfo.builder()
                        .withPartnerSpecification(OfferPartnerSpecification.builder()
                                .withModelDescription(OfferModelDescription.builder()
                                        .withVendorInfo(OfferVendorInfo.builder()
                                                .withVendorCode("CODE 228")
                                                .withVendor("PKCELL")
                                                .build())
                                        .withDescription("Offer description")
                                        .withName("Батарейка AG3 щелочная PKCELL AG3-10B 10шт")
                                        .withBarCodes(List.of("4985058793639"))
                                        .withProductParam(OfferProductParam.builder()
                                                .withName("Диагональ")
                                                .withValue("27")
                                                .withUnit("дюймов")
                                                .build())
                                        .build())
                                .withSalesNotes("Предоплата 42%")
                                .withManufacturerWarranty(true)
                                .withCertificates(List.of("584723957169"))
                                .build())
                        .withSpecification(OfferSpecification.builder()
                                .withManufactureInfo(OfferManufactureInfo.builder()
                                        .withCountryOfOrigins(List.of("Китай", "Вьетнам"))
                                        .withManufacturer("PKCELL")
                                        .build())
                                .withCustomsCommodityCodes(List.of("8506101100", "3216101100"))
                                .withWeightDimensions(OfferWeightDimensions.builder()
                                        .withWeight(50000L)
                                        .withDimensions(OfferDimensions.builder()
                                                .withWidth(10000L)
                                                .withHeight(10000L)
                                                .withLength(50000L)
                                                .build())
                                        .build())
                                .build())
                        .withCategoryInfo(new OfferCategoryInfo("Батарейки и аккумуляторы"))
                        .withLifeCycleInfo(OfferLifeCycleInfo.builder()
                                .withWarranty(OfferPeriodInfo.builder()
                                        .withPeriod(OfferPeriod.builder()
                                                .withHours(4)
                                                .withDays(3)
                                                .withMonths(2)
                                                .build())
                                        .withComment("Guarantee period comment from partner spec")
                                        .build())
                                .withShelfLife(OfferPeriodInfo.builder()
                                        .withPeriod(OfferPeriod.builder()
                                                .withHours(10)
                                                .withDays(6)
                                                .build())
                                        .withComment("Shelf life comment from partner spec")
                                        .build())
                                .withServiceLife(OfferPeriodInfo.builder()
                                        .withPeriod(OfferPeriod.builder()
                                                .withMonths(6)
                                                .build())
                                        .withComment("Life time comment from partner spec")
                                        .build())
                                .build())
                        .build())
                .withSystemInfo(OfferSystemInfo.builder()
                        .withIdentifier(OfferIdentifier.builder()
                                .withShopId(partnerId)
                                .withShopSku(offerId)
                                .withMarketSku(100687839874L)
                                .build())
                        .withBid(new OfferBid(80))
                        .withHidden(Boolean.TRUE)
                        .build())
                .withTradeInfo(OfferTradeInfo.builder()
                        .withSupplyInfo(OfferSupplyInfo.builder()
                                .withSupplyUnitInfo(OfferSupplyUnitInfo.builder()
                                        .withTransportUnit(15)
                                        .withBoxCount(10)
                                        .withQuantum(1000)
                                        .withMinDeliveryPieces(5000)
                                        .build())
                                .withSupplyTimeInfo(OfferSupplyTimeInfo.builder()
                                        .withLeadTime(4)
                                        .withSchedule(DayOfWeek.MONDAY)
                                        .withSchedule(DayOfWeek.WEDNESDAY)
                                        .withSchedule(DayOfWeek.FRIDAY)
                                        .withSchedule(DayOfWeek.SATURDAY)
                                        .build())
                                .withSupplyStatus(OfferSupplyStatus.ACTIVE)
                                .build())
                        .withQuantityInfo(OfferQuantityInfo.builder()
                                .withQuantity(5)
                                .withMin(10)
                                .build())
                        .withPrice(OfferPrice.builder()
                                .withPrice(DataCampUtil.unpowFromIdx(3890000000L))
                                .withOldPrice(DataCampUtil.unpowFromIdx(5060000000L))
                                .withCurrency("RUR")
                                .withVat(VatRate.NO_VAT)
                                .build())
                        .withDeliveryInfo(OfferDeliveryInfo.builder()
                                .withStore(OfferReceivingInfo.builder()
                                        .withAvailable(false)
                                        .build())
                                .withPickup(OfferReceivingInfo.builder()
                                        .withAvailable(true)
                                        .withReceivingCondition(OfferReceivingCondition.builder()
                                                .withCost(BigDecimal.ZERO)
                                                .withMinDays(4)
                                                .build())
                                        .build())
                                .withDelivery(OfferReceivingInfo.builder()
                                        .withAvailable(true)
                                        .withReceivingCondition(OfferReceivingCondition.builder()
                                                .withCost(BigDecimal.TEN)
                                                .withMinDays(3)
                                                .withMaxDays(5)
                                                .build())
                                        .build())
                                .withAvailable(true)
                                .build())
                        .withStocksCount(100L)
                        .withSkuDynamicPricingStrategy(
                                new SkuDynamicPricingStrategy.Builder()
                                        .withValue(100.0)
                                        .withThresholdType(DynamicPricingThresholdType.VALUE)
                                        .withType(DynamicPricingStrategy.REFERENCE)
                                        .build()
                        )
                        .build())
                .withPartnerInfo(OfferPartnerInfo.builder()
                        .withPictureUrl(
                                withValidPicUrl ? "https://image.com/ad" : "//image.com/ad"
                        )
                        .withPictureUrl(
                                "https://avatars.mds.yandex.net/get-marketpic/1662891/market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig")
                        .withPictureUrl(
                                "//avatars.mds.yandex.net/get-marketpic/1041839/market_3tgf4RglGTwniQNavB4giA_upload/orig")
                        .withUrl("https://boomaa.nethouse.ru/products/pkcell-ag3-10b")
                        .build())
                .withSuggestedInfo(OfferSuggestedInfo.builder()
                        .withModelName("Батарейка PKCELL Super Akaline Button Cell AG3")
                        .withCategoryName("Батарейки и аккумуляторы для аудио- и видеотехники")
                        .withMarketSku(151515L)
                        .build())
                .build();
    }

    @Nonnull
    public static UnitedOffer buildRequiredUnitedOffer(Integer partnerId, String offerId) {
        return buildRequiredUnitedOffer(partnerId, offerId, true);
    }

    @Nonnull
    public static UnitedOffer buildRequiredUnitedOffer(Integer partnerId, String offerId, boolean withValidPicUrl) {
        return UnitedOffer.builder()
                .withSystemInfo(OfferSystemInfo.builder()
                        .withIdentifier(OfferIdentifier.builder()
                                .withShopId(partnerId)
                                .withShopSku(offerId)
                                .build())
                        .build())
                .withPartnerInfo(OfferPartnerInfo.builder()
                        .withUrl("https://bez-granic.jp/ban")
                        .withPictureUrl("https://2020.nu")
                        .withPictureUrl("https://2021.su")
                        .withPictureUrl(
                                withValidPicUrl ? "https://avatars.mds.yandex.net/tu" : "//avatars.mds.yandex.net/tu"
                        )
                        .withPictureUrl(
                                withValidPicUrl ? "https://avatars.mds.yandex.net/ha" : "avatars.mds.yandex.net/ha"
                        )
                        .build())
                .withTradeInfo(OfferTradeInfo.builder()
                        .withPrice(OfferPrice.builder()
                                .withPrice(BigDecimal.valueOf(412.0))
                                .withCurrency("USD")
                                .build())
                        .build())
                .withContentInfo(OfferContentInfo.builder()
                        .withCategoryInfo(new OfferCategoryInfo("Телефон"))
                        .withPartnerSpecification(OfferPartnerSpecification.builder()
                                .withModelDescription(OfferModelDescription.builder()
                                        .withName("Ban")
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Nonnull
    public static UnitedOffer buildEmptyUnitedOfferFromInputOffer() {
        return UnitedOffer.builder()
                .withSystemInfo(OfferSystemInfo.builder()
                        .withIdentifier(OfferIdentifier.builder()
                                .build())
                        .build())
                .withContentInfo(OfferContentInfo.builder()
                        .withPartnerSpecification(OfferPartnerSpecification.builder()
                                .withModelDescription(OfferModelDescription.builder()
                                        .build())
                                .build())
                        .build())
                .withPartnerInfo(OfferPartnerInfo.builder()
                        .build())
                .withTradeInfo(OfferTradeInfo.builder()
                        .withPrice(OfferPrice.builder()
                                .build())
                        .build())
                .build();
    }

    @Nonnull
    public static UnitedOffer buildUnitedOfferFromInputOffer(String offerId) {
        return UnitedOffer.builder()
                .withSystemInfo(OfferSystemInfo.builder()
                        .withIdentifier(OfferIdentifier.builder()
                                .withShopSku(offerId)
                                .withMarketSku(7902348L)
                                .build())
                        .withHidden(true)
                        .build())
                .withContentInfo(OfferContentInfo.builder()
                        .withPartnerSpecification(OfferPartnerSpecification.builder()
                                .withModelDescription(OfferModelDescription.builder()
                                        .withName("Hidden offer")
                                        .withBarCodes(List.of("8576126490"))
                                        .build())
                                .build())
                        .withCategoryInfo(new OfferCategoryInfo("Товары для дома"))
                        .build())
                .withPartnerInfo(OfferPartnerInfo.builder()
                        .withUrl("https://yandex.market.ru/trade")
                        .build())
                .withTradeInfo(OfferTradeInfo.builder()
                        .withPrice(OfferPrice.builder()
                                .withVat(VatRate.NO_VAT)
                                .withPrice(BigDecimal.valueOf(932.0))
                                .withCurrency("USD")
                                .build())
                        .withStocksCount(83L)
                        .build())
                .build();
    }
}
