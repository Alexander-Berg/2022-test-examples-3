package ru.yandex.market.rg.asyncreport.promo;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.excel.XlsSheet;
import ru.yandex.market.common.excel.wrapper.PoiCell;
import ru.yandex.market.common.excel.wrapper.PoiRow;
import ru.yandex.market.common.excel.wrapper.PoiSheet;
import ru.yandex.market.common.excel.wrapper.PoiWorkbook;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.feed.supplier.report.SupplierReportPriceService;
import ru.yandex.market.core.feed.supplier.report.model.CheckPricesReportResponse;
import ru.yandex.market.core.supplier.promo.dto.PiPromoMechanicDto;
import ru.yandex.market.core.supplier.promo.model.offer.xls.PromocodeXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.service.loyalty.LoyaltyRestClientImpl;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferInfo;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;
import ru.yandex.market.rg.config.FunctionalTest;

import static Market.DataCamp.DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.supplier.promo.service.PromoService.BUSINESS_ID_ANAPLAN;
import static ru.yandex.market.core.supplier.promo.service.PromoService.TERMLESS_PROMO_DATE_TIME;
import static ru.yandex.market.core.supplier.promo.service.PromoService.getDateInSeconds;

@DbUnitDataSet(before = "before.csv")
public class AnaplanPromocodeGeneratorTest extends FunctionalTest {
    private static final String PREFIX_PROMO_NAME = "Вы настраиваете участие в акции: ";

    @Autowired
    private AnaplanPromocodeGenerator anaplanPromocodeGenerator;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private SaasService saasDataCampShopService;

    @Autowired
    private SupplierReportPriceService supplierReportPriceService;

    @Autowired
    private DataCampService dataCampService;

    @Autowired
    @Qualifier("loyaltyRestClientImpl")
    private LoyaltyRestClientImpl loyaltyRestClientImpl;

    @Test
    @DbUnitDataSet(before = "before_multiPromo.csv")
    public void testIntersectedPromos() {
        long partnerId = 10;
        long businessId = 1;
        String offerId = "hid.1000161";
        int year = LocalDateTime.now().getYear() + 1;
        LocalDateTime startDate = LocalDateTime.of(year, 9, 10, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(year, 9, 30, 23, 59, 0);
        String targetPromoId = "#12345";
        String targetPromocode = "12345XYZ";

        doReturn(SaasSearchResult.builder()
                .setOffers(
                        List.of(SaasOfferInfo.newBuilder()
                                .addOfferId(offerId)
                                .build()))
                .build())
                .when(saasDataCampShopService).searchBusinessOffers(any());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/assortment_multiPromo.json",
                getClass()
        );
        List<OffersBatch.UnitedOffersBatchResponse.Entry> entries1 = getUnitedOffersResponse.getOffersList().stream()
                .map(unitedOffer -> {
                    OffersBatch.UnitedOffersBatchResponse.Entry.Builder entryBuilder =
                            OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                                    .setUnitedOffer(unitedOffer);
                    return entryBuilder.build();
                }).collect(Collectors.toList());
        OffersBatch.UnitedOffersBatchResponse response = OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addAllEntries(entries1)
                .build();

        doReturn(response)
                .when(dataCampShopClient).getBusinessUnitedOffers(businessId, Set.of(offerId), partnerId);

        CompletableFuture<List<Map<String, CheckPricesReportResponse>>> offerPriceResponse =
                CompletableFuture.completedFuture(
                        List.of(Map.of(
                                offerId,
                                new CheckPricesReportResponse.Builder()
                                        .setOfferId(offerId)
                                        .setReportPrice(BigDecimal.valueOf(389))
                                        .build()
                        ))
                );

        doReturn(offerPriceResponse)
                .when(supplierReportPriceService).getOffersPricesInfo(partnerId, Set.of(offerId), false);

        var reportParams = new AnaplanPromoOffersParams();
        reportParams.setPromoId(targetPromoId);
        reportParams.setSupplierId(partnerId);
        reportParams.setMultiPromo(true);

        DataCampPromo.PromoDescription targetPromo = createPromocodePromo(
                targetPromoId, targetPromocode, startDate, endDate);

        mockStandardCashback(partnerId, businessId);
        mockCustomCashback(partnerId, businessId, year);
        mockLoyaltyGroups();
        mockIntersectedPromos(year);

        String expectedActivePromos = "" +
                "1) Прямая скидка «Direct Discount», скидка 50%, 20 — 22 сентября\n" +
                "2) Флеш-акция «Blue Flash», скидка 80%, 20 сентября\n" +
                "3) Промокод «12345XYZ», скидка 20%, 20 сентября — 10 ноября\n" +
                "4) Акция 2=1 «Cheapest As Gift», 20 сентября — 18 октября\n" +
                "5) Кешбэк баллами плюса 10% на группу «12345_promo_2», 19 — 21 сентября\n" +
                "6) Кешбэк баллами плюса 5% на группу «10_PCC_1634563510_10», бессрочно\n" +
                "7) Кешбэк баллами плюса 12% на весь ассортимент, бессрочно";

        Stream<PromocodeXlsPromoOffer> reportResultStream =
                anaplanPromocodeGenerator.generateOffersStreamWithSaas(reportParams, targetPromo, businessId);

        var listResult = reportResultStream.collect(Collectors.toList());
        Assertions.assertEquals(listResult.size(), 1);
        Assertions.assertEquals(listResult.get(0).getActivePromosNames(), expectedActivePromos);
        Assertions.assertEquals(listResult.get(0).getVendorName(), "Samsung");
    }

