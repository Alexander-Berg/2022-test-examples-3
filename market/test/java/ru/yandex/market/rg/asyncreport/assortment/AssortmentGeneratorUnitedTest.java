package ru.yandex.market.rg.asyncreport.assortment;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import Market.DataCamp.SyncAPI.GetVerdicts;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
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
import ru.yandex.market.core.feed.offer.united.OfferQuantityInfo;
import ru.yandex.market.core.feed.offer.united.OfferReceivingInfo;
import ru.yandex.market.core.feed.offer.united.OfferResolutionInfo;
import ru.yandex.market.core.feed.offer.united.OfferSpecification;
import ru.yandex.market.core.feed.offer.united.OfferSuggestedInfo;
import ru.yandex.market.core.feed.offer.united.OfferSupplyInfo;
import ru.yandex.market.core.feed.offer.united.OfferSupplyStatus;
import ru.yandex.market.core.feed.offer.united.OfferSupplyTimeInfo;
import ru.yandex.market.core.feed.offer.united.OfferSupplyUnitInfo;
import ru.yandex.market.core.feed.offer.united.OfferSystemInfo;
import ru.yandex.market.core.feed.offer.united.OfferTankerMessage;
import ru.yandex.market.core.feed.offer.united.OfferTradeInfo;
import ru.yandex.market.core.feed.offer.united.OfferVendorInfo;
import ru.yandex.market.core.feed.offer.united.OfferVerdict;
import ru.yandex.market.core.feed.offer.united.OfferWeightDimensions;
import ru.yandex.market.core.feed.offer.united.UnitedOffer;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.feed.supplier.report.SupplierReportPriceService;
import ru.yandex.market.core.indexer.model.ReturnCode;
import ru.yandex.market.core.report.client.model.PriceRecommendationsDTO;
import ru.yandex.market.core.report.client.model.ReportRecommendationsResultDTO;
import ru.yandex.market.core.supplier.model.IndexerErrorInfo;
import ru.yandex.market.core.supplier.model.OfferInfo;
import ru.yandex.market.core.supplier.model.PriceSuggestType;
import ru.yandex.market.core.supplier.model.SuggestedPrice;
import ru.yandex.market.core.tanker.model.TankerKeySets;
import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.rg.asyncreport.assortment.model.AssortmentParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.mbi.datacamp.model.search.filter.PartnerSupplyPlan.WILL_SUPPLY;
import static ru.yandex.market.mbi.datacamp.model.search.filter.PartnerSupplyPlan.WONT_SUPPLY;

@DbUnitDataSet(before = {"csv/AssortmentFunctionalTest.csv", "csv/AssortmentUniteEnv.csv"})
class AssortmentGeneratorUnitedTest extends AbstractAssortmentGeneratorTest {

    private static final long SUPPLIER_ID = 774L;

    @Autowired
    private AsyncMarketReportService asyncMarketReportService;
    @Autowired
    @Qualifier("unitedSupplierXlsHelper")
    private SupplierXlsHelper unitedSupplierXlsHelper;
    @Autowired
    private SupplierReportPriceService reportPriceService;

    public AssortmentGeneratorUnitedTest() {
        super(false, false);
    }

