package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import Market.DataCamp.SyncAPI.OffersBatch;
import Market.DataCamp.SyncAPI.SyncCategory;
import Market.UltraControllerServiceData.UltraController.EnrichedOffer.EnrichType;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.business.migration.BusinessMigrationServiceGrpc;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.model.unitedcatalog.CopyOffersParams;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.feed.validation.result.FeedXlsService;
import ru.yandex.market.core.supplier.model.OfferInfo;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mboc.http.MboMappings;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.rg.asyncreport.unitedcatalog.migration.UnitedOfferBuilder.offerBuilder;

/**
 * Тесты для {@link OffersToEkatCopier}.
 */
@DbUnitDataSet(before = "OffersToEkatCopierTest.before.csv")
class OffersToEkatCopierTest extends AbstractMigrationTaskProcessorTest {

    public static final long PARTNER_ID = 101L;
    public static final long BUSINESS_ID = 99L;
    private static final CopyOffersParams COPY_OFFERS_PARAMS =
            new CopyOffersParams(BUSINESS_ID, BUSINESS_ID, PARTNER_ID);

    @Autowired
    private ru.yandex.market.mboc.http.MboMappingsService mboMappingsService;

    @Autowired
    private FeedXlsService<OfferInfo> feedXlsService;

    OffersToEkatCopier offersToEkatCopier;

    @BeforeEach
    void setUp() {
        if (offersToEkatCopier == null) {
            offersToEkatCopier = new OffersToEkatCopier(
                    reportsMdsStorage, dataCampMigrationClient, reportsService, migrationTaskStateService,
                    assortmentReportWriteService, mboMappingService, offerConversionService,
                    clock, distributedMigrators, environmentService
            );
        }
        willAnswer(this::successMergeAnswer).given(mbocGrpcService).merge(any(), any());
    }

    @Test
    void empty() {
        willReturn(MboMappings.SearchMappingsResponse.newBuilder().build())
                .given(mboMappingsService).searchMappingsByShopId(any());
        willReturn(SyncCategory.PartnerCategoriesResponse.newBuilder().build())
                .given(dataCampMigrationClient).getPartnerCategories(anyLong());

        offersToEkatCopier.generate("1", COPY_OFFERS_PARAMS);

        ArgumentCaptor<MboMappings.SearchMappingsBySupplierIdRequest> captor =
                ArgumentCaptor.forClass(MboMappings.SearchMappingsBySupplierIdRequest.class);
        then(mboMappingsService).should().searchMappingsByShopId(captor.capture());
        Assertions.assertFalse(captor.getValue().getReturnMasterData(), "Should search without masterdata");
        then(dataCampMigrationClient).should().getPartnerCategories(anyLong());

        Mockito.verifyNoMoreInteractions(dataCampMigrationClient, mboMappingsService);
        Mockito.verifyNoMoreInteractions(mdmGrpcService, mboMappingsService);
        Mockito.verifyNoMoreInteractions(pppGrpcService, mboMappingsService);
    }

