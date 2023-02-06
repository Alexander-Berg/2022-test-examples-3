package ru.yandex.market.rg.asyncreport.promo;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalBrandRestriction;
import Market.DataCamp.DataCampPromo.PromoConstraints.OffersMatchingRule.OriginalCategoryRestriction;
import Market.DataCamp.DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import NMarket.Common.Promo.Promo;
import one.util.streamex.StreamEx;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;

import ru.yandex.market.common.excel.XlsSheet;
import ru.yandex.market.common.excel.wrapper.PoiCell;
import ru.yandex.market.common.excel.wrapper.PoiRow;
import ru.yandex.market.common.excel.wrapper.PoiSheet;
import ru.yandex.market.common.excel.wrapper.PoiWorkbook;
import ru.yandex.market.common.mds.s3.client.content.provider.FileContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.util.ParamsUtils;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.feed.supplier.report.SupplierReportPriceService;
import ru.yandex.market.core.feed.supplier.report.model.CheckPricesReportResponse;
import ru.yandex.market.core.offer.mapping.AsyncMboMappingService;
import ru.yandex.market.core.offer.mapping.MappedOffer;
import ru.yandex.market.core.offer.mapping.ShopOffer;
import ru.yandex.market.core.supplier.promo.dto.PiPromoMechanicDto;
import ru.yandex.market.core.supplier.promo.dto.PromocodeDiscountTypeDto;
import ru.yandex.market.core.supplier.promo.model.context.PromocodeTemplateContext;
import ru.yandex.market.core.supplier.promo.model.offer.xls.PromocodeXlsPromoOffer;
import ru.yandex.market.core.supplier.promo.service.loyalty.LoyaltyRestClientImpl;
import ru.yandex.market.core.supplier.promo.xlsx.SupplierPromoOffersXlsxProcessor;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.datacamp.stroller.model.GetPromoBatchRequestWithFilters;
import ru.yandex.market.mbi.datacamp.stroller.model.PromoDatacampRequest;
import ru.yandex.market.rg.config.FunctionalTest;

import static Market.DataCamp.DataCampPromo.PromoType.PARTNER_CUSTOM_CASHBACK;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.yandex.market.core.supplier.promo.service.PromoOffersFetcher.mergeUnitedOffers;
import static ru.yandex.market.core.supplier.promo.service.PromoService.BUSINESS_ID_ANAPLAN;
import static ru.yandex.market.core.supplier.promo.service.PromoService.TERMLESS_PROMO_DATE_TIME;
import static ru.yandex.market.core.supplier.promo.service.PromoService.getDateInSeconds;

@DbUnitDataSet(before = "before.csv")
public class PartnerPromocodeGeneratorTest extends FunctionalTest {
    @Autowired
    PartnerPromocodeGenerator partnerPromocodeGenerator;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private DataCampService dataCampService;

    @Autowired
    private SupplierReportPriceService supplierReportPriceService;

    @Autowired
    private AsyncMboMappingService asyncMboMappingService;

    @Autowired
    @Qualifier("promocodePromoTemplate")
    private Resource promocodePromoTemplate;

    @Autowired
    private PromocodeTemplateContext promocodeTemplateContext;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    @Qualifier("loyaltyRestClientImpl")
    private LoyaltyRestClientImpl loyaltyRestClientImpl;

