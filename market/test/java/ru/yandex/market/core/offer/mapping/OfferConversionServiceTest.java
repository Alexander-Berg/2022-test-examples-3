package ru.yandex.market.core.offer.mapping;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.offer.united.OfferPicture;
import ru.yandex.market.core.feed.offer.united.OfferPictureSource;
import ru.yandex.market.core.tanker.model.UserMessage;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ParametersAreNonnullByDefault
class OfferConversionServiceTest extends FunctionalTest {

    @Autowired
    private OfferConversionService offerConversionService;

    @Nonnull
    private static Matcher<ShopOffer> isTestMboShopOffer() {
        return Matchers.allOf(
                MbiMatchers.transformedBy(ShopOffer::supplierId, Matchers.is(662L)),
                MbiMatchers.transformedBy(ShopOffer::shopSku, Matchers.is("ShopSKU2")),
                MbiMatchers.transformedBy(ShopOffer::title, Matchers.is("Test Title")),
                MbiMatchers.transformedBy(ShopOffer::description,
                        MbiMatchers.isPresent(Matchers.is("Test Description"))),
                MbiMatchers.transformedBy(ShopOffer::categoryName,
                        MbiMatchers.isPresent(Matchers.is("Category/Name"))),
                MbiMatchers.transformedBy(ShopOffer::barcodes,
                        Matchers.contains("barcode1234")),
                MbiMatchers.transformedBy(ShopOffer::vendor,
                        MbiMatchers.isPresent(Matchers.is("Vendor3"))),
                MbiMatchers.transformedBy(ShopOffer::vendorCode,
                        MbiMatchers.isPresent(Matchers.is("VendorCode678"))),
                MbiMatchers.transformedBy(ShopOffer::urls,
                        Matchers.contains("http://test.ru/offer2"))
        );
    }