    @BeforeEach
    void mockVerdicts() {
        GetVerdicts.GetVerdictsBatchResponse verdictsResponse = ProtoTestUtil.getProtoMessageByJson(
                GetVerdicts.GetVerdictsBatchResponse.class,
                "proto/AssortmentGeneratorTest.emptyVerdicts.json",
                getClass()
        );
        Mockito.doReturn(verdictsResponse)
                .when(dataCampShopClient).getVerdicts(any(), anyLong(), anyLong());
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/AssortmentGeneratorTest.scanLimit.before.csv"
    })
    void testAssortmentParamsConversion() {
        SearchBusinessOffersRequest expectedDataCampRequest = SearchBusinessOffersRequest.builder()
                .setBusinessId(1774L)
                .setPartnerId(774L)
                .setPageRequest(SeekSliceRequest.firstN(1000))
                .setScanLimit(10000)
                .setWithRetry(true)
                .setCreationTsFrom(1000L)
                .addMarketCategoryIds(Set.of(321L,654L))
                .build();
        AssortmentParams params = new AssortmentParams();
        params.setEntityId(774L);
        params.setCreationTsFrom(1000L);
        params.setMarketCategoryIds(Set.of(321L, 654L));

        SearchBusinessOffersRequest capturedRequest = runGeneratorAndCaptureDataCampRequest(params);
        assertThat(capturedRequest).usingRecursiveComparison().isEqualTo(expectedDataCampRequest);
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv")
    @CsvSource({
            "offer_with_direct_link_picture_type," +
                    "proto/AssortmentGeneratorTest.DataCampGetUnitedOffers.picture_url_type_direct_link.json," +
                    "https://yandex.ru/images/offer1.jpeg," + "https://yandex.ru/images/offer2.jpeg",
            "offer_with_mbo_picture_type," +
                    "proto/AssortmentGeneratorTest.DataCampGetUnitedOffers.picture_url_type_mbo.json," +
                    "https://yandex.ru/images/offer1.jpeg," + "https://yandex.ru/images/offer2.jpeg"
    })
    void testUnitedCatalogAssortmentReportGenerationWithDifferentPictureUrlTypes(String shopSku,
                                                                                 String offerJsonFileName,
                                                                                 String url1, String url2) {
        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                offerJsonFileName,
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(SUPPLIER_ID);
        assertThat(capturedOffers).hasSize(1);
        assertThat(capturedOffers.get(0).getShopSku()).isEqualTo(shopSku);
        assertThat(capturedOffers.get(0).getUnitedOffer().getPartnerInfo().getPictureUrls())
                .usingRecursiveComparison().isEqualTo(List.of(url1, url2));
    }

    @Test
    @DbUnitDataSet(before = "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv")
    void testUnitedCatalogAssortmentReportGeneration_blue() {
        checkPartnerAssortment(
                SUPPLIER_ID,
                100L,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponse_774.json"
        );
    }

    @ParameterizedTest
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorUniteTest.singleWarehouseStockEnabled.csv",
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv"
    })
    @CsvSource({
            "775, proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponse_775.json", //  dropship
            // dropship with yandex warehouse and with one of its own
            "775, proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponse_775_2.json",
            "776, proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponse_776.json", //  crossdock
            "777, proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponse_777.json"  //  dropship_by_seller
    })
    void testUnitedCatalogAssortmentReportGeneration_partnerWithItsOwnWarehouse(
            long partnerId,
            String mockUCatOfferJsonPath
    ) {
        checkPartnerAssortment(partnerId, 3L, mockUCatOfferJsonPath);
    }

    private void checkPartnerAssortment(long partnerId, long expectedStocksCount, String mockUCatOfferJsonPath) {
        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                mockUCatOfferJsonPath,
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        OfferInfo expectedOfferInfo = buildExpectedBlueOfferInfo(expectedStocksCount, (int) partnerId);

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(partnerId);
        assertThat(capturedOffers).hasSize(1);
        assertThat(capturedOffers.get(0)).usingRecursiveComparison().isEqualTo(expectedOfferInfo);
    }

    @Test
    @DbUnitDataSet(before = "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv")
    void testUnitedCatalogAssortmentReportGenerationEmpty() {
        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersMinimumFieldsResponse.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        OfferInfo expectedOfferInfo = buildExceptedEmptyUnitedOffer();

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(SUPPLIER_ID);
        assertThat(capturedOffers).hasSize(1);
        assertThat(capturedOffers.get(0)).usingRecursiveComparison().isEqualTo(expectedOfferInfo);
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/AssortmentSupplyPlansEnv.csv"
    })
    void testFilterSupplyPlanPositive() {
        AssortmentParams assortmentParams = new AssortmentParams();
        assortmentParams.setEntityId(SUPPLIER_ID);
        assortmentParams.setSupplyPlans(Set.of(WILL_SUPPLY));

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponse_774.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(assortmentParams);
        assertThat(capturedOffers).hasSize(1);
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/AssortmentSupplyPlansEnv.csv"
    })
    void testFilterSupplyPlanNegative() {
        AssortmentParams assortmentParams = new AssortmentParams();
        assortmentParams.setEntityId(SUPPLIER_ID);
        assortmentParams.setSupplyPlans(Set.of(WONT_SUPPLY));

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponse_774.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(assortmentParams);
        assertThat(capturedOffers).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/AssortmentEmptyBasicEnv.csv"
    })
    void testFilterEmptyBasicPositive() {

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponseWithFullBasic.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(SUPPLIER_ID);
        assertThat(capturedOffers).hasSize(1);
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/AssortmentEmptyBasicEnv.csv"
    })
    void testFilterEmptyBasicNegative() {

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponse_774.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(SUPPLIER_ID);
        assertThat(capturedOffers).isEmpty();
    }

    @Test
    @DisplayName("Вердикты из оффера остаются, не заменяются ручкой вердиктов")
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/testVerdictsFromStroller.before.csv"
    })
    void testVerdictsFromStroller() {
        List<String> actualVerdicts = getVerdictsFromReport();
        Assertions.assertThat(actualVerdicts)
                .containsExactlyInAnyOrder("text_1", "text_1", "text_2");
    }

    @Test
    @DisplayName("Вердикты из оффера заменяются на вердикты из ручки вердиктов")
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/testVerdictsFromStroller.before.csv",
            "csv/verdictReplacementEnabled.before.csv"
    })
    void testVerdictsFromStrollerReplacement() {
        List<String> actualVerdicts = getVerdictsFromReport();
        Assertions.assertThat(actualVerdicts)
                .containsExactlyInAnyOrder("text_2");
    }

    private List<String> getVerdictsFromReport() {
        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.testVerdictsFromStroller.json",
                getClass()
        );
        Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        GetVerdicts.GetVerdictsBatchResponse verdictsResponse = ProtoTestUtil.getProtoMessageByJson(
                GetVerdicts.GetVerdictsBatchResponse.class,
                "proto/testVerdictsFromStroller.json",
                getClass()
        );
        Mockito.doReturn(verdictsResponse)
                .when(dataCampShopClient).getVerdicts(any(), anyLong(), anyLong());


        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(SUPPLIER_ID);
        return capturedOffers.stream()
                .map(OfferInfo::getIndexerErrorInfos)
                .flatMap(Collection::stream)
                .map(IndexerErrorInfo::getDescription)
                .collect(Collectors.toList());
    }

    @Test
    @DbUnitDataSet(before = "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv")
    void generate_priceFeedInPriceTemplate_correctResult() throws IOException {
        String marketSku = "151515";
        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffers.responseWithApprovedMapping.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        ReportRecommendationsResultDTO mockedReportResult = new ReportRecommendationsResultDTO();
        mockedReportResult.setRecommendations(List.of(new PriceRecommendationsDTO(
                marketSku,
                List.of(
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(150),
                                BigDecimal.valueOf(500),
                                "buybox"
                        ),
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(200),
                                BigDecimal.valueOf(750),
                                "minPriceMarket"
                        ),
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(250),
                                BigDecimal.valueOf(1000),
                                "defaultOffer"
                        ),
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(300),
                                BigDecimal.valueOf(1250),
                                "maxOldPrice"
                        ),
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(350),
                                BigDecimal.valueOf(1500),
                                "priceLimit"
                        ),
                        new PriceRecommendationsDTO.PriceRecommendationDTO(
                                BigDecimal.valueOf(200),
                                BigDecimal.ZERO,
                                "maxDiscountPrice"
                        )
                )
        )));
        doReturn(CompletableFuture.completedFuture(mockedReportResult))
                .when(asyncMarketReportService)
                .async(any(), any());

        OfferInfo expectedSupplierOffer = buildExceptedSuggestUnitedOffer();

        AssortmentParams assortmentParams = new AssortmentParams();
        assortmentParams.setEntityId(SUPPLIER_ID);
        assortmentParams.setUseSuggesting(true);

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(assortmentParams);
        assertThat(capturedOffers).hasSize(1);
        assertThat(capturedOffers.get(0))
                .usingRecursiveComparison()
                .isEqualTo(expectedSupplierOffer);

        verify(unitedSupplierXlsHelper, never()).fillTemplateStreamed(any(), any(), any());
    }

    @Test
    @DbUnitDataSet(before = "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv")
    void generate_priceFeedInPriceTemplate_NoMSku() {
        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersMinimumFieldsResponse.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        OfferInfo expectedSupplierOffer = buildExceptedSuggestUnitedOfferNegative();

        AssortmentParams assortmentParams = new AssortmentParams();
        assortmentParams.setEntityId(SUPPLIER_ID);
        assortmentParams.setUseSuggesting(true);

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(assortmentParams);
        assertThat(capturedOffers).hasSize(1);
        assertThat(capturedOffers.get(0))
                .usingRecursiveComparison()
                .isEqualTo(expectedSupplierOffer);

        verify(reportPriceService, never()).getRecommendedPrice(any());
    }

    private static OfferInfo buildExpectedBlueOfferInfo(long stocksCount, int partnerId) {
        return OfferInfo.builder()
                .withShopSku("0516465165")
                .withUnitedOffer(UnitedOffer.builder()
                        .withContentInfo(OfferContentInfo.builder()
                                .withCategoryInfo(new OfferCategoryInfo("Батарейки и аккумуляторы"))
                                .withPartnerSpecification(OfferPartnerSpecification.builder()
                                        .withModelDescription(OfferModelDescription.builder()
                                                .withName("Батарейка AG3 щелочная PKCELL AG3-10B 10шт")
                                                .withDescription("Offer description from partner spec")
                                                .withVendorInfo(OfferVendorInfo.builder()
                                                        .withVendor("PKCELL")
                                                        .withVendorCode("CODE 228")
                                                        .build())
                                                .withBarCodes(List.of("4985058793639"))
                                                .build())
                                        .withSalesNotes("Предоплата 42%")
                                        .withCertificates(List.of("01234321", "76543210"))
                                        .build())
                                .withSpecification(OfferSpecification.builder()
                                        .withManufactureInfo(OfferManufactureInfo.builder()
                                                .withManufacturer("PKCELL 123")
                                                .withCountryOfOrigins(List.of("Индонезия", "Таиланд"))
                                                .build())
                                        .withWeightDimensions(OfferWeightDimensions.builder()
                                                .withWeight(130000L)
                                                .withDimensions(OfferDimensions.builder()
                                                        .withLength(7000L)
                                                        .withWidth(4000L)
                                                        .withHeight(2000L)
                                                        .build())
                                                .build())
                                        .withCustomsCommodityCodes(List.of("8506101100", "3216101100"))
                                        .build())
                                .withLifeCycleInfo(OfferLifeCycleInfo.builder()
                                        .withShelfLife(OfferPeriodInfo.builder()
                                                .withPeriod(OfferPeriod.builder()
                                                        .withHours(21)
                                                        .build())
                                                .withComment("Shelf life comment from partner spec")
                                                .build())
                                        .withServiceLife(OfferPeriodInfo.builder()
                                                .withPeriod(OfferPeriod.builder()
                                                        .withDays(8)
                                                        .withMonths(6)
                                                        .build())
                                                .withComment("Life time comment from partner spec")
                                                .build())
                                        .withWarranty(OfferPeriodInfo.builder()
                                                .withPeriod(OfferPeriod.builder()
                                                        .withMonths(2)
                                                        .withYears(1)
                                                        .build())
                                                .withComment("Guarantee period comment from partner spec")
                                                .build())
                                        .build())
                                .build())
                        .withSystemInfo(OfferSystemInfo.builder()
                                .withIdentifier(OfferIdentifier.builder()
                                        .withShopId(partnerId)
                                        .withShopSku("0516465165")
                                        .withMarketSku(100687839874L)
                                        .build())
                                .withHidden(true)
                                .build())
                        .withTradeInfo(OfferTradeInfo.builder()
                                .withPrice(OfferPrice.builder()
                                        .withPrice(new BigDecimal("389.0000000"))
                                        .withOldPrice(new BigDecimal("506.0000000"))
                                        .withVat(VatRate.NO_VAT)
                                        .build())
                                .withDeliveryInfo(OfferDeliveryInfo.builder()
                                        .withDelivery(OfferReceivingInfo.builder()
                                                .withAvailable(true)
                                                .build())
                                        .withPickup(OfferReceivingInfo.builder()
                                                .withAvailable(true)
                                                .build())
                                        .withStore(OfferReceivingInfo.builder()
                                                .withAvailable(false)
                                                .build())
                                        .build())
                                .withStocksCount(stocksCount)
                                .withSupplyInfo(OfferSupplyInfo.builder()
                                        .withSupplyStatus(OfferSupplyStatus.ACTIVE)
                                        .withSupplyTimeInfo(OfferSupplyTimeInfo.builder()
                                                .withLeadTime(4)
                                                .withSchedule(DayOfWeek.TUESDAY)
                                                .withSchedule(DayOfWeek.THURSDAY)
                                                .withSchedule(DayOfWeek.SUNDAY)
                                                .build())
                                        .withSupplyUnitInfo(OfferSupplyUnitInfo.builder()
                                                .withQuantum(200)
                                                .withMinDeliveryPieces(4000)
                                                .withTransportUnit(115)
                                                .withBoxCount(110)
                                                .build())
                                        .build())
                                .withQuantityInfo(OfferQuantityInfo.builder()
                                        .withQuantity(5)
                                        .withMin(10)
                                        .build())
                                .build())
                        .withPartnerInfo(OfferPartnerInfo.builder()
                                .withUrl("https://boomaa.nethouse.ru/products/pkcell-ag3-10b")
                                .withPictureUrl("https://yandex.ru/images/offer1.jpeg")
                                .withPictureUrl("https://yandex.ru/images/offer2.jpeg")
                                .build())
                        .withSuggestedInfo(OfferSuggestedInfo.builder()
                                .withModelName("Батарейка PKCELL Super Akaline Button Cell AG3")
                                .withCategoryName("Батарейки и аккумуляторы для аудио- и видеотехники")
                                .withMarketSku(151515L)
                                .build())
                        .withResolutionInfo(OfferResolutionInfo.builder()
                                .withVerdict(OfferVerdict.builder()
                                        .withLevel(ReturnCode.ERROR)
                                        .withBasic(false)
                                        .withCode("mboc.error.excel-value-is-required")
                                        .withTitle(OfferTankerMessage.builder()
                                                .withKeySet(TankerKeySets.SHARED_HIDDEN_OFFERS_SUBREASONS)
                                                .withCode("MDM_mboc.error.excel-value-is-required")
                                                .withParams(Map.of("header", "Страна производства"))
                                                .build()
                                        )
                                        .withDetails(OfferTankerMessage.builder()
                                                .withKeySet(TankerKeySets.SHARED_HIDDEN_OFFERS_DETAILS)
                                                .withCode("MDM_mboc.error.excel-value-is-required")
                                                .withParams(Map.of("header", "Страна производства"))
                                                .withDefaultText(
                                                        "Отсутствует значение для колонки 'Страна производства'"
                                                )
                                                .build())
                                        .build())
                                .withVerdict(OfferVerdict.builder()
                                        .withLevel(ReturnCode.ERROR)
                                        .withBasic(false)
                                        .withCode("")
                                        .withTitle(OfferTankerMessage.builder()
                                                .withKeySet("shared.hidden-offers.subreasons")
                                                .withCode("")
                                                .withParams(Map.of())
                                                .build())
                                        .withDetails(OfferTankerMessage.builder()
                                                .withKeySet("shared.hidden-offers.details")
                                                .withCode("")
                                                .withDefaultText("VerdictComment by MARKET_MBI_MIGRATOR")
                                                .withParams(Map.of())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private static OfferInfo buildExceptedEmptyUnitedOffer() {
        return OfferInfo.builder()
                .withShopSku("0516465165")
                .withUnitedOffer(UnitedOffer.builder()
                        .withContentInfo(OfferContentInfo.builder()
                                .withPartnerSpecification(OfferPartnerSpecification.builder()
                                        .withModelDescription(OfferModelDescription.builder()
                                                .withVendorInfo(OfferVendorInfo.builder().build())
                                                .build())
                                        .build())
                                .withSpecification(OfferSpecification.builder()
                                        .withManufactureInfo(OfferManufactureInfo.builder().build())
                                        .withWeightDimensions(OfferWeightDimensions.builder()
                                                .withDimensions(OfferDimensions.builder().build())
                                                .build())
                                        .build())
                                .withLifeCycleInfo(OfferLifeCycleInfo.builder()
                                        .withWarranty(OfferPeriodInfo.builder().build())
                                        .withShelfLife(OfferPeriodInfo.builder().build())
                                        .withServiceLife(OfferPeriodInfo.builder().build())
                                        .build())
                                .build())
                        .withSystemInfo(OfferSystemInfo.builder()
                                .withIdentifier(OfferIdentifier.builder()
                                        .withShopId(774)
                                        .withShopSku("0516465165")
                                        .build())
                                .build())
                        .withTradeInfo(OfferTradeInfo.builder()
                                .withSupplyInfo(OfferSupplyInfo.builder()
                                        .withSupplyUnitInfo(OfferSupplyUnitInfo.builder().build())
                                        .withSupplyTimeInfo(OfferSupplyTimeInfo.builder().build())
                                        .build())
                                .withQuantityInfo(OfferQuantityInfo.builder()
                                        .build())
                                .withPrice(OfferPrice.builder().build())
                                .withDeliveryInfo(OfferDeliveryInfo.builder().build())
                                .build())
                        .withPartnerInfo(OfferPartnerInfo.builder().build())
                        .withSuggestedInfo(OfferSuggestedInfo.builder().build())
                        .withResolutionInfo(OfferResolutionInfo.builder().build())
                        .build())
                .build();
    }

    private static OfferInfo buildExceptedSuggestUnitedOffer() {
        return OfferInfo.builder()
                .withShopSku("0516465165")
                .withUnitedOffer(UnitedOffer.builder()
                        .withContentInfo(OfferContentInfo.builder()
                                .withPartnerSpecification(OfferPartnerSpecification.builder()
                                        .withModelDescription(OfferModelDescription.builder()
                                                .withVendorInfo(OfferVendorInfo.builder().build())
                                                .build())
                                        .build())
                                .withSpecification(OfferSpecification.builder()
                                        .withManufactureInfo(OfferManufactureInfo.builder().build())
                                        .withWeightDimensions(OfferWeightDimensions.builder()
                                                .withDimensions(OfferDimensions.builder().build())
                                                .build())
                                        .build())
                                .withLifeCycleInfo(OfferLifeCycleInfo.builder()
                                        .withWarranty(OfferPeriodInfo.builder().build())
                                        .withShelfLife(OfferPeriodInfo.builder().build())
                                        .withServiceLife(OfferPeriodInfo.builder().build())
                                        .build())
                                .build())
                        .withSystemInfo(OfferSystemInfo.builder()
                                .withIdentifier(OfferIdentifier.builder()
                                        .withShopId(774)
                                        .withShopSku("0516465165")
                                        .withMarketSku(151515L)
                                        .build())
                                .build())
                        .withTradeInfo(OfferTradeInfo.builder()
                                .withSupplyInfo(OfferSupplyInfo.builder()
                                        .withSupplyUnitInfo(OfferSupplyUnitInfo.builder().build())
                                        .withSupplyTimeInfo(OfferSupplyTimeInfo.builder().build())
                                        .build())
                                .withQuantityInfo(OfferQuantityInfo.builder()
                                        .build())
                                .withPrice(OfferPrice.builder().build())
                                .withDeliveryInfo(OfferDeliveryInfo.builder().build())
                                .build())
                        .withPartnerInfo(OfferPartnerInfo.builder().build())
                        .withSuggestedInfo(OfferSuggestedInfo.builder()
                                .withCategoryName("Батарейки и аккумуляторы для аудио- и видеотехники")
                                .withModelName("Батарейка PKCELL Super Akaline Button Cell AG3")
                                .withMarketSku(151515L)
                                .build())
                        .withResolutionInfo(OfferResolutionInfo.builder()
                                .withVerdict(OfferVerdict.builder()
                                        .withLevel(ReturnCode.ERROR)
                                        .withBasic(true)
                                        .withCode("ir.partner_content.dcp.validation.image.mboInvalidImageFormat")
                                        .withDetails(OfferTankerMessage.builder()
                                                .withCode(
                                                        "ir.partner_content.dcp.validation.image.mboInvalidImageFormat"
                                                )
                                                .withKeySet("shared.offer-content.errors")
                                                .withParams(Map.of("invalidFormat", "false"))
                                                .withDefaultText("С изображением {{&url}} обнаружены проблемы.")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .withSuggestedPrice(PriceSuggestType.BUYBOX, new SuggestedPrice(
                        BigDecimal.valueOf(150),
                        BigDecimal.valueOf(500)))
                .withSuggestedPrice(PriceSuggestType.MIN_PRICE_MARKET, new SuggestedPrice(
                        BigDecimal.valueOf(200),
                        BigDecimal.valueOf(750)))
                .withSuggestedPrice(PriceSuggestType.DEFAULT_OFFER, new SuggestedPrice(
                        BigDecimal.valueOf(250),
                        BigDecimal.valueOf(1000)))
                .withSuggestedPrice(PriceSuggestType.MAX_OLD_PRICE, new SuggestedPrice(
                        BigDecimal.valueOf(300),
                        BigDecimal.valueOf(1250)))
                .withSuggestedPrice(PriceSuggestType.MARKET_OUTLIER_PRICE, new SuggestedPrice(
                        BigDecimal.valueOf(350),
                        BigDecimal.valueOf(1500)))
                .withSuggestedPrice(PriceSuggestType.MAX_DISCOUNT_PRICE, new SuggestedPrice(
                        BigDecimal.valueOf(200),
                        BigDecimal.ZERO))
                .build();
    }

    private static OfferInfo buildExceptedSuggestUnitedOfferNegative() {
        return OfferInfo.builder()
                .withShopSku("0516465165")
                .withUnitedOffer(UnitedOffer.builder()
                        .withContentInfo(OfferContentInfo.builder()
                                .withPartnerSpecification(OfferPartnerSpecification.builder()
                                        .withModelDescription(OfferModelDescription.builder()
                                                .withVendorInfo(OfferVendorInfo.builder().build())
                                                .build())
                                        .build())
                                .withSpecification(OfferSpecification.builder()
                                        .withManufactureInfo(OfferManufactureInfo.builder().build())
                                        .withWeightDimensions(OfferWeightDimensions.builder()
                                                .withDimensions(OfferDimensions.builder().build())
                                                .build())
                                        .build())
                                .withLifeCycleInfo(OfferLifeCycleInfo.builder()
                                        .withWarranty(OfferPeriodInfo.builder().build())
                                        .withShelfLife(OfferPeriodInfo.builder().build())
                                        .withServiceLife(OfferPeriodInfo.builder().build())
                                        .build())
                                .build())
                        .withSystemInfo(OfferSystemInfo.builder()
                                .withIdentifier(OfferIdentifier.builder()
                                        .withShopId(774)
                                        .withShopSku("0516465165")
                                        .build())
                                .build())
                        .withTradeInfo(OfferTradeInfo.builder()
                                .withSupplyInfo(OfferSupplyInfo.builder()
                                        .withSupplyUnitInfo(OfferSupplyUnitInfo.builder().build())
                                        .withSupplyTimeInfo(OfferSupplyTimeInfo.builder().build())
                                        .build())
                                .withQuantityInfo(OfferQuantityInfo.builder()
                                        .build())
                                .withPrice(OfferPrice.builder().build())
                                .withDeliveryInfo(OfferDeliveryInfo.builder().build())
                                .build())
                        .withPartnerInfo(OfferPartnerInfo.builder().build())
                        .withSuggestedInfo(OfferSuggestedInfo.builder()
                                .build())
                        .withResolutionInfo(OfferResolutionInfo.builder().build())
                        .build())
                .build();
    }
}