    @Test
    public void testEmptyReport() throws MalformedURLException {
        PartnerPromocodePotentialAssortmentParams params = new PartnerPromocodePotentialAssortmentParams();
        long supplierId = 10;
        String offerId = "hid.1000161";
        long businessId = 1L;
        params.setSupplierId(supplierId);
        params.setPromocodeDiscountType(PromocodeDiscountTypeDto.VALUE);
        params.setDiscountValue(500);
        params.setStartDate(LocalDateTime.now());
        params.setEndDate(LocalDateTime.now());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/assortment.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampService).searchBusinessOffers(any());

        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampService).getPromos(supplierId, businessId, Collections.emptySet());

        doReturn(CompletableFuture.completedFuture(List.of(Map.of())))
                .when(supplierReportPriceService).getOffersPricesInfo(supplierId, Set.of(offerId), false);

        doReturn(CompletableFuture.completedFuture(List.of()))
                .when(asyncMboMappingService).mappingsByShopSku(supplierId, Set.of(offerId));

        String fakeUrl = "http://fake.url";
        doReturn(new URL(fakeUrl)).when(mdsS3Client).getUrl(any());
        ArgumentCaptor<FileContentProvider> contentProvider = ArgumentCaptor.forClass(FileContentProvider.class);
        doNothing().when(mdsS3Client).upload(any(), contentProvider.capture());

        mockActivePromos();

        var reportResult = partnerPromocodeGenerator.generate("10", params);
        assertEquals(fakeUrl, reportResult.getReportGenerationInfo().getUrlToDownload());
        assertNotNull(contentProvider.getValue().getFile());
        assertEquals(ReportState.DONE, reportResult.getNewState());
    }

    @Test
    public void testDiscountValuePromocode() {
        long supplierId = 10;
        long businessId = 1;
        String offerId = "hid.1000161";

        PartnerPromocodePotentialAssortmentParams reportParams = new PartnerPromocodePotentialAssortmentParams();
        reportParams.setSupplierId(supplierId);
        reportParams.setDiscountValue(100);
        reportParams.setPromocodeDiscountType(PromocodeDiscountTypeDto.VALUE);
        reportParams.setStartDate(LocalDateTime.now());
        reportParams.setEndDate(LocalDateTime.now());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/assortment.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampService).searchBusinessOffers(any());

        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampService).getPromos(supplierId, businessId, Collections.emptySet());

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
                .when(supplierReportPriceService).getOffersPricesInfo(supplierId, Set.of(offerId), false);

        CompletableFuture<List<MappedOffer>> mappingByShopSkuResponse = CompletableFuture.completedFuture(
                List.of(new MappedOffer.Builder()
                        .setShopOffer(
                                new ShopOffer.Builder()
                                        .setSupplierId(supplierId)
                                        .setShopSku(offerId)
                                        .setMarketCategoryName("имя категории")
                                        .setTitle("Батарейка PKCELL Super Akaline Button Cell AG3 10 шт блистер")
                                        .build())
                        .build()
                )
        );

        doReturn(mappingByShopSkuResponse)
                .when(asyncMboMappingService).mappingsByShopSku(supplierId, Set.of(offerId));

        mockActivePromos();

        var reportResultStream =
                partnerPromocodeGenerator.getPromoOffers(reportParams, businessId);
        var listResult = reportResultStream.collect(Collectors.toList());
        PromocodeXlsPromoOffer expected =
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku(offerId)
                        .withMarketSku(1000161L)
                        .withName("Микроволновая печь Samsung PG838R-W")
                        .withCount(633L)
                        .withPrice(3100L)
                        .withPromocodePrice(289L)
                        .withCategoryName("Микроволновые печи")
                        .withVendorName("Samsung")
                        .withReportPrice(389L)
                        .withCategoryId(90595L)
                        .build();
        Assertions.assertThat(listResult).hasSize(1);
        Assertions.assertThat(listResult.get(0)).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DbUnitDataSet(before = "before_multiPromo.csv")
    public void testIntersectedPromos() {
        long supplierId = 10;
        long businessId = 1;
        String offerId = "hid.1000161";
        int year = LocalDateTime.now().getYear() + 1;
        LocalDateTime startDate = LocalDateTime.of(year, 9, 10, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(year, 9, 30, 23, 59, 0);

        PartnerPromocodePotentialAssortmentParams reportParams = new PartnerPromocodePotentialAssortmentParams();
        reportParams.setSupplierId(supplierId);
        reportParams.setDiscountValue(100);
        reportParams.setPromocodeDiscountType(PromocodeDiscountTypeDto.VALUE);
        reportParams.setStartDate(startDate);
        reportParams.setEndDate(endDate);
        reportParams.setPromoId("#12345");
        reportParams.setMultiPromo(true);

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/assortment_multiPromo.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampService).searchBusinessOffers(any());

        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampService).getPromos(supplierId, businessId, Collections.emptySet());

        mockPromocode("#12345", "Promocode");

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
                .when(supplierReportPriceService).getOffersPricesInfo(supplierId, Set.of(offerId), false);

        CompletableFuture<List<MappedOffer>> mappingByShopSkuResponse = CompletableFuture.completedFuture(
                List.of(new MappedOffer.Builder()
                        .setShopOffer(
                                new ShopOffer.Builder()
                                        .setSupplierId(supplierId)
                                        .setShopSku(offerId)
                                        .setMarketCategoryName("имя категории")
                                        .setTitle("Батарейка PKCELL Super Akaline Button Cell AG3 10 шт блистер")
                                        .build())
                        .build()
                )
        );

        doReturn(mappingByShopSkuResponse)
                .when(asyncMboMappingService).mappingsByShopSku(supplierId, Set.of(offerId));

        mockStandardCashback(supplierId, businessId);
        mockCustomCashback(supplierId, businessId, year);
        mockLoyaltyGroups();
        mockIntersectedPromos(year);

        String intersectedPromos = "" +
                "1) Прямая скидка «Direct Discount», скидка 50%, 20 — 22 сентября\n" +
                "2) Флеш-акция «Blue Flash», скидка 80%, 20 сентября\n" +
                "3) Промокод «12345XYZ», скидка 20%, 20 сентября — 10 ноября\n" +
                "4) Акция 2=1 «Cheapest As Gift», 20 сентября — 18 октября\n" +
                "5) Кешбэк баллами плюса 10% на группу «12345_promo_2», 19 — 21 сентября\n" +
                "6) Кешбэк баллами плюса 5% на группу «10_PCC_1634563510_10», бессрочно\n" +
                "7) Кешбэк баллами плюса 12% на весь ассортимент, бессрочно";

        var reportResultStream =
                partnerPromocodeGenerator.getPromoOffers(reportParams, businessId);
        var listResult = reportResultStream.collect(Collectors.toList());
        PromocodeXlsPromoOffer expected =
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku(offerId)
                        .withMarketSku(1000161L)
                        .withName("Микроволновая печь Samsung PG838R-W")
                        .withCount(633L)
                        .withPrice(3100L)
                        .withPromocodePrice(289L)
                        .withCategoryName("Микроволновые печи")
                        .withVendorName("Samsung")
                        .withReportPrice(389L)
                        .withCategoryId(90595L)
                        .withActivePromosNames(intersectedPromos)
                        .build();
        Assertions.assertThat(listResult).hasSize(1);
        Assertions.assertThat(listResult.get(0)).usingRecursiveComparison().isEqualTo(expected);
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

    public static WarehouseRestriction createWarehouseRestriction(int warehouseId) {
        return DataCampPromo.PromoConstraints.OffersMatchingRule.WarehouseRestriction.newBuilder()
                .setWarehouse(DataCampPromo.PromoConstraints.OffersMatchingRule.IntList.newBuilder()
                        .addId(warehouseId)
                        .build()
                )
                .build();
    }

    private DataCampPromo.PromoDescription createPromocodePromo(
            String promoId,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        DataCampPromo.PromoMechanics.MarketPromocode.Builder marketPromocodeBuilder =
                DataCampPromo.PromoMechanics.MarketPromocode.newBuilder()
                        .setPromoCode("12345XYZ")
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

    public static OriginalCategoryRestriction createOriginalCategoryRestriction(
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

    private static OriginalBrandRestriction createOriginalBrandRestriction(
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

    @Test
    public void testDiscountPercentagePromocode() {
        long supplierId = 10;
        long businessId = 1;
        String offerId = "hid.1000161";
        String categoryName = "Микроволновые печи";

        PartnerPromocodePotentialAssortmentParams reportParams = new PartnerPromocodePotentialAssortmentParams();
        reportParams.setSupplierId(supplierId);
        reportParams.setDiscountValue(20);
        reportParams.setPromocodeDiscountType(PromocodeDiscountTypeDto.PERCENTAGE);
        reportParams.setStartDate(LocalDateTime.now());
        reportParams.setEndDate(LocalDateTime.now());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/assortment.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampService).searchBusinessOffers(any());

        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampService).getPromos(supplierId, businessId, Collections.emptySet());

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
                .when(supplierReportPriceService).getOffersPricesInfo(supplierId, Set.of(offerId), false);

        CompletableFuture<List<MappedOffer>> mappingByShopSkuResponse = CompletableFuture.completedFuture(
                List.of(new MappedOffer.Builder()
                        .setShopOffer(
                                new ShopOffer.Builder()
                                        .setSupplierId(supplierId)
                                        .setShopSku(offerId)
                                        .setMarketCategoryName(categoryName)
                                        .setTitle("Батарейка PKCELL Super Akaline Button Cell AG3 10 шт блистер")
                                        .build())
                        .build()
                )
        );

        doReturn(mappingByShopSkuResponse)
                .when(asyncMboMappingService).mappingsByShopSku(supplierId, Set.of(offerId));

        mockActivePromos();

        var reportResultStream =
                partnerPromocodeGenerator.getPromoOffers(reportParams, businessId);
        var listResult = reportResultStream.collect(Collectors.toList());
        PromocodeXlsPromoOffer expected =
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku("hid.1000161")
                        .withMarketSku(1000161L)
                        .withName("Микроволновая печь Samsung PG838R-W")
                        .withVendorName("Samsung")
                        .withCount(633L)
                        .withPrice(3100L)
                        .withPromocodePrice(312L)
                        .withCategoryName(categoryName)
                        .withReportPrice(389L)
                        .withCategoryId(90595L)
                        .build();
        Assertions.assertThat(listResult).hasSize(1);
        Assertions.assertThat(listResult.get(0)).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    public void testDiscountManyOffers() {
        long supplierId = 10;
        long businessId = 1;
        String offerId1 = "hid.1000161";
        String offerId2 = "hid.1000165";
        String categoryName1 = "Микроволновые печи";
        String categoryName2 = "Микроволновые печи5";
        String nameOfferId1 = "Микроволновая печь Samsung PG838R-W";
        String nameOfferId2 = "Микроволновая печь Samsung PG838R-W5";

        PartnerPromocodePotentialAssortmentParams reportParams = new PartnerPromocodePotentialAssortmentParams();
        reportParams.setSupplierId(supplierId);
        reportParams.setDiscountValue(20);
        reportParams.setPromocodeDiscountType(PromocodeDiscountTypeDto.PERCENTAGE);
        reportParams.setStartDate(LocalDateTime.ofInstant(Instant.now().minus(2, ChronoUnit.DAYS), UTC));
        reportParams.setEndDate(LocalDateTime.ofInstant(Instant.now().plus(7, ChronoUnit.DAYS), UTC));

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/responseForMerge.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampService).searchBusinessOffers(any());
        String activePromoId = "#3946";
        SyncGetPromo.GetPromoBatchResponse response =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                        .setAdditionalInfo(
                                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                        .setName("Promo name")
                                                        .build())
                                        .setConstraints(
                                                DataCampPromo.PromoConstraints.newBuilder()
                                                        .setStartDate(Instant.now().minus(3, ChronoUnit.DAYS).getEpochSecond())
                                                        .setEndDate(Instant.now().plus(5, ChronoUnit.DAYS).getEpochSecond())
                                                        .setEnabled(true)
                                                        .build())
                                        .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                                .setPromoType(DataCampPromo.PromoType.CHEAPEST_AS_GIFT))
                                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(activePromoId)
                                                .build())
                                        .build())
                                .build())
                        .build();
        doReturn(response).when(dataCampService).getPromos(supplierId, businessId, Set.of(activePromoId));

        CompletableFuture<List<Map<String, CheckPricesReportResponse>>> offerPriceResponse =
                CompletableFuture.completedFuture(
                        List.of(Map.of(
                                offerId1,
                                new CheckPricesReportResponse.Builder()
                                        .setOfferId(offerId1)
                                        .setReportPrice(BigDecimal.valueOf(389))
                                        .build(),

                                offerId2,
                                new CheckPricesReportResponse.Builder()
                                        .setOfferId(offerId1)
                                        .setReportPrice(BigDecimal.valueOf(399))
                                        .build())
                        ));

        doReturn(offerPriceResponse)
                .when(supplierReportPriceService).getOffersPricesInfo(supplierId, Set.of(offerId1, offerId2), false);

        CompletableFuture<List<MappedOffer>> mappingByShopSkuResponse = CompletableFuture.completedFuture(
                List.of(new MappedOffer.Builder()
                        .setShopOffer(
                                new ShopOffer.Builder()
                                        .setSupplierId(supplierId)
                                        .setShopSku(offerId1)
                                        .setMarketCategoryName(categoryName1)
                                        .setTitle(nameOfferId1)
                                        .build())
                        .build()
                )
        );

        doReturn(mappingByShopSkuResponse)
                .when(asyncMboMappingService).mappingsByShopSku(supplierId, Set.of(offerId1));

        doReturn(null)
                .when(dataCampService).getPromos(supplierId, businessId, Collections.emptySet());

        var reportResultStream =
                partnerPromocodeGenerator.getPromoOffers(reportParams, businessId);
        var listResult = reportResultStream.collect(Collectors.toList());
        PromocodeXlsPromoOffer expected1 =
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku(offerId1)
                        .withMarketSku(1000161L)
                        .withName(nameOfferId1)
                        .withCount(633L)
                        .withPrice(3100L)
                        .withPromocodePrice(312L)
                        .withCategoryName(categoryName1)
                        .withVendorName("Samsung")
                        .withReportPrice(389L)
                        .withCategoryId(90595L)
                        .build();

        PromocodeXlsPromoOffer expected2 =
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku(offerId2)
                        .withMarketSku(1000165L)
                        .withName(nameOfferId2)
                        .withCount(12345L)
                        .withPrice(3100L)
                        .withPromocodePrice(320L)
                        .withCategoryName(categoryName2)
                        .withVendorName("Samsung")
                        .withActivePromosNames("Promo name")
                        .withReportPrice(399L)
                        .withCategoryId(90595L)
                        .build();
        Assertions.assertThat(listResult).hasSize(2);
        Assertions.assertThat(listResult.get(0)).usingRecursiveComparison().isEqualTo(expected2);
        Assertions.assertThat(listResult.get(1)).usingRecursiveComparison().isEqualTo(expected1);
    }


    @Test
    public void testGeneratePromocodeFile() throws IOException {
        long supplierId = 10;
        long businessId = 1;
        String offerId1 = "hid.1000161";
        String offerId2 = "hid.1000165";
        Long feedId = 54321L;
        String categoryName1 = "Микроволновые печи";
        String categoryName2 = "Микроволновые печи5";
        String nameOfferId1 = "Микроволновая печь Samsung PG838R-W";
        String nameOfferId2 = "Микроволновая печь Samsung PG838R-W5";

        PartnerPromocodePotentialAssortmentParams reportParams = new PartnerPromocodePotentialAssortmentParams();
        reportParams.setSupplierId(supplierId);
        reportParams.setDiscountValue(20);
        reportParams.setPromocodeDiscountType(PromocodeDiscountTypeDto.PERCENTAGE);
        reportParams.setStartDate(LocalDateTime.ofInstant(Instant.now().minus(2, ChronoUnit.DAYS), UTC));
        reportParams.setEndDate(LocalDateTime.ofInstant(Instant.now().plus(7, ChronoUnit.DAYS), UTC));

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/responseForMerge.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampService).searchBusinessOffers(any());
        String activePromoId = "#3946";
        SyncGetPromo.GetPromoBatchResponse response =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                        .setAdditionalInfo(
                                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                        .setName("Promo name")
                                                        .build())
                                        .setConstraints(
                                                DataCampPromo.PromoConstraints.newBuilder()
                                                        .setStartDate(Instant.now().minus(3, ChronoUnit.DAYS).getEpochSecond())
                                                        .setEndDate(Instant.now().plus(5, ChronoUnit.DAYS).getEpochSecond())
                                                        .setEnabled(true)
                                                        .build()
                                        )
                                        .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId(activePromoId)
                                                .build())
                                        .build())
                                .build())
                        .build();
        doReturn(response).when(dataCampService).getPromos(supplierId, businessId, Set.of(activePromoId));

        CompletableFuture<List<Map<String, CheckPricesReportResponse>>> offerPriceResponse =
                CompletableFuture.completedFuture(
                        List.of(Map.of(
                                offerId1,
                                new CheckPricesReportResponse.Builder()
                                        .setOfferId(offerId1)
                                        .setReportPrice(BigDecimal.valueOf(389))
                                        .build(),

                                offerId2,
                                new CheckPricesReportResponse.Builder()
                                        .setOfferId(offerId1)
                                        .setReportPrice(BigDecimal.valueOf(399))
                                        .build()
                        ))
                );

        doReturn(offerPriceResponse)
                .when(supplierReportPriceService).getOffersPricesInfo(supplierId, Set.of(offerId1, offerId2), false);

        CompletableFuture<List<MappedOffer>> mappingByShopSkuResponse = CompletableFuture.completedFuture(
                List.of(new MappedOffer.Builder()
                        .setShopOffer(
                                new ShopOffer.Builder()
                                        .setSupplierId(supplierId)
                                        .setShopSku(offerId1)
                                        .setMarketCategoryName(categoryName1)
                                        .setTitle(nameOfferId1)
                                        .build())
                        .build()
                )
        );

        doReturn(mappingByShopSkuResponse)
                .when(asyncMboMappingService).mappingsByShopSku(supplierId, Set.of(offerId1));

        doReturn(null)
                .when(dataCampService).getPromos(supplierId, Collections.emptySet());

        var reportResultStream =
                partnerPromocodeGenerator.getPromoOffers(reportParams, businessId);
        File reportFile = TempFileUtils.createTempFile();
        SupplierPromoOffersXlsxProcessor.fillTemplateWithSupplierPromoOffersStream(
                promocodePromoTemplate,
                reportFile,
                promocodeTemplateContext,
                reportResultStream
        );

        final PoiWorkbook wb = PoiWorkbook.load(reportFile);
        final PoiSheet sheet = wb.getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        assertNotNull(sheet);
        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        assertEquals("Вы настраиваете участие в акции: Промокод на скидку",
                promoNameCell.getFormattedCellValue().get());
        FileUtils.deleteQuietly(reportFile);
        List<Integer> headerRows = Arrays.asList(0, 1, 2);

        List<String> rowsOfferId1 = List.of(
                "null", offerId1, nameOfferId1, "Samsung", categoryName1, "633", "3100", "389", "312", "null", "null"
        );
        List<String> rowsOfferId2 = List.of(
                "null", offerId2, nameOfferId2, "Samsung", categoryName2, "12345", "3100", "399", "320", "null", "Promo name");
        for (int rowNum = 0; rowNum <= 4; rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // header description may be blank
            }
            final PoiRow row = sheet.getRow(rowNum);
            for (int colNum = 0; colNum <= 9; colNum++) {
                final String offerIdCellVal = row.getCell(1).getFormattedCellValue().orElse("null");
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse("null");

                if (offerId1.equals(offerIdCellVal)) {
                    assertEquals(rowsOfferId1.get(colNum), cellVal);
                } else if (offerId2.equals(offerIdCellVal)) {
                    assertEquals(rowsOfferId2.get(colNum), cellVal);
                }
            }
        }
    }


    @Test
    public void testGeneratePromocodeFileAfterPromocodeCreation() throws IOException {
        long supplierId = 10;
        long businessId = 1;
        String offerId1 = "hid.1000161";
        String offerId2 = "hid.1000165";
        String categoryName1 = "Микроволновые печи";
        String categoryName2 = "Микроволновые печи5";
        String nameOfferId1 = "Микроволновая печь Samsung PG838R-W";
        String nameOfferId2 = "Микроволновая печь Samsung PG838R-W5";
        String currentPromocodeId = "10_TYDGE";

        PartnerPromocodePotentialAssortmentParams reportParams = new PartnerPromocodePotentialAssortmentParams();
        reportParams.setSupplierId(supplierId);
        reportParams.setDiscountValue(20);
        reportParams.setPromocodeDiscountType(PromocodeDiscountTypeDto.PERCENTAGE);
        reportParams.setStartDate(LocalDateTime.ofInstant(Instant.now().minus(2, ChronoUnit.DAYS), UTC));
        reportParams.setEndDate(LocalDateTime.ofInstant(Instant.now().plus(7, ChronoUnit.DAYS), UTC));
        reportParams.setPromoId(currentPromocodeId);

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/responseForMerge_activePromocode.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampService).searchBusinessOffers(any());

        String activeAnaplanPromoId = "#3946";
        String anotherActivePromocodeId = "10_YDHEN";
        String futurePromocodeId = "10_RTSBB";
        String pastPromocodeId = "10_AAAAA";
        SyncGetPromo.GetPromoBatchResponse response =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(
                                        DataCampPromo.PromoDescription.newBuilder()
                                                .setAdditionalInfo(
                                                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                                .setName("Anaplan #3946 promo name")
                                                                .build()
                                                )
                                                .setConstraints(
                                                        DataCampPromo.PromoConstraints.newBuilder()
                                                                .setStartDate(Instant.now().minus(3, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEndDate(Instant.now().plus(5, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEnabled(true)
                                                                .build()
                                                )
                                                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId(activeAnaplanPromoId)
                                                        .build()
                                                )
                                                .build()
                                )
                                .addPromo(
                                        DataCampPromo.PromoDescription.newBuilder()
                                                .setAdditionalInfo(
                                                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                                .setName("Curr promocode name")
                                                                .build())
                                                .setConstraints(
                                                        DataCampPromo.PromoConstraints.newBuilder()
                                                                .setStartDate(Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEndDate(Instant.now().plus(8, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEnabled(true)
                                                                .build()
                                                )
                                                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId(currentPromocodeId)
                                                        .build())
                                                .build()
                                )
                                .addPromo(
                                        DataCampPromo.PromoDescription.newBuilder()
                                                .setAdditionalInfo(
                                                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                                .setName("Another active promocode name")
                                                                .build())
                                                .setConstraints(
                                                        DataCampPromo.PromoConstraints.newBuilder()
                                                                .setStartDate(Instant.now().minus(13, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEndDate(Instant.now().plus(2, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEnabled(true)
                                                                .build())
                                                .setPromoGeneralInfo(DataCampPromo.PromoGeneralInfo.newBuilder()
                                                        .setPromoType(DataCampPromo.PromoType.MARKET_PROMOCODE))
                                                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId(anotherActivePromocodeId)
                                                        .build())
                                                .build()
                                )
                                .addPromo(
                                        DataCampPromo.PromoDescription.newBuilder()
                                                .setAdditionalInfo(
                                                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                                .setName("Future promocode name")
                                                                .build())
                                                .setConstraints(
                                                        DataCampPromo.PromoConstraints.newBuilder()
                                                                .setStartDate(Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEndDate(Instant.now().plus(22, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEnabled(true)
                                                                .build()
                                                )
                                                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId(futurePromocodeId)
                                                        .build())
                                                .build()
                                )
                                .addPromo(
                                        DataCampPromo.PromoDescription.newBuilder()
                                                .setAdditionalInfo(
                                                        DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                                .setName("Past promocode name")
                                                                .build())
                                                .setConstraints(
                                                        DataCampPromo.PromoConstraints.newBuilder()
                                                                .setStartDate(Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEndDate(Instant.now().plus(22, ChronoUnit.DAYS).getEpochSecond())
                                                                .setEnabled(true)
                                                                .build()
                                                )
                                                .setPrimaryKey(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId(pastPromocodeId)
                                                        .build())
                                                .build()
                                )
                                .build()
                        )
                        .build();
        Set<String> promoIds = Set.of(activeAnaplanPromoId, currentPromocodeId,
                anotherActivePromocodeId, futurePromocodeId, pastPromocodeId);
        doReturn(response).when(dataCampService)
                .getPromos(supplierId, businessId, promoIds);

        CompletableFuture<List<Map<String, CheckPricesReportResponse>>> offerPriceResponse =
                CompletableFuture.completedFuture(
                        List.of(Map.of(
                                offerId1,
                                new CheckPricesReportResponse.Builder()
                                        .setOfferId(offerId1)
                                        .setReportPrice(BigDecimal.valueOf(389))
                                        .build(),

                                offerId2,
                                new CheckPricesReportResponse.Builder()
                                        .setOfferId(offerId1)
                                        .setReportPrice(BigDecimal.valueOf(399))
                                        .build()
                        ))
                );

        doReturn(offerPriceResponse)
                .when(supplierReportPriceService).getOffersPricesInfo(supplierId, Set.of(offerId1, offerId2), false);

        CompletableFuture<List<MappedOffer>> mappingByShopSkuResponse = CompletableFuture.completedFuture(
                List.of(new MappedOffer.Builder()
                        .setShopOffer(
                                new ShopOffer.Builder()
                                        .setSupplierId(supplierId)
                                        .setShopSku(offerId1)
                                        .setMarketCategoryName(categoryName1)
                                        .setTitle(nameOfferId1)
                                        .build())
                        .build()
                )
        );

        doReturn(mappingByShopSkuResponse)
                .when(asyncMboMappingService).mappingsByShopSku(supplierId, Set.of(offerId1));

        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampService).getPromos(supplierId, Collections.emptySet());

        var reportResultStream =
                partnerPromocodeGenerator.getPromoOffers(reportParams, businessId);
        File reportFile = TempFileUtils.createTempFile();
        SupplierPromoOffersXlsxProcessor.fillTemplateWithSupplierPromoOffersStream(
                promocodePromoTemplate,
                reportFile,
                promocodeTemplateContext,
                reportResultStream
        );

        final PoiWorkbook wb = PoiWorkbook.load(reportFile);
        final PoiSheet sheet = wb.getSheet(XlsSheet.newBuilder().withName("Товары и цены").build());
        assertNotNull(sheet);
        PoiCell promoNameCell = sheet.getCell(0, (short) 1);
        assertTrue(promoNameCell.getFormattedCellValue().isPresent());
        assertEquals("Вы настраиваете участие в акции: Промокод на скидку",
                promoNameCell.getFormattedCellValue().get());
        FileUtils.deleteQuietly(reportFile);
        List<Integer> headerRows = Arrays.asList(0, 1, 2);

        List<String> rowsOfferId1 = List.of(
                "null", offerId1, "1000161", nameOfferId1, categoryName1, "633", "3100", "389", "312", "Да", "Another active promocode name"
        );
        List<String> rowsOfferId2 = List.of(
                "null", offerId2, "1000165", nameOfferId2, categoryName2, "12345", "3100", "399", "320", "Да", "null");
        for (int rowNum = 0; rowNum <= 4; rowNum++) {
            if (headerRows.contains(rowNum)) {
                continue; // header description may be blank
            }
            final PoiRow row = sheet.getRow(rowNum);
            for (int colNum = 0; colNum <= 10; colNum++) {
                final String offerIdCellVal = row.getCell(1).getFormattedCellValue().orElse("null");
                final String cellVal = row.getCell(colNum).getFormattedCellValue().orElse("null");
                System.out.println("rowNum: " + rowNum + "colNum:" + colNum);
                if (offerId1.equals(offerIdCellVal)) {
                    assertEquals(rowsOfferId1.get(colNum), cellVal);
                } else if (offerId2.equals(offerIdCellVal)) {
                    assertEquals(rowsOfferId2.get(colNum), cellVal);
                }
            }
        }
    }

    @Test
    void testDeserializeAssortmentParams() {
        PartnerPromocodePotentialAssortmentParams promocodePotentialAssortmentParams =
                new PartnerPromocodePotentialAssortmentParams();

        promocodePotentialAssortmentParams.setPromocodeDiscountType(PromocodeDiscountTypeDto.PERCENTAGE);
        promocodePotentialAssortmentParams.setDiscountValue(10);
        promocodePotentialAssortmentParams.setSupplierId(10263850);
        promocodePotentialAssortmentParams.setStartDate(LocalDateTime.of(2020, 3, 3, 0, 0, 0, 0));
        promocodePotentialAssortmentParams.setEndDate(LocalDateTime.of(2020, 3, 3, 23, 59, 59, 0));

        PartnerPromocodePotentialAssortmentParams deserializedParams =
                deserializeAssortmentParams("json/params.json");
        assertThat(deserializedParams).usingRecursiveComparison().isEqualTo(promocodePotentialAssortmentParams);
    }

    private PartnerPromocodePotentialAssortmentParams deserializeAssortmentParams(String jsonPath) {
        String rawBody = StringTestUtil.getString(getClass(), jsonPath);
        Map<String, Object> paramsMap = ParamsUtils.convertParamsToMap(rawBody);
        return ParamsUtils.convertToParams(paramsMap, PartnerPromocodePotentialAssortmentParams.class);
    }


    @Test
    public void testMergeUnitedOffers() {
        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/responseForMerge.json",
                getClass()
        );

        var mergeResponse =
                StreamEx.of(mergeUnitedOffers(getUnitedOffersResponse.getOffersList()))
                        .groupingBy(p -> p.getIdentifiers().getOfferId());
        assertEquals(2, mergeResponse.size());
        var hid_1000161List = mergeResponse.get("hid.1000161");
        assertEquals(2, hid_1000161List.size());
        hid_1000161List.forEach(
                p -> {
                    assertEquals(1000161L, p.getIdentifiers().getExtra().getMarketSkuId());
                    assertEquals(1000161L, p.getContent().getBinding().getApproved().getMarketSkuId());
                    assertEquals("hid.1000161", p.getIdentifiers().getOfferId());
                    assertEquals("Samsung PG838R-W", p.getContent().getMarket().getProductName());
                    assertEquals("Микроволновые печи", p.getContent().getMarket().getMarketCategory());
                    assertEquals("Микроволновые печи",
                            p.getContent().getBinding().getApproved().getMarketCategoryName());
                    assertTrue(p.getStockInfo().getMarketStocks().getCount() == 312 ||
                            p.getStockInfo().getMarketStocks().getCount() == 321);
                }
        );
        var hid_1000165List = mergeResponse.get("hid.1000165");
        assertEquals(1, hid_1000165List.size());
        hid_1000165List.forEach(
                p -> {
                    assertEquals(1000165L, p.getIdentifiers().getExtra().getMarketSkuId());
                    assertEquals(1000165L, p.getContent().getBinding().getApproved().getMarketSkuId());
                    assertEquals("hid.1000165", p.getIdentifiers().getOfferId());
                    assertEquals("Samsung PG838R-W5", p.getContent().getMarket().getProductName());
                    assertEquals("Микроволновые печи5", p.getContent().getMarket().getMarketCategory());
                    assertEquals("Микроволновые печи5",
                            p.getContent().getBinding().getApproved().getMarketCategoryName());
                    assertEquals(12345, p.getStockInfo().getMarketStocks().getCount());
                    assertEquals(1, p.getPromos().getAnaplanPromos().getActivePromos().getPromosList().size());
                    assertEquals("#3946", p.getPromos().getAnaplanPromos().getActivePromos().getPromosList()
                            .get(0).getId());
                }
        );
    }

    @Test
    @DbUnitDataSet(before = "getContentFromMbiAndDcTest.before.csv")
    void getContentFromMbiAndDcTest() {
        long supplierId = 10;
        long businessId = 1;
        String offerId = "hid.1000161";

        PartnerPromocodePotentialAssortmentParams reportParams = new PartnerPromocodePotentialAssortmentParams();
        reportParams.setSupplierId(supplierId);
        reportParams.setDiscountValue(100);
        reportParams.setPromocodeDiscountType(PromocodeDiscountTypeDto.VALUE);
        reportParams.setStartDate(LocalDateTime.now());
        reportParams.setEndDate(LocalDateTime.now());

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/assortment_without_category_name.json",
                getClass()
        );

        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampService).searchBusinessOffers(any());

        doReturn(SyncGetPromo.GetPromoBatchResponse.getDefaultInstance())
                .when(dataCampService).getPromos(supplierId, businessId, Collections.emptySet());

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
                .when(supplierReportPriceService).getOffersPricesInfo(supplierId, Set.of(offerId), false);

        doReturn(CompletableFuture.completedFuture(Collections.emptyList()))
                .when(asyncMboMappingService).mappingsByShopSku(supplierId, Set.of(offerId));

        var reportResultStream =
                partnerPromocodeGenerator.getPromoOffers(reportParams, businessId);
        var listResult = reportResultStream.collect(Collectors.toList());
        PromocodeXlsPromoOffer expected =
                new PromocodeXlsPromoOffer.Builder()
                        .withShopSku(offerId)
                        .withMarketSku(1000161L)
                        .withName("Микроволновая печь Samsung PG838R-W")
                        .withVendorName("Samsung")
                        .withCount(633L)
                        .withPrice(3100L)
                        .withPromocodePrice(289L)
                        .withCategoryName("Категория из бд mbi")
                        .withReportPrice(389L)
                        .withCategoryId(90595L)
                        .build();
        Assertions.assertThat(listResult).hasSize(1);
        Assertions.assertThat(listResult.get(0)).usingRecursiveComparison().isEqualTo(expected);
    }

    private void mockActivePromos() {
        GetPromoBatchRequestWithFilters requestForPromo =
                new GetPromoBatchRequestWithFilters.Builder()
                        .setRequest(
                                SyncGetPromo.GetPromoBatchRequest.newBuilder()
                                        .addEntries(DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                .setPromoId("#6431")
                                                .setBusinessId(0)
                                                .setSource(Promo.ESourceType.ANAPLAN))
                                        .build()
                        )
                        .build();

        SyncGetPromo.GetPromoBatchResponse promo =
                SyncGetPromo.GetPromoBatchResponse.newBuilder()
                        .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                                .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                        .setPrimaryKey(
                                                DataCampPromo.PromoDescriptionIdentifier.newBuilder()
                                                        .setPromoId("#6431")
                                                        .setBusinessId(0)
                                                        .setSource(Promo.ESourceType.ANAPLAN)
                                        )
                                        .setAdditionalInfo(
                                                DataCampPromo.PromoAdditionalInfo.newBuilder()
                                                        .setName("Прямая скидка!")
                                                        .build()
                                        )
                                        .setPromoGeneralInfo(
                                                DataCampPromo.PromoGeneralInfo.newBuilder()
                                                        .setPromoType(DataCampPromo.PromoType.DIRECT_DISCOUNT)
                                        )
                                        .setConstraints(
                                                DataCampPromo.PromoConstraints.newBuilder()
                                                        .setStartDate(1)
                                                        .setEndDate(1928838815L)
                                        )
                                )
                        )
                        .build();
        doReturn(promo).when(dataCampShopClient).getPromos(requestForPromo);
    }
}