    private void mockIntersectedPromos(int year) {
        GetPromoBatchRequestWithFilters requestForPromo =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#258445267")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN)
                                                .build())
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#6666")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#6431")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#6419")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .build()
                        )
                        .setEnabled(true)
                        .setOnlyUnfinished(true)
                        .build();

        SyncGetPromo.GetPromoBatchResponse promo =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(
                                        createDirectDiscountPromo(
                                                "#258445267",
                                                LocalDateTime.of(year, 9, 20, 0, 0),
                                                LocalDateTime.of(year, 9, 22, 23, 59)))
                                .addPromo(
                                        createBlueFlashPromo(
                                                "#6419",
                                                LocalDateTime.of(year, 9, 20, 0, 0),
                                                LocalDateTime.of(year, 9, 20, 23, 59)))
                                .addPromo(
                                        createPromocodePromo(
                                                "#6431",
                                                "12345XYZ",
                                                LocalDateTime.of(year, 9, 20, 0, 0),
                                                LocalDateTime.of(year, 11, 10, 23, 59)))
                                .addPromo(
                                        createCheapestAsGiftPromo(
                                                "#6666",
                                                LocalDateTime.of(year, 9, 20, 0, 0),
                                                LocalDateTime.of(year, 10, 18, 23, 59)))
                        )
                        .build();
        doReturn(promo)
                .when(dataCampShopClient).getPromos(ArgumentMatchers.eq(requestForPromo));
    }

    private DataCampPromo.PromoDescription createCheapestAsGiftPromo(
            String promoId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule =
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                        .setWarehouseRestriction(createWarehouseRestriction(48339))
                        .build();
        DataCampPromo.PromoConstraints promoConstraints =
                DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(offersMatchingRule)
                        .setStartDate(getDateInSeconds(startDate))
                        .setEndDate(getDateInSeconds(endDate))
                        .setEnabled(true)
                        .build();

        DataCampPromo.PromoAdditionalInfo.Builder promoAdditionalInfoBuilder =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Cheapest As Gift")
                        .setSendPromoPi(true);

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT)
                )
                .setConstraints(promoConstraints)
                .setAdditionalInfo(promoAdditionalInfoBuilder)
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setCheapestAsGift(
                                        DataCampPromo.PromoMechanics.CheapestAsGift.newBuilder()
                                                .setCount(2)
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    public static DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction createWarehouseRestriction(int warehouseId) {
        return DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                        .addId(warehouseId)
                        .build()
                )
                .build();
    }

    private DataCampPromo.PromoDescription createBlueFlashPromo(
            String promoId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory> promoCategories = List.of(
                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                        .setId(90595L)
                        .setMinDiscount(20)
                        .build()
        );
        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule =
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                        .setCategoryRestriction(
                                DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addAllPromoCategory(promoCategories)
                                        .build()
                        )
                        .build();
        DataCampPromo.PromoConstraints promoConstraints =
                DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(offersMatchingRule)
                        .setEnabled(true)
                        .setStartDate(getDateInSeconds(startDate))
                        .setEndDate(getDateInSeconds(endDate))
                        .build();

        DataCampPromo.PromoAdditionalInfo promoAdditionalInfo =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Blue Flash")
                        .setSendPromoPi(true)
                        .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.BLUE_FLASH)
                )
                .setConstraints(promoConstraints)
                .setAdditionalInfo(promoAdditionalInfo)
                .build();
    }

    private DataCampPromo.PromoDescription createDirectDiscountPromo(
            String promoId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        List<DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory> promoCategories = List.of(
                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                        .setId(90595L)
                        .setMinDiscount(25)
                        .build()
        );
        DataCampPromo.PromoConstraints.OffersMatchingRule offersMatchingRule =
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder()
                        .setCategoryRestriction(
                                DataCampPromo.PromoConstraints.OffersMatchingRule.CategoryRestriction.newBuilder()
                                        .addAllPromoCategory(promoCategories)
                                        .build()
                        )
                        .build();
        DataCampPromo.PromoConstraints promoConstraints =
                DataCampPromo.PromoConstraints.newBuilder()
                        .addOffersMatchingRules(offersMatchingRule)
                        .setEnabled(true)
                        .setStartDate(getDateInSeconds(startDate))
                        .setEndDate(getDateInSeconds(endDate))
                        .build();

        DataCampPromo.PromoAdditionalInfo promoAdditionalInfo =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Direct Discount")
                        .setSendPromoPi(true)
                        .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                )
                .setConstraints(promoConstraints)
                .setAdditionalInfo(promoAdditionalInfo)
                .build();
    }

    private void mockLoyaltyGroups() {
        doReturn(new LoyaltyRestClientImpl.StandartPromosResponse(
                        0,
                        List.of(
                                new LoyaltyRestClientImpl.Group("default", 5, 5, 25, 1,
                                        List.of(new LoyaltyRestClientImpl.Category(90401, "Все товары"))),
                                new LoyaltyRestClientImpl.Group("cehac", 1, 1, 25, 0,
                                        List.of(new LoyaltyRestClientImpl.Category(198118, "Бытовая техника"),
                                                new LoyaltyRestClientImpl.Category(198119, "Электроника"),
                                                new LoyaltyRestClientImpl.Category(91009, "Компьютерная техника"))),
                                new LoyaltyRestClientImpl.Group("diy", 3, 3, 25, 1,
                                        List.of(new LoyaltyRestClientImpl.Category(91597, "Строительство и ремонт"),
                                                new LoyaltyRestClientImpl.Category(90719, "Дача, сад и огород"),
                                                new LoyaltyRestClientImpl.Category(16056423, "Сувенирная продукция"),
                                                new LoyaltyRestClientImpl.Category(90574, "Климатическая техника"),
                                                new LoyaltyRestClientImpl.Category(90402, "Авто"),
                                                new LoyaltyRestClientImpl.Category(91512, "Спорт и отдых"),
                                                new LoyaltyRestClientImpl.Category(90666, "Товары для дома")))
                        )
                )
        )
                .when(loyaltyRestClientImpl).getActualLoyaltyStandartPromos();
    }

    private void mockCustomCashback(long partnerId, long businessId, int year) {
        PromoDatacampRequest.Builder request = new PromoDatacampRequest.Builder(businessId);
        request.withPartnerId(partnerId);
        request.withPromoType(
                Collections.singleton(PiPromoMechanicDto.PARTNER_CUSTOM_CASHBACK.getPromoStorageType())
        );
        request.withEnabled(true);
        request.withOnlyUnfinished(true);
        request.withSourceTypes(Collections.singleton(Promo.ESourceType.PARTNER_SOURCE));
        doReturn(createCustomCashback(year))
                .when(dataCampService).getPromos(ArgumentMatchers.eq(request.build()));
    }

    private SyncGetPromo.GetPromoBatchResponse createCustomCashback(int year) {
        DataCampPromo.PromoDescription promo1 = createCustomCashbackPromo(
                "12345_promo",
                getDateInSeconds(LocalDateTime.of(year, 9, 19, 10, 20, 0)),
                getDateInSeconds(LocalDateTime.of(year, 9, 21, 17, 53, 20)),
                List.of(1L, 2L, 90595L),
                List.of(5L, 6L, 153061L),
                10,
                2,
                DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab.DYNAMIC_GROUPS,
                true);

        DataCampPromo.PromoDescription promo2 = createCustomCashbackPromo(
                "12345_qwe",
                getDateInSeconds(LocalDateTime.of(year, 9, 19, 10, 20, 0)),
                getDateInSeconds(TERMLESS_PROMO_DATE_TIME),
                List.of(10L, 20L, 30L),
                List.of(50L, 60L, 70L),
                15,
                1,
                DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab.DYNAMIC_GROUPS,
                true);

        DataCampPromo.PromoDescription promo3 = createCustomCashbackPromo(
                "10_PCC_1634563510",
                getDateInSeconds(LocalDateTime.of(year, 9, 19, 10, 20, 0)),
                getDateInSeconds(TERMLESS_PROMO_DATE_TIME),
                Collections.emptyList(),
                Collections.emptyList(),
                5,
                10,
                DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab.FILE,
                true);

        DataCampPromo.PromoDescriptionBatch batch = DataCampPromo.PromoDescriptionBatch.newBuilder()
                .addPromo(promo1)
                .addPromo(promo2)
                .addPromo(promo3)
                .build();

        return SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(batch)
                .build();
    }

    private DataCampPromo.PromoDescription createCustomCashbackPromo(
            String promoId,
            long startDate,
            long endDate,
            List<Long> categoryIds,
            List<Long> brandIds,
            Integer cashbackValue,
            int promoPriority,
            DataCampPromo.PromoMechanics.PartnerCustomCashback.CreationTab creationTab,
            boolean enabled
    ) {
        DataCampPromo.PromoConstraints.OffersMatchingRule.Builder offersMatchingRuleBuilder =
                DataCampPromo.PromoConstraints.OffersMatchingRule.newBuilder();

        if (CollectionUtils.isNotEmpty(categoryIds)) {
            offersMatchingRuleBuilder.setOrigionalCategoryRestriction(
                    createOriginalCategoryRestriction(categoryIds)
            );
        }
        if (CollectionUtils.isNotEmpty(brandIds)) {
            offersMatchingRuleBuilder.setOriginalBrandRestriction(
                    createOriginalBrandRestriction(brandIds)
            );
        }

        DataCampPromo.PromoConstraints promoConstraints = DataCampPromo.PromoConstraints.newBuilder()
                .setStartDate(startDate)
                .setEndDate(endDate)
                .addOffersMatchingRules(offersMatchingRuleBuilder.build())
                .setEnabled(enabled)
                .build();

        DataCampPromo.PromoMechanics.PartnerCustomCashback customCashback =
                DataCampPromo.PromoMechanics.PartnerCustomCashback.newBuilder()
                        .setMarketTariffsVersionId(0)
                        .setCashbackValue(cashbackValue)
                        .setPriority(promoPriority)
                        .setSource(creationTab)
                        .build();

        DataCampPromo.PromoMechanics mechanics = DataCampPromo.PromoMechanics.newBuilder()
                .setPartnerCustomCashback(customCashback)
                .build();

        DataCampPromo.PromoAdditionalInfo additionalInfo = DataCampPromo.PromoAdditionalInfo.newBuilder()
                .setName(promoId + "_" + promoPriority)
                .build();

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(PARTNER_CUSTOM_CASHBACK)
                )
                .setConstraints(promoConstraints)
                .setMechanicsData(mechanics)
                .setAdditionalInfo(additionalInfo)
                .build();
    }

    public static DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction createOriginalCategoryRestriction(
            List<Long> categoryIds
    ) {
        DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.Builder originalCategoryRestrictionBuilder =
                DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction.newBuilder();
        categoryIds.forEach(categoryId ->
                originalCategoryRestrictionBuilder.addIncludeCategegoryRestriction(
                        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoCategory.newBuilder()
                                .setId(categoryId)
                                .build()
                )
        );
        return originalCategoryRestrictionBuilder.build();
    }

    private static DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalBrandRestriction createOriginalBrandRestriction(
            List<Long> brandIds
    ) {
        DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.Builder promoBrandsBuilder =
                DataCampPromo.PromoConstraints.OffersMatchingRule.PromoBrands.newBuilder();
        brandIds.forEach(brandId ->
                promoBrandsBuilder.addBrands(
                        DataCampPromo.PromoBrand.newBuilder()
                                .setId(brandId)
                                .build()
                )
        );
        return DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalBrandRestriction.newBuilder()
                .setIncludeBrands(promoBrandsBuilder)
                .build();
    }

    private void mockStandardCashback(long partnerId, long businessId) {
        PromoDatacampRequest request = new PromoDatacampRequest.Builder(businessId)
                .withPartnerId(partnerId)
                .withPromoType(Set.of(DataCampPromo.PromoType.PARTNER_STANDART_CASHBACK))
                .withEnabled(true)
                .build();
        SyncGetPromo.GetPromoBatchResponse standardCashback = SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(
                        DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(createStandardCashback("standard_cb_promoId", (int) businessId))
                )
                .build();
        doReturn(standardCashback)
                .when(dataCampService).getPromos(ArgumentMatchers.eq(request));
    }

    private DataCampPromo.PromoDescription createStandardCashback(String promoId, int businessId) {
        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setPromoId(promoId)
                                .setBusinessId(businessId)
                )
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setPartnerStandartCashback(
                                        DataCampPromo.PromoMechanics.PartnerStandartCashback.newBuilder()
                                                .setMarketTariffsVersionId(0)
                                                .addStandartGroup(
                                                        DataCampPromo.PromoMechanics.
                                                                PartnerStandartCashback.StandartGroup.newBuilder()
                                                                .setCodeName("cehac")
                                                                .setValue(12)
                                                )
                                                .addStandartGroup(
                                                        DataCampPromo.PromoMechanics.
                                                                PartnerStandartCashback.StandartGroup.newBuilder()
                                                                .setCodeName("diy")
                                                                .setValue(5)
                                                )
                                                .addStandartGroup(
                                                        DataCampPromo.PromoMechanics.
                                                                PartnerStandartCashback.StandartGroup.newBuilder()
                                                                .setCodeName("default")
                                                                .setValue(18)
                                                )
                                )
                )
                .setConstraints(
                        DataCampPromo.PromoConstraints.newBuilder()
                                .setEnabled(true)
                                .setStartDate(getDateInSeconds(LocalDateTime.now()))
                                .setEndDate(getDateInSeconds(TERMLESS_PROMO_DATE_TIME))
                )
                .build();
    }

    private DataCampPromo.PromoDescription createPromocodePromo(
            String promoId,
            String promocode,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        DataCampPromo.PromoMechanics.MarketPromocode.Builder marketPromocodeBuilder =
                DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                        .setPromoCode(promocode)
                        .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.PERCENTAGE)
                        .setValue(20);

        DataCampPromo.PromoConstraints.Builder promoConstraintsBuilder =
                DataCampPromo.PromoConstraints.newBuilder()
                        .setStartDate(getDateInSeconds(startDate))
                        .setEndDate(getDateInSeconds(endDate))
                        .setEnabled(true);

        DataCampPromo.PromoAdditionalInfo.Builder promoAdditionalInfoBuilder =
                DataCampPromo.PromoAdditionalInfo.newBuilder()
                        .setName("Promocode")
                        .setSendPromoPi(true);

        return DataCampPromo.PromoDescription.newBuilder()
                .setPrimaryKey(
                        DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                .setSource(Promo.ESourceType.ANAPLAN)
                                .setPromoId(promoId)
                )
                .setPromoGeneralInfo(
                        DataCampPromo.PromoGeneralInfo.newBuilder()
                                .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE)
                )
                .setConstraints(
                        promoConstraintsBuilder
                )
                .setMechanicsData(
                        DataCampPromo.PromoMechanics.newBuilder()
                                .setMarketPromocode(marketPromocodeBuilder)
                )
                .setAdditionalInfo(
                        promoAdditionalInfoBuilder
                )
                .build();
    }

    @Test
    public void testMarketPromocode() {
        String reportId = "reportId";
        String promocodePromoId = "#6666";
        String discountPromoId = "#6431";
        long partnerId = 10;
        long businessId = 1;
        String promocodePromoName = "Маркетплейсненький промокодик";
        String discountPromoName = "Скидочка";
        AnaplanPromoOffersParams reportParams = new AnaplanPromoOffersParams();
        String offerId = "hid.1000161";
        reportParams.setPromoId(promocodePromoId);
        reportParams.setSupplierId(partnerId);

        mockPromocode(promocodePromoId, promocodePromoName);
        mockActivePromos(discountPromoId, discountPromoName, promocodePromoId, promocodePromoName);

        doReturn(SaasSearchResult.builder()
                .setOffers(
                        List.of(SaasOfferInfo.newBuilder()
                                .addOfferId(offerId)
                                .build()))
                .build())
                .when(saasDataCampShopService).searchBusinessOffers(any());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/marketPromocodeAssortment.json",
                getClass()
        );
        List<OffersBatch.UnitedOffersBatchResponse.Entry> entries1 = getUnitedOffersResponse.getOffersList().stream()
                .map(unitedOffer -> {
                    OffersBatch.UnitedOffersBatchResponse.Entry.Builder entryBuilder =
                            OffersBatch.UnitedOffersBatchResponse.Entry.newBuilder()
                                    .setUnitedOffer(unitedOffer);
                    return entryBuilder.build();
                }).collect(Collectors.toList());
        OffersBatch.UnitedOffersBatchResponse response = OffersBatch.UnitedOffersBatchResponse.newBuilder()
                .addAllEntries(entries1)
                .build();
        doReturn(response)
                .when(dataCampShopClient).getBusinessUnitedOffers(businessId, Set.of(offerId), partnerId);

        AtomicReference<PoiWorkbook> wb = new AtomicReference<>();
        when(amazonS3.putObject(anyString(), anyString(), any(File.class)))
                .then(a -> {
                    wb.set(PoiWorkbook.load((File) a.getArgument(2)));
                    return null;
                });

        CompletableFuture<List<Map<String, CheckPricesReportResponse>>> offerPriceResponse =
                CompletableFuture.completedFuture(
                        List.of(Map.of(
                                offerId,
                                new CheckPricesReportResponse.Builder()
                                        .setOfferId(offerId)
                                        .setReportPrice(BigDecimal.valueOf(3100))
                                        .build()
                        )));

        doReturn(offerPriceResponse)
                .when(supplierReportPriceService).getOffersPricesInfo(partnerId, Set.of(offerId), false);

        anaplanPromocodeGenerator.generate(reportId, reportParams);

        final PoiSheet sheet = wb.get().getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        Assertions.assertNotNull(sheet);

        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        Assertions.assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        Assertions.assertEquals(PREFIX_PROMO_NAME + promocodePromoName, promoNameCell.getFormattedCellValue().get());

        promoNameCell.getFormattedCellValue();
        List<Integer> headerRows = Arrays.asList(0, 1, 2);
        List<String> rowsOfferId1 = List.of(
                "null", "hid.1000161", "Микроволновая печь Samsung PG838R-W", "Samsung", "Микроволновые печи", "633", "3100", "3100", "1550", "Да", "null"
        );
        for (int rowNum = 0; rowNum <= 3; rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // not checking the header
            }
            final PoiRow row = sheet.getRow(rowNum);

            for (int colNum = 0; colNum <= 10; colNum++) {
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse("null");
                assertEquals(cellVal, rowsOfferId1.get(colNum));
            }
        }
    }

    private void mockActivePromos(String discountPromoId, String discountPromoName, String promocodePromoId, String promocodePromoName) {
        GetPromoBatchRequestWithFilters requestForPromo =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(promocodePromoId)
                                                .setBusinessId(BUSINESS_ID_ANAPLAN)
                                                .setSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN))
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(discountPromoId)
                                                .setBusinessId(BUSINESS_ID_ANAPLAN)
                                                .setSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN))
                                        .build()
                        )
                        .build();

        SyncGetPromo.GetPromoBatchResponse marketPromocodePromo =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                        .setPrimaryKey(
                                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId(discountPromoId)
                                                        .setBusinessId(BUSINESS_ID_ANAPLAN)
                                                        .setSource(Promo.ESourceType.ANAPLAN))
                                        .setPromoGeneralInfo(
                                                DataCampPromo.PromoGeneralInfo.newBuilder()
                                                        .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT))
                                        .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                .setName(discountPromoName)
                                                .build())
                                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                                .setStartDate(1)
                                                .setEndDate(2137307401)
                                                .setEnabled(true)
                                        )
                                )
                                .addPromo(createPromocode(promocodePromoId, promocodePromoName))
                        )
                        .build();
        doReturn(marketPromocodePromo).when(dataCampShopClient).getPromos(requestForPromo);
    }

    private void mockPromocode(String promocodePromoId, String promocodePromoName) {
        GetPromoBatchRequestWithFilters requestForTargetPromo =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(promocodePromoId)
                                                .setBusinessId(BUSINESS_ID_ANAPLAN)
                                                .setSource(NMarket.Common.Promo.Promo.ESourceType.ANAPLAN))
                                        .build()
                        )
                        .build();

        SyncGetPromo.GetPromoBatchResponse marketPromocodePromo =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(createPromocode(promocodePromoId, promocodePromoName))
                        )
                        .build();
        doReturn(marketPromocodePromo).when(dataCampShopClient).getPromos(requestForTargetPromo);
    }

    private DataCampPromo.PromoDescription createPromocode(String promocodePromoId, String promocodePromoName) {
       return DataCampPromo.PromoDescription.newBuilder()
                        .setPrimaryKey(
                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                        .setPromoId(promocodePromoId)
                                        .setBusinessId(BUSINESS_ID_ANAPLAN)
                                        .setSource(Promo.ESourceType.ANAPLAN))
                        .setAdditionalInfo(DataCampPromo.PromoAdditionalInfo.newBuilder()
                                .setName(promocodePromoName)
                                .build())
                        .setPromoGeneralInfo(
                                DataCampPromo.PromoGeneralInfo.newBuilder()
                                        .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE))
                        .setMechanicsData(DataCampPromo.PromoMechanics.newBuilder()
                                .setMarketPromocode(DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                                        .setValue(50)
                                        .setDiscountType(DataCampPromo.PromoMechanics.MarketPromocode.DiscountType.PERCENTAGE)
                                )
                        )
                        .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                .setStartDate(1)
                                .setEndDate(100)
                        )
                        .build();
    }
}