    @Test
    @DisplayName("Одна страница в МБО")
    void onePage() {
        mockMbo(BUSINESS_ID, PARTNER_ID, 1, new DataCampMockRequest(null, List.of()),
                MboMockResponse.ofOfferIds(null, List.of("1000"))
        );
        mockDataCamp(BUSINESS_ID, PARTNER_ID, 1, new DataCampMockRequest(null, List.of("1000")),
                DataCampMockResponse.ofOffers(null, List.of(
                        offerBuilder((int) BUSINESS_ID, null, "1000")
                                .withBasicPrice((int) PARTNER_ID)
                                .build())));
        mockPopulateService(mdmGrpcService);
        willReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .given(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());

        offersToEkatCopier.generate("1", COPY_OFFERS_PARAMS);

        //noinspection unchecked
        ArgumentCaptor<List<DataCampUnitedOffer.UnitedOffer>> captor = ArgumentCaptor.forClass(List.class);
        then(mboMappingsService).should(times(1)).searchMappingsByShopId(any());
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(eq(BUSINESS_ID), eq(PARTNER_ID), captor.capture());
        var actual = captor.getValue();
        assertEquals(1, actual.size());
        //Проверим, что установили флаг еката и скоуп
        assertEquals(DataCampOfferMeta.OfferScope.SELECTIVE, actual.get(0).getBasic().getMeta().getScope());
        assertTrue(actual.get(0).getBasic().getStatus().getUnitedCatalog().getFlag());
        assertEquals(DataCampOfferMeta.OfferScope.SELECTIVE,
                actual.get(0).getServiceMap().get((int) PARTNER_ID).getMeta().getScope());
        assertTrue(actual.get(0).getServiceMap().get((int) PARTNER_ID).getStatus().getUnitedCatalog().getFlag());
        assertTrue(actual.get(0).getServiceMap().get((int) PARTNER_ID).getStatus().getUnitedCatalog().hasMeta());
        assertTrue(actual.get(0).getServiceMap().get((int) PARTNER_ID).getPrice().hasOriginalPriceFields());
        assertEquals("новая вещь",
                actual.get(0).getBasic().getContent().getPartner().getOriginal().getName().getValue());
        assertEquals(1, actual.get(0).getBasic().getContent().getPartner().getOriginal().getCategory().getId());

        var content = actual.get(0).getBasic().getContent();
        assertEquals(100000, content.getMasterData().getDimensions().getHeightMkm());
        assertEquals(120000, content.getMasterData().getDimensions().getWidthMkm());
        assertEquals(140000, content.getMasterData().getDimensions().getLengthMkm());
        assertEquals(1000, content.getMasterData().getWeightGross().getGrams());
        assertEquals(101000, content.getPartner().getOriginal().getDimensions().getHeightMkm());
        assertEquals(121000, content.getPartner().getOriginal().getDimensions().getWidthMkm());
        assertEquals(141000, content.getPartner().getOriginal().getDimensions().getLengthMkm());
        assertEquals(1001, content.getPartner().getOriginal().getWeight().getGrams());
    }

    @Test
    @DisplayName("Одна страница в МБО, логгируем расхождение сорс и таргет")
    void onePageDifferentResolution() {
        environmentService.setValue("united.catalog.business_migration.mboc.logMismatch", "true");
        mockMbo(BUSINESS_ID, PARTNER_ID, 1, new DataCampMockRequest(null, List.of()),
                MboMockResponse.ofOfferIds(null, List.of("1000"))
        );
        mockDataCamp(BUSINESS_ID, PARTNER_ID, 1, new DataCampMockRequest(null, List.of("1000")),
                DataCampMockResponse.ofOffers(null, List.of(
                        offerBuilder((int) BUSINESS_ID, null, "1000")
                                .withBasicPrice((int) PARTNER_ID)
                                .build())));
        mockPopulateService(mdmGrpcService);
        willReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .given(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());
        willAnswer(invocation -> {
            StreamObserver<BusinessMigration.MergeOffersResponse> mergeOffersResponseObserver =
                    invocation.getArgument(1);
            mergeOffersResponseObserver.onNext(BusinessMigration.MergeOffersResponse.newBuilder()
                    .addMergeResponseItem(BusinessMigration.MergeOffersResponseItem.newBuilder()
                            .setResolution(BusinessMigration.Resolution.USE_TARGET)
                            .setResult(offerBuilder((int) BUSINESS_ID, null, "1000")
                                    .withBasicPrice((int) PARTNER_ID)
                                    .build())
                            .build())
                    .setSuccess(true).build());
            mergeOffersResponseObserver.onCompleted();
            return null;
        }).given(mbocGrpcService).merge(any(), any());

        offersToEkatCopier.generate("1", COPY_OFFERS_PARAMS);

        then(mboMappingsService).should(times(1)).searchMappingsByShopId(any());
    }