    @Test
    void testToMappedOfferAcceptedSupplierLink() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withAcceptedSupplierLink.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                Matchers.allOf(
                        MbiMatchers.transformedBy(MappedOffer::shopOffer, isTestMboShopOffer()),
                        MbiMatchers.transformedBy(
                                MappedOffer::activeLink,
                                MbiMatchers.isPresent(MappedOfferMatchers.isMarketSku(1234))
                        ),
                        MbiMatchers.transformedBy(
                                MappedOffer::partnerLink,
                                MbiMatchers.isPresent(
                                        MappedOfferMatchers.isModeratedLink(
                                                ModerationStatus.ACCEPTED,
                                                MappedOfferMatchers.isMarketSku(1235)
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void testToMappedOfferWithAcceptedAndModeratedSupplierLink() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withAcceptedAndModeratedSupplierLink.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                Matchers.allOf(
                        MbiMatchers.transformedBy(MappedOffer::shopOffer, isTestMboShopOffer()),
                        MbiMatchers.transformedBy(
                                MappedOffer::activeLink,
                                MbiMatchers.isPresent(MappedOfferMatchers.isMarketSku(1234))
                        ),
                        MbiMatchers.transformedBy(
                                MappedOffer::partnerLink,
                                MbiMatchers.isPresent(
                                        MappedOfferMatchers.isModeratedLink(
                                                ModerationStatus.MODERATION,
                                                MappedOfferMatchers.isMarketSku(1235)
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void testToMappedOfferWithoutAcceptedButWithModeratedSupplierLink() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withoutAcceptedButWithModeratedSupplierLink.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                Matchers.allOf(
                        MbiMatchers.transformedBy(MappedOffer::shopOffer, isTestMboShopOffer()),
                        MbiMatchers.transformedBy(MappedOffer::activeLink, Matchers.is(Optional.empty())),
                        MbiMatchers.transformedBy(
                                MappedOffer::partnerLink,
                                MbiMatchers.isPresent(
                                        MappedOfferMatchers.isModeratedLink(
                                                ModerationStatus.MODERATION,
                                                MappedOfferMatchers.isMarketSku(1235)
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void testToMappedOfferWithoutAcceptedButWithResortSupplierLink() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withoutAcceptedButWithResortSupplierLink.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                Matchers.allOf(
                        MbiMatchers.transformedBy(MappedOffer::shopOffer, isTestMboShopOffer()),
                        MbiMatchers.transformedBy(MappedOffer::activeLink, Matchers.is(Optional.empty())),
                        MbiMatchers.transformedBy(
                                MappedOffer::partnerLink,
                                MbiMatchers.isPresent(
                                        MappedOfferMatchers.isModeratedLink(
                                                ModerationStatus.MODERATION,
                                                MappedOfferMatchers.isMarketSku(1235)
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void testToMappedOfferWithoutAcceptedButWithAcceptedSupplierLink() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withoutAcceptedButWithAcceptedSupplierLink.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                Matchers.allOf(
                        MbiMatchers.transformedBy(MappedOffer::shopOffer, isTestMboShopOffer()),
                        MbiMatchers.transformedBy(MappedOffer::activeLink,
                                MbiMatchers.isPresent(MappedOfferMatchers.isMarketSku(1235))
                        ),
                        MbiMatchers.transformedBy(
                                MappedOffer::partnerLink,
                                MbiMatchers.isPresent(
                                        MappedOfferMatchers.isModeratedLink(
                                                ModerationStatus.ACCEPTED,
                                                MappedOfferMatchers.isMarketSku(1235)
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void testToMappedOfferWithoutAcceptedAndWithRejectedSupplierLink() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withoutAcceptedAndWithRejectedSupplierLink.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                Matchers.allOf(
                        MbiMatchers.transformedBy(MappedOffer::shopOffer, isTestMboShopOffer()),
                        MbiMatchers.transformedBy(MappedOffer::activeLink, Matchers.is(Optional.empty())),
                        MbiMatchers.transformedBy(
                                MappedOffer::partnerLink,
                                MbiMatchers.isPresent(
                                        MappedOfferMatchers.isModeratedLink(
                                                ModerationStatus.REJECTED,
                                                MappedOfferMatchers.isMarketSku(1235)
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void testToMappedOfferWithMinimumProperties() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.minimumProperties.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                MbiMatchers.<MappedOffer>newAllOfBuilder()
                        .add(MappedOffer::shopOffer, MbiMatchers.<ShopOffer>newAllOfBuilder()
                                .add(ShopOffer::title, "Test H125")
                                .add(ShopOffer::supplierId, 123L)
                                .add(ShopOffer::shopSku, "H125")
                                .add(ShopOffer::categoryName, Optional.empty())
                                .add(ShopOffer::barcodes, Matchers.empty())
                                .add(ShopOffer::vendorCode, Optional.empty())
                                .add(ShopOffer::vendor, Optional.empty())
                                .add(ShopOffer::description, Optional.empty())
                                .add(ShopOffer::vat, Optional.empty())
                                .add(ShopOffer::price, Optional.empty())
                                .add(ShopOffer::enabled, true)
                                .add(ShopOffer::masterData, Optional.empty())
                                .build())
                        .add(MappedOffer::activeLink, Optional.empty())
                        .add(MappedOffer::partnerLink, Optional.empty())
                        .add(MappedOffer::suggestedLink, Optional.empty())
                        .build()
        );
    }

    @Test
    void testToMappedOfferWithAllProperties() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.allProperties.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                MbiMatchers.<MappedOffer>newAllOfBuilder()
                        .add(MappedOffer::shopOffer, MbiMatchers.<ShopOffer>newAllOfBuilder()
                                .add(ShopOffer::title, "Test H123")
                                .add(ShopOffer::supplierId, 123L)
                                .add(ShopOffer::shopSku, "H123")
                                .add(ShopOffer::categoryName, MbiMatchers.isPresent("Shop/Category/Name"))
                                .add(ShopOffer::barcodes, Matchers.contains(
                                        "sdkgjsdh12431254",
                                        "sdjgh124314231",
                                        "dskjghs124152"))
                                .add(ShopOffer::vendorCode, MbiMatchers.isPresent("sgsd23523"))
                                .add(ShopOffer::vendor, MbiMatchers.isPresent("Apple"))
                                .add(ShopOffer::description, MbiMatchers.isPresent("Test H123 Description"))
                                .add(ShopOffer::vat, MbiMatchers.isPresent(VatRate.fromId(1)))
                                .add(ShopOffer::price, MbiMatchers.isPresent(BigDecimal.valueOf(12399, 2)))
                                .add(ShopOffer::enabled, false)
                                .add(ShopOffer::masterData, Optional.empty())
                                .add(o -> o.offerProcessingState().offerProcessingStatus(),
                                        OfferProcessingStatus.IN_WORK)
                                .build())
                        .add(MappedOffer::activeLink, MbiMatchers.isPresent(
                                MbiMatchers.transformedBy(MarketEntityInfo::marketSku, MbiMatchers.isPresent(
                                        MbiMatchers.<MarketSkuInfo>newAllOfBuilder()
                                                .add(MarketSkuInfo::marketSku, 1288L)
                                                .add(MarketSkuInfo::name, "MarketSku1288")
                                                .add(MarketSkuInfo::categoryId, 123L)
                                                .add(MarketSkuInfo::categoryName, "Category123")
                                                .add(MarketSkuInfo::timestamp, Instant.ofEpochSecond(1529341200L))
                                                .build()
                                ))
                        ))
                        .add(MappedOffer::partnerLink, MbiMatchers.isPresent(
                                MbiMatchers.<ModeratedLink<MarketEntityInfo>>newAllOfBuilder()
                                        .add(ModeratedLink::status, ModerationStatus.MODERATION)
                                        .add(ModeratedLink::target,
                                                MbiMatchers.transformedBy(MarketEntityInfo::marketSku,
                                                        MbiMatchers.isPresent(
                                                                MbiMatchers.<MarketSkuInfo>newAllOfBuilder()
                                                                        .add(MarketSkuInfo::marketSku, 1214L)
                                                                        .add(MarketSkuInfo::name, "MarketSku1214")
                                                                        .add(MarketSkuInfo::categoryId, 123L)
                                                                        .add(MarketSkuInfo::categoryName, "Category123")
                                                                        .add(MarketSkuInfo::timestamp,
                                                                                Instant.ofEpochSecond(1529351200L))
                                                                        .build()
                                                        )))
                                        .build()

                        ))
                        .add(MappedOffer::suggestedLink, MbiMatchers.isPresent(
                                MbiMatchers.transformedBy(MarketEntityInfo::marketSku, MbiMatchers.isPresent(
                                        MbiMatchers.<MarketSkuInfo>newAllOfBuilder()
                                                .add(MarketSkuInfo::marketSku, 1215L)
                                                .add(MarketSkuInfo::name, "MarketSku1215")
                                                .add(MarketSkuInfo::categoryId, 123L)
                                                .add(MarketSkuInfo::categoryName, "Category123")
                                                .add(MarketSkuInfo::timestamp, Instant.ofEpochSecond(1529361200L))
                                                .build()
                                ))
                        ))
                        .add(MappedOffer::deletedApproved, MbiMatchers.isPresent(
                                MbiMatchers.transformedBy(MarketEntityInfo::marketSku, MbiMatchers.isPresent(
                                        MbiMatchers.<MarketSkuInfo>newAllOfBuilder()
                                                .add(MarketSkuInfo::marketSku, 1216L)
                                                .add(MarketSkuInfo::name, "MarketSku1216")
                                                .add(MarketSkuInfo::categoryId, 123L)
                                                .add(MarketSkuInfo::categoryName, "Category123")
                                                .add(MarketSkuInfo::timestamp, Instant.ofEpochSecond(1529371200L))
                                                .build()
                                ))
                        ))
                        .build()
        );
    }

    @Test
    void testToMappedOfferWithIllegalValuesSkipped() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withIllegalValuesSkipped.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                MbiMatchers.<MappedOffer>newAllOfBuilder()
                        .add(MappedOffer::shopOffer, MbiMatchers.<ShopOffer>newAllOfBuilder()
                                .add(ShopOffer::title, "Test H124")
                                .add(ShopOffer::supplierId, 123L)
                                .add(ShopOffer::shopSku, "H124")
                                .add(ShopOffer::categoryName, MbiMatchers.isPresent("Shop/Category/Name"))
                                .add(ShopOffer::barcodes, Matchers.contains(
                                        "sdkgjsdh12431254",
                                        "sdjgh124314231",
                                        "dskjghs124152"))
                                .add(ShopOffer::vendorCode, MbiMatchers.isPresent("sgsd23523"))
                                .add(ShopOffer::vendor, MbiMatchers.isPresent("Apple"))
                                .add(ShopOffer::description, MbiMatchers.isPresent("Test H124 Description"))
                                .add(ShopOffer::vat, Optional.empty())
                                .add(ShopOffer::price, Optional.empty())
                                .add(ShopOffer::enabled, true)
                                .add(ShopOffer::masterData, Optional.empty())
                                .build())
                        .add(MappedOffer::activeLink, MbiMatchers.isPresent(
                                MbiMatchers.transformedBy(MarketEntityInfo::marketSku, MbiMatchers.isPresent(
                                        MbiMatchers.<MarketSkuInfo>newAllOfBuilder()
                                                .add(MarketSkuInfo::marketSku, 1288L)
                                                .add(MarketSkuInfo::name, "MarketSku1288")
                                                .add(MarketSkuInfo::categoryId, 123L)
                                                .add(MarketSkuInfo::categoryName, "Category123")
                                                .build()
                                ))
                        ))
                        .add(MappedOffer::partnerLink, Optional.empty())
                        .build()
        );
    }

    @Test
    void testToMappedOfferWithMasterData() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withMasterData.json",
                getClass()
        );

        MappedOffer actual = offerConversionService.toMappedOffer(mboOffer);

        MappedOffer expected = new MappedOffer.Builder()
                .setShopOffer(ShopOffer.builder()
                        .setSupplierId(123)
                        .setShopSku("H123")
                        .setVendorCode("sgsd23523")
                        .setVendor("Apple")
                        .addBarcode("sdkgjsdh12431254")
                        .addBarcode("sdjgh124314231")
                        .addBarcode("dskjghs124152")
                        .setTitle("Test H123")
                        .setCategoryName("Shop/Category/Name")
                        .setDescription("Test H123 Description")
                        .setVat(VatRate.fromId(1))
                        .setEnabled(false)
                        .setPrice(BigDecimal.valueOf(12399, 2))
                        .addUrl("http://url.ru/url")
                        .addParams(List.of(
                                new ShopOfferParam("param1", "val1", "unit4")
                        ))
                        .addPicture(OfferPicture.builder()
                                .withUrl("http://cdn.ya.ru/pic1")
                                .withSource(OfferPictureSource.DIRECT_LINK)
                                .build())
                        .addPicture(OfferPicture.builder()
                                .withUrl("http://cdn.ya.ru/pic2")
                                .withSource(OfferPictureSource.DIRECT_LINK)
                                .build())
                        .setAvailabilityStatus(AvailabilityStatus.ACTIVE)
                        .setMasterData(new MasterData.Builder()
                                .addManufacturerCountries(List.of("Россия"))
                                .setManufacturer("manuf100")
                                .setBoxCount(100)
                                .setQuantityInPack(90)
                                .setTransportUnitSize(33)
                                .setQuantumOfSupply(5)
                                .setDeliveryDuration(Duration.ofDays(3))
                                .addSupplyScheduleDays(List.of(
                                        DayOfWeek.TUESDAY,
                                        DayOfWeek.WEDNESDAY
                                ))
                                .setMinShipment(100)
                                .addGtins(List.of("gg1", "gg2"))
                                .setUseInMercury(true)
                                .addVetisGuids(List.of("vet1"))
                                .setWeightDimensions(new WeightDimensions.Builder()
                                        .setWeight(100L)
                                        .setWeightNet(200L)
                                        .setWeightTare(300L)
                                        .setWidth(400L)
                                        .setHeight(500L)
                                        .setLength(600L)
                                        .build())
                                .setShelfLife(TimePeriodWithUnits.ofDays(10))
                                .setLifeTime(TimePeriodWithUnits.ofDays(15))
                                .setLifeTimeComment("lifeTimeComment")
                                .build())
                        .setAcceptGoodContent(false)
                        .setAvailabilityStatus(AvailabilityStatus.ACTIVE)
                        .setOfferProcessingState(OfferProcessingState.builder()
                                .setOfferProcessingStatus(OfferProcessingStatus.IN_WORK)
                                .addOfferProcessingComments(List.of(
                                        new UserMessage.Builder()
                                                .setMessageCode("code1")
                                                .setDefaultTranslation("template")
                                                .build()
                                ))
                                .build())
                        .build())
                .setActiveLink(MarketEntityInfo.marketSku(MarketSkuInfo.of(
                        12345,
                        "товар",
                        MarketCategoryInfo.of(10, "категория"),
                        SkuType.MARKET,
                        Instant.ofEpochMilli(1625154265000L))))
                .build();

        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(expected);
    }

    @Test
    void testToMappedOfferWithEmptyMasterData() {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withEmptyMasterData.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);
        MatcherAssert.assertThat(
                mappedOffer,
                MbiMatchers.<MappedOffer>newAllOfBuilder()
                        .add(MappedOffer::shopOffer, MbiMatchers.<ShopOffer>newAllOfBuilder()
                                .add(ShopOffer::title, "Test H123")
                                .add(ShopOffer::supplierId, 123L)
                                .add(ShopOffer::shopSku, "H123")
                                .add(ShopOffer::categoryName, MbiMatchers.isPresent("Shop/Category/Name"))
                                .add(ShopOffer::barcodes, Matchers.contains(
                                        "sdkgjsdh12431254",
                                        "sdjgh124314231",
                                        "dskjghs124152"))
                                .add(ShopOffer::vendorCode, MbiMatchers.isPresent("sgsd23523"))
                                .add(ShopOffer::vendor, MbiMatchers.isPresent("Apple"))
                                .add(ShopOffer::description, MbiMatchers.isPresent("Test H123 Description"))
                                .add(ShopOffer::vat, MbiMatchers.isPresent(VatRate.fromId(1)))
                                .add(ShopOffer::price, MbiMatchers.isPresent(BigDecimal.valueOf(12399, 2)))
                                .add(ShopOffer::enabled, false)
                                .add(ShopOffer::masterData, Optional.empty())
                                .build())
                        .add(MappedOffer::activeLink, Matchers.is(Optional.empty()))
                        .add(MappedOffer::partnerLink, Matchers.is(Optional.empty()))
                        .add(MappedOffer::suggestedLink, Matchers.is(Optional.empty()))
                        .build()
        );
    }

    @Test
    void testMapDataCampUnitedOfferWithAllFields_parseMasterDataFromPartnerSpec() {
        DataCampUnitedOffer.UnitedOffer unitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.withAllFields.json",
                getClass()
        );

        MappedOffer expectedMappedOffer = new MappedOffer.Builder()
                .setShopOffer(ShopOffer.builder()
                        .setTitle("Батарейка AG3 щелочная PKCELL AG3-10B 10шт")
                        .setSupplierId(123456L)
                        .setShopSku("0516465165")
                        .setCategoryName("Батарейки и аккумуляторы")
                        .addBarcode("4985058793639")
                        .setVendorCode("CODE 228")
                        .setVendor("PKCELL")
                        .setDescription("Offer description")
                        .setPrice(BigDecimal.valueOf(389L))
                        .addUrl("https://boomaa.nethouse.ru/products/pkcell-ag3-10b")
                        .setEnabled(true)
                        .setAvailabilityStatus(AvailabilityStatus.ACTIVE)
                        .setMasterData(new MasterData.Builder()
                                .setMinShipment(5000)
                                .setShelfLife(TimePeriodWithUnits.ofHours(6 * 24 + 10))
                                .setShelfLifeComment("Shelf life comment from partner spec")
                                .setManufacturer("PKCELL")
                                .addManufacturerCountries(List.of("Китай", "Вьетнам"))
                                .setCustomsCommodityCode("8506101100, 3216101100")
                                .setDeliveryDuration(Duration.ofDays(4))
                                .setGuaranteePeriod(TimePeriodWithUnits.ofHours(2 * 30 * 24 + 3 * 24 + 4))
                                .setGuaranteePeriodComment("Guarantee period comment from partner spec")
                                .setLifeTime(TimePeriodWithUnits.ofMonths(6))
                                .setLifeTimeComment("Life time comment from partner spec")
                                .setQuantumOfSupply(1000)
                                .setBoxCount(10)
                                .addSupplyScheduleDays(List.of(
                                        DayOfWeek.MONDAY,
                                        DayOfWeek.WEDNESDAY,
                                        DayOfWeek.FRIDAY,
                                        DayOfWeek.SATURDAY
                                ))
                                .setTransportUnitSize(15)
                                .setWeightDimensions(new WeightDimensions.Builder()
                                        .setLength(50000L)
                                        .setWidth(10000L)
                                        .setHeight(10000L)
                                        .setWeight(50000L)
                                        .build()
                                )
                                .addCertificates(List.of("584723957169"))
                                .build()
                        )
                        .setOfferProcessingState(OfferProcessingState.builder()
                                .setOfferProcessingStatus(OfferProcessingStatus.READY)
                                .addOfferProcessingComments(List.of(
                                        new UserMessage.Builder()
                                                .setDefaultTranslation("Ошибка обработки оффера '555666'.")
                                                .setMessageCode("mboc.error.dc-offer-content-processing.failed")
                                                .setMustacheArguments("{\"offerId\":\"555666\"}")
                                                .build()
                                ))
                                .build())
                        .addContentUserMessage(new UserMessage.Builder()
                                .setDefaultTranslation("С изображением {{&url}} обнаружены проблемы.")
                                .setMessageCode("ir.partner_content.dcp.validation.image.mboInvalidImageFormat")
                                .setMustacheArguments("{\"invalidFormat\":\"false\"}")
                                .build())
                        .addPicture(OfferPicture.builder()
                                .withUrl("https://image.com/ad")
                                .withSource(OfferPictureSource.DIRECT_LINK)
                                .build())
                        .addPicture(OfferPicture.builder()
                                .withUrl("https://avatars.mds.yandex.net/get-marketpic/1662891/market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig")
                                .withSource(OfferPictureSource.MBO)
                                .build())
                        .addPicture(OfferPicture.builder()
                                .withUrl("//avatars.mds.yandex.net/get-marketpic/1041839/market_3tgf4RglGTwniQNavB4giA_upload/orig")
                                .withSource(OfferPictureSource.UPLOAD)
                                .build())
                        .build()
                )
                .setActiveLink(MarketEntityInfo.marketSku(MarketSkuInfo.of(
                        151515L,
                        "Батарейка PKCELL Super Akaline Button Cell AG3 СИНЕНЬКАЯ",
                        MarketCategoryInfo.of(1300L, "Батарейки и аккумуляторы для аудио- и видеотехники"),
                        SkuType.MARKET,
                        null)))
                .setPartnerLink(
                        MarketEntityInfo.marketSku(MarketSkuInfo.of(
                                151515L,
                                "Батарейка PKCELL Super Akaline Button Cell AG3 СИНЕНЬКАЯ",
                                MarketCategoryInfo.of(1300L, "Батарейки и аккумуляторы для аудио- и видеотехники"),
                                SkuType.MARKET,
                                null)),
                        ModerationStatus.MODERATION
                )
                .build();
        MappedOffer mappedOffer = offerConversionService.toMappedOffer(unitedOffer, 123456, true);
        ReflectionAssert.assertReflectionEquals(expectedMappedOffer, mappedOffer);
    }

    @Test
    void testMapDataCampUnitedOfferWithAllFields_parseMasterDataFromMDM() {
        DataCampUnitedOffer.UnitedOffer unitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.withAllFields.json",
                getClass()
        );

        MappedOffer expectedMappedOffer = new MappedOffer.Builder()
                .setShopOffer(ShopOffer.builder()
                        .setTitle("Батарейка AG3 щелочная PKCELL AG3-10B 10шт")
                        .setSupplierId(123456L)
                        .setShopSku("0516465165")
                        .setCategoryName("Батарейки и аккумуляторы")
                        .addBarcode("4985058793639")
                        .setVendorCode("CODE 228")
                        .setVendor("PKCELL")
                        .setDescription("Offer description")
                        .setPrice(BigDecimal.valueOf(389L))
                        .addUrl("https://boomaa.nethouse.ru/products/pkcell-ag3-10b")
                        .setEnabled(true)
                        .setAvailabilityStatus(AvailabilityStatus.ACTIVE)
                        .setMasterData(new MasterData.Builder()
                                .setMinShipment(5000)
                                .setShelfLife(TimePeriodWithUnits.ofWeeks(21))
                                .setShelfLifeComment("Shelf life comment from MDM")
                                .setManufacturer("PKCELL")
                                .addManufacturerCountries(List.of("Китай", "Вьетнам"))
                                .setCustomsCommodityCode("8506101100")
                                .setDeliveryDuration(Duration.ofDays(4))
                                .setGuaranteePeriod(TimePeriodWithUnits.ofYears(2))
                                .setGuaranteePeriodComment("Guarantee period comment from MDM")
                                .setLifeTime(TimePeriodWithUnits.ofMonths(6))
                                .setLifeTimeComment("Life time comment from MDM")
                                .setQuantumOfSupply(1000)
                                .setBoxCount(10)
                                .addSupplyScheduleDays(List.of(
                                        DayOfWeek.MONDAY,
                                        DayOfWeek.WEDNESDAY,
                                        DayOfWeek.FRIDAY,
                                        DayOfWeek.SATURDAY
                                ))
                                .setTransportUnitSize(15)
                                .setWeightDimensions(new WeightDimensions.Builder()
                                        .setLength(50000L)
                                        .setWidth(10000L)
                                        .setHeight(10000L)
                                        .setWeight(50000L)
                                        .build()
                                )
                                .build()
                        )
                        .setOfferProcessingState(OfferProcessingState.builder()
                                .setOfferProcessingStatus(OfferProcessingStatus.READY)
                                .addOfferProcessingComments(List.of(
                                        new UserMessage.Builder()
                                                .setDefaultTranslation("Ошибка обработки оффера '555666'.")
                                                .setMessageCode("mboc.error.dc-offer-content-processing.failed")
                                                .setMustacheArguments("{\"offerId\":\"555666\"}")
                                                .build()
                                ))
                                .build()
                        )
                        .addContentUserMessage(new UserMessage.Builder()
                                .setDefaultTranslation("С изображением {{&url}} обнаружены проблемы.")
                                .setMessageCode("ir.partner_content.dcp.validation.image.mboInvalidImageFormat")
                                .setMustacheArguments("{\"invalidFormat\":\"false\"}")
                                .build())
                        .addPicture(OfferPicture.builder()
                                .withUrl("https://image.com/ad")
                                .withSource(OfferPictureSource.DIRECT_LINK)
                                .build())
                        .addPicture(OfferPicture.builder()
                                .withUrl("https://avatars.mds.yandex.net/get-marketpic/1662891/market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig")
                                .withSource(OfferPictureSource.MBO)
                                .build())
                        .addPicture(OfferPicture.builder()
                                .withUrl("//avatars.mds.yandex.net/get-marketpic/1041839/market_3tgf4RglGTwniQNavB4giA_upload/orig")
                                .withSource(OfferPictureSource.UPLOAD)
                                .build())
                        .build()
                )
                .setActiveLink(MarketEntityInfo.marketSku(MarketSkuInfo.of(
                        151515L,
                        "Батарейка PKCELL Super Akaline Button Cell AG3 СИНЕНЬКАЯ",
                        MarketCategoryInfo.of(1300L, "Батарейки и аккумуляторы для аудио- и видеотехники"),
                        SkuType.MARKET,
                        null)))
                .setPartnerLink(
                        MarketEntityInfo.marketSku(MarketSkuInfo.of(
                                151515L,
                                "Батарейка PKCELL Super Akaline Button Cell AG3 СИНЕНЬКАЯ",
                                MarketCategoryInfo.of(1300L, "Батарейки и аккумуляторы для аудио- и видеотехники"),
                                SkuType.MARKET,
                                null)),
                        ModerationStatus.MODERATION
                )
                .build();
        MappedOffer mappedOffer = offerConversionService.toMappedOffer(unitedOffer, 123456, false);
        ReflectionAssert.assertReflectionEquals(expectedMappedOffer, mappedOffer);
    }

    @Test
    void testMapEmptyDataCampUnitedOffer() {
        DataCampUnitedOffer.UnitedOffer unitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.emptyFields.json",
                getClass()
        );

        MappedOffer expectedMappedOffer = new MappedOffer.Builder()
                .setShopOffer(ShopOffer.builder()
                        .setSupplierId(123456L)
                        .setShopSku("0516465165")
                        .setAvailabilityStatus(AvailabilityStatus.ACTIVE)
                        .setOfferProcessingState(OfferProcessingState.builder()
                                .setOfferProcessingStatus(OfferProcessingStatus.UNKNOWN).build())
                        .build())
                .build();

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(unitedOffer, 123456, false);
        ReflectionAssert.assertReflectionEquals(expectedMappedOffer, mappedOffer);
    }

    @ParameterizedTest
    @CsvSource({
            "true,60000",
            "false,40000"
    })
    void testParseWeightFromGramsField(boolean fromPartnerSpec, long expected) {
        DataCampUnitedOffer.UnitedOffer unitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.weightGrams.json",
                getClass()
        );

        Long weight = offerConversionService.toMappedOffer(
                unitedOffer,
                123456,
                fromPartnerSpec
        )
                .shopOffer()
                .masterData()
                .flatMap(MasterData::weightDimensions)
                .flatMap(WeightDimensions::weight)
                .orElse(null);
        assertEquals(expected, weight);
    }

    @Test
    void testMapTimePeriodsFromPartnerSpecification() {

        DataCampUnitedOffer.UnitedOffer unitedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                "proto/DataCampUnitedOffer.UnitedOffer.periodsFromPartnerSpec.json",
                getClass()
        );

        MappedOffer expectedMappedOffer = new MappedOffer.Builder()
                .setShopOffer(ShopOffer.builder()
                        .setSupplierId(123456L)
                        .setShopSku("0516465165")
                        .setAvailabilityStatus(AvailabilityStatus.ACTIVE)
                        .setMasterData(new MasterData.Builder()
                                .setShelfLife(TimePeriodWithUnits.ofDays(36))
                                .setGuaranteePeriod(TimePeriodWithUnits.ofHours(2 * 30 * 24 + 3 * 24 + 4))
                                .setLifeTime(TimePeriodWithUnits.ofMonths(18))
                                .build()
                        )
                        .setOfferProcessingState(OfferProcessingState.builder()
                                .setOfferProcessingStatus(OfferProcessingStatus.UNKNOWN).build())
                        .build())
                .build();

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(unitedOffer, 123456, true);
        ReflectionAssert.assertReflectionEquals(expectedMappedOffer, mappedOffer);
    }

    @Test
    @DisplayName("Конвертация MBO.MappedOffer в DataCamp.UnitedOffer")
    void testToDataCampOffer() {
        checkToDataCampConvert(null, "proto/DataCampUnitedOffer.UnitedOffer.testToDataCampOffer.json");
    }

    @Test
    @DisplayName("Конвертация MBO.MappedOffer в DataCamp.UnitedOffer с категорией из ЕОХ")
    void testToDataCampOfferWithCategory() {
        PartnerCategoryOuterClass.PartnerCategory category = PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                .setId(100)
                .setName("cat100")
                .build();

        checkToDataCampConvert(category, "proto/DataCampUnitedOffer.UnitedOffer.testToDataCampOfferWithCat.json");
    }

    private void checkToDataCampConvert(@Nullable PartnerCategoryOuterClass.PartnerCategory category,
                                        String expectedPath) {
        SupplierOffer.Offer mboOffer = ProtoTestUtil.getProtoMessageByJson(
                SupplierOffer.Offer.class,
                "proto/SupplierOffer.Offer.withMasterData.json",
                getClass()
        );

        MappedOffer mappedOffer = offerConversionService.toMappedOffer(mboOffer);

        DataCampOfferMeta.UpdateMeta updateMeta = DataCampOfferMeta.UpdateMeta.newBuilder()
                .setSource(DataCampOfferMeta.DataSource.MARKET_MBI_MIGRATOR)
                .setTimestamp(DateTimes.toTimestamp(DateTimes.toInstant(2020, 1, 2)))
                .build();


        DataCampUnitedOffer.UnitedOffer actual = offerConversionService.toDataCampOffer(2000L, mappedOffer, updateMeta, category);

        DataCampUnitedOffer.UnitedOffer expected = ProtoTestUtil.getProtoMessageByJson(
                DataCampUnitedOffer.UnitedOffer.class,
                expectedPath,
                getClass()
        );
        ProtoTestUtil.assertThat(actual)
                .isEqualTo(expected);
    }
}