    @Test
    @DisplayName("Две страницы в МБО")
    void twoPages() {
        mockMbo(BUSINESS_ID, PARTNER_ID, 2, new DataCampMockRequest(null, List.of()),
                MboMockResponse.ofOfferIds("1000", List.of("1000"))
        );
        mockDataCamp(BUSINESS_ID, PARTNER_ID, 2, new DataCampMockRequest(null, List.of("1000")),
                DataCampMockResponse.ofOfferIds(null, List.of("1001"))
        );
        mockMbo(BUSINESS_ID, PARTNER_ID, 2, new DataCampMockRequest("1000", List.of()),
                MboMockResponse.ofOfferIds(null, List.of("1001"))
        );
        mockDataCamp(BUSINESS_ID, PARTNER_ID, 2, new DataCampMockRequest(null, List.of("1001")),
                DataCampMockResponse.ofOfferIds(null, List.of())
        );
        doReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .when(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());
        mockPopulateService(mdmGrpcService);
        //when
        offersToEkatCopier.generate("1", COPY_OFFERS_PARAMS);
        //then
        then(dataCampMigrationClient).should(times(4)).searchBusinessOffers(any());
        verify(mboMappingsService, times(2)).searchMappingsByShopId(any());

        //noinspection unchecked
        ArgumentCaptor<List<DataCampUnitedOffer.UnitedOffer>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataCampMigrationClient, times(2)).changeBusinessUnitedOffers(eq(BUSINESS_ID), eq(PARTNER_ID),
                captor.capture());
        verify(mdmGrpcService, times(2)).populate(any(), any());
        var actual = captor.getValue();
        assertEquals(1, actual.size());
        assertEquals("новая вещь",
                actual.get(0).getBasic().getContent().getPartner().getOriginal().getName().getValue());
        assertEquals("1001", actual.get(0).getBasic().getIdentifiers().getOfferId());
        assertEquals(1, actual.get(0).getBasic().getContent().getPartner().getOriginal().getCategory().getId());
    }

    @Test
    void testConflict() {
        environmentService.setValue("business.migration.batch.size", "10");

        UnitedOfferBuilder.clock = clock;
        mockMbo(BUSINESS_ID, PARTNER_ID, 3, new DataCampMockRequest(null, List.of()),
                MboMockResponse.ofOffers(null, List.of(
                        MboOfferBuilder.offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1000")
                                .withMapping(10000)
                                .withName("test1").build(),
                        MboOfferBuilder.offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1001")
                                .withMapping(10001)
                                .withName("strange-name")
                                .build(),
                        MboOfferBuilder.offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1002")
                                .withMapping(10002)
                                .withName("good-name")
                                .build(),
                        MboOfferBuilder.offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1003")
                                .withMapping(10003)
                                .withBarcode("1111")
                                .withVendorCode("rightvendor", "rightvendor")
                                .withName("good-name-2")
                                .build(),
                        MboOfferBuilder.offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1005")
                                .withMapping(10005)
                                .withName("only-mbo")
                                .build()))
        );
        mockPopulateService(mdmGrpcService);
        mockDataCamp(BUSINESS_ID, PARTNER_ID, 3, new DataCampMockRequest(null, List.of("1000", "1001", "1002", "1003"
                , "1005")),
                DataCampMockResponse.ofOffers(null, List.of(
                        // Другой marketsku в матчинге, проверяем название товара, но видя конфликт не берем маппинг
                        offerBuilder((int) BUSINESS_ID, null, "1000")
                                .withUcMapping(20000)
                                .withName("test2")
                                .build(),
                        // Одинаковые marketsku, до проверки названия дело не дойдет
                        offerBuilder((int) BUSINESS_ID, null, "1001")
                                .withMapping(10001)
                                .withName("non-conflicted-name")
                                .withBasicPrice((int) PARTNER_ID)
                                .build(),
                        // Другой marketsku, но название то же. Так можно
                        offerBuilder((int) BUSINESS_ID, null, "1002")
                                .withMapping(20002)
                                .withName("good-name").build(),
                        offerBuilder((int) BUSINESS_ID, null, "1003")
                                .withMapping(20003)
                                .withBarcode("1111") // штрихкод одинаковый, чтобы конфликт не влетел
                                .withVendorCode("wrongvendor", "wrongvendorcode") // отличается артикул
                                .withEnrichedOffer(20002, EnrichType.ET_MAIN) // автоматчинг по ответу УК
                                .withName("bad-name-2") // отличается название
                                .build()
                )));

        doReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .when(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());
        List<OfferInfo> capturedOffers = new ArrayList<>();

        var processedOffersCount = offersToEkatCopier.generate("1", COPY_OFFERS_PARAMS);

        //noinspection unchecked
        ArgumentCaptor<List<DataCampUnitedOffer.UnitedOffer>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(eq(BUSINESS_ID), eq(PARTNER_ID), captor.capture());

        var actual = captor.getValue();

        //TODO: emgusev при тестировании вероятно придется разобраться
        // с общими полями между базовой и сервисной часитью. Тест пока не удаляю, но может и не пригодиться
        Assertions.assertAll(
/*
                () -> assertThat(actual.get(0),
                        equalTo(offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1000")
                                .withMapping(20000)
                                .withName("test2")
                                .withBasicVerdict("sourceName", "test1", "targetName", "test2")
                                .withUcatFlag()
                                .withSelectiveMeta()
                                .build())),
                () -> assertThat(actual.get(1),
                        equalTo(offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1001")
                                .withMapping(10001)
                                .withName("non-conflicted-name")
                                .withUcatFlag()
                                .withSelectiveMeta()
                                .build())),
                () -> assertThat(actual.get(2),
                        equalTo(offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1002")
                                .withMapping(20002)
                                .withName("good-name")
                                .withUcatFlag()
                                .withSelectiveMeta()
                               .build()))
*/
                () -> assertThat(actual.get(3), // базовая часть взята из КИ, ЕКат базовая часть без маппинга
                        // проигнорирована
                        MbiMatchers.transformedBy(
                                // сравниваем только несколько интересующих полей, убеждаемся, что приехала из КИ
                                offer -> offerBuilder(offer.getBasic().getIdentifiers().getBusinessId(),
                                        offer.getServiceMap().keySet().iterator().next(),
                                        offer.getBasic().getIdentifiers().getOfferId())
                                        .withMapping(offer.getBasic().getContent().getBinding().getApproved().getMarketSkuId())
                                        .withBarcode(offer.getBasic().getContent().getPartner().getOriginal().getBarcode().getValue(0))
                                        .withVendorCode(offer.getBasic().getContent().getPartner().getOriginal().getVendor().getValue(),
                                                offer.getBasic().getContent().getPartner().getOriginal().getVendorCode().getValue())
                                        .withName(offer.getBasic().getContent().getPartner().getOriginal().getName().getValue())
                                        .build(),
                                equalTo(offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1003")
                                        .withMapping(10003)
                                        .withBarcode("1111")
                                        .withVendorCode("rightvendor", "rightvendor")
                                        .withName("good-name-2")
                                        .build()))),
                () -> assertFalse(actual.get(3).getBasic().hasResolution(), "no conflict")
        );

        var offer = captor.getValue().stream()
                .filter(o -> o.getBasic().getIdentifiers().getOfferId().equals("1000"))
                .findFirst().orElseThrow();
        assertThat(offer.getBasic().getContent().getPartner().getOriginal().getName().getValue(),
                equalTo("test2"));

        assertEquals(ReportResult.done(null,
                "Report done. Processed objects: 5"), processedOffersCount);
    }

    @Test
    @DisplayName("Выбираем категорию по названию")
    void resolveCategory() {
        mockMbo(BUSINESS_ID, PARTNER_ID, 1, new DataCampMockRequest(null, List.of()),
                MboMockResponse.ofOffers(null,
                        List.of(MboOfferBuilder.offerBuilder((int) BUSINESS_ID, (int) PARTNER_ID, "1000")
                                .withCategory("category1")
                                .build()))
        );
        mockDataCamp(BUSINESS_ID, PARTNER_ID, 1, new DataCampMockRequest(null, List.of("1000")),
                DataCampMockResponse.ofOfferIds(null, List.of())
        );
        mockPopulateService(mdmGrpcService);
        willReturn(SyncCategory.PartnerCategoriesResponse.newBuilder()
                .setCategories(
                        PartnerCategoryOuterClass.PartnerCategoriesBatch.newBuilder()
                                .addCategories(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                        .setBusinessId((int) BUSINESS_ID)
                                        .setId(2)
                                        .setName("category1"))
                                // Должна выбраться эта категория, как корневая с минимальным id
                                .addCategories(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                        .setBusinessId((int) BUSINESS_ID)
                                        .setId(1)
                                        .setName("category1"))
                                .addCategories(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                        .setBusinessId((int) BUSINESS_ID)
                                        .setId(11)
                                        .setParentId(1)
                                        .setName("category1"))
                                .addCategories(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                                        .setBusinessId((int) BUSINESS_ID)
                                        .setId(22)
                                        .setParentId(2)
                                        .setName("category1"))
                )
                .build())
                .given(dataCampMigrationClient).getPartnerCategories(eq(BUSINESS_ID));
        willReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .given(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());

        offersToEkatCopier.generate("1", COPY_OFFERS_PARAMS);

        //noinspection unchecked
        ArgumentCaptor<List<DataCampUnitedOffer.UnitedOffer>> captor = ArgumentCaptor.forClass(List.class);
        then(mboMappingsService).should(times(1)).searchMappingsByShopId(any());
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(eq(BUSINESS_ID), eq(PARTNER_ID), captor.capture());
        var actual = captor.getValue();
        assertEquals(1, actual.size());
        //Проверим, что установили флаг еката и скоуп
        var actualCategory = actual.get(0).getBasic().getContent().getPartner().getOriginal().getCategory();
        assertThat(actualCategory.toBuilder().clearMeta().build(),
                equalTo(PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                        .setName("category1")
                        .setId(1)
                        .setBusinessId((int) BUSINESS_ID)
                        .build()
                ));
    }


    BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mockPopulate(
            Function<BusinessMigration.PopulateOffersRequest, BusinessMigration.PopulateOffersResponse> fun) {
        return new BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase() {
            @Override
            public void populate(BusinessMigration.PopulateOffersRequest request,
                                 StreamObserver<BusinessMigration.PopulateOffersResponse> responseObserver) {
                var resp = fun.apply(request);
                responseObserver.onNext(resp);
                responseObserver.onCompleted();
            }
        };
    }

    void mockPopulateService(BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase service) {
        doAnswer(delegatesTo(mockPopulate(req -> BusinessMigration.PopulateOffersResponse.newBuilder()
                .setSuccess(true)
                .addAllItem(req.getItemList().stream()
                        .map(it -> BusinessMigration.PopulateOffersResponseItem.newBuilder()
                                .setResult(UnitedOfferBuilder.offerBuilder(
                                        it.getResult().getBasic().getIdentifiers().getBusinessId(),
                                        null,
                                        it.getResult().getBasic().getIdentifiers().getOfferId())
                                        .withMasterData(100, 120, 140, 1000)
                                        .withOriginalSpec(101, 121, 141, 1001).build()).build())
                        .collect(Collectors.toList()))
                .build()
        ))).when(service).populate(any(), any());
    }

}
