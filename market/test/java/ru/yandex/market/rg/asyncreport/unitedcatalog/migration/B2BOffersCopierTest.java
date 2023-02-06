package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.SyncAPI.OffersBatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.model.unitedcatalog.CopyOffersParams;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.supplier.model.OfferInfo;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.rg.asyncreport.unitedcatalog.migration.UnitedOfferBuilder.offerBuilder;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "B2BOffersCopierTest.before.csv")
class B2BOffersCopierTest extends AbstractMigrationTaskProcessorTest {

    @Autowired
    private EnvironmentService environmentService;


    B2BOffersCopier b2bOffersCopier;

    @BeforeEach
    void setUp() {
        b2bOffersCopier = new B2BOffersCopier(
                reportsMdsStorage, dataCampMigrationClient, reportsService, migrationTaskStateService,
                assortmentReportWriteService, mboMappingService, offerConversionService,
                clock, distributedMigrators, environmentService
        );
    }

    @Test
    void empty() throws IOException {
        long partnerId = 101L;
        mockDataCamp(99, partnerId, 0, new DataCampMockRequest(null, List.of()),
                DataCampMockResponse.ofOfferIds(null, List.of())
        );
        doReturn(SearchBusinessOffersResult.builder().build())
                .when(dataCampMigrationClient).searchBusinessOffers(any());
        doReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .when(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());
        willAnswer(this::successMergeAnswer).given(mbocGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(mdmGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(pppGrpcService).merge(any(), any());
        b2bOffersCopier.generate("1", objectMapper.readValue("{\"entityId\":101,\"partnerId\":101," +
                "\"sourceBusinessId\":\"99\",\"targetBusinessId\":\"98\"}", CopyOffersParams.class));
        ArgumentCaptor<SearchBusinessOffersRequest> captor =
                ArgumentCaptor.forClass(SearchBusinessOffersRequest.class);
        verify(dataCampMigrationClient).searchBusinessOffers(captor.capture());
        assertThat("Search with full", captor.getValue().getFull());
        verifyNoMoreInteractions(dataCampMigrationClient);
        verify(mbocGrpcService, times(0)).merge(any(), any());
        verify(mdmGrpcService, times(0)).merge(any(), any());
        verify(pppGrpcService, times(0)).merge(any(), any());
    }

    @Test
    @DbUnitDataSet(after = "B2BOffersCopierTest.onePage.after.csv")
    void onePage() {
        long partnerId = 101L;
        mockDataCamp(99, partnerId, 1, new DataCampMockRequest(null, List.of()),
                DataCampMockResponse.ofOfferIds(null, List.of("1000"))
        );
        mockDataCamp(98, partnerId, 1, new DataCampMockRequest(null, List.of("1000")),
                DataCampMockResponse.ofOfferIds(null, List.of())
        );
        doReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .when(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());
        willAnswer(this::successMergeAnswer).given(mbocGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(mdmGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(pppGrpcService).merge(any(), any());
        b2bOffersCopier.generate("1", new CopyOffersParams(99, 98, partnerId));
        verify(dataCampMigrationClient, times(2)).searchBusinessOffers(any());
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(98L, partnerId,
                List.of(offerBuilder(98, (int) partnerId, "1000").build()));
        verify(mbocGrpcService).merge(any(), any());
        verify(mdmGrpcService, times(0)).merge(any(), any());
        verify(pppGrpcService, times(0)).merge(any(), any());
    }

    @Test
    @DbUnitDataSet(after = "B2BOffersCopierTest.twoPage.after.csv")
    void twoPages() {
        checkTwoPagesFor("1", 101);
    }

    private void checkTwoPagesFor(String taskId, long partnerId) {
        mockDataCamp(99, partnerId, 2, new DataCampMockRequest(null, List.of()),
                DataCampMockResponse.ofOfferIds("1000", List.of("1000"))
        );
        mockDataCamp(98, partnerId, 2, new DataCampMockRequest(null, List.of("1000")),
                DataCampMockResponse.ofOfferIds(null, List.of("1001"))
        );
        mockDataCamp(99, partnerId, 2, new DataCampMockRequest("1000", List.of()),
                DataCampMockResponse.ofOfferIds(null, List.of("1001"))
        );
        mockDataCamp(98, partnerId, 2, new DataCampMockRequest(null, List.of("1001")),
                DataCampMockResponse.ofOfferIds(null, List.of())
        );
        doReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .when(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());
        willAnswer(this::successMergeAnswer).given(mbocGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(mdmGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(pppGrpcService).merge(any(), any());
        b2bOffersCopier.generate(taskId, new CopyOffersParams(99, 98, partnerId));
        verify(dataCampMigrationClient, times(4)).searchBusinessOffers(any());
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(98L, partnerId,
                List.of(offerBuilder(98, (int) partnerId, "1000").build())
        );
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(98L, partnerId,
                List.of(offerBuilder(98, (int) partnerId, "1001").build())
        );
        verify(mbocGrpcService, times(2)).merge(any(), any());
        verify(mdmGrpcService, times(0)).merge(any(), any());
        verify(pppGrpcService, times(0)).merge(any(), any());
    }

    @Test
    @DisplayName("Таска вырубается после полного копирования первой страницы." +
            "Проверяем что новая таска стартует со второй страницы.")
    @DbUnitDataSet(
            before = "B2BOffersCopierTest.fail1.before.csv"
    )
    void failedAfterFirstPage_startsFromSecond() {
        long partnerId = 101L;
        mockDataCamp(99, partnerId, 2, new DataCampMockRequest("1000", List.of()),
                DataCampMockResponse.ofOfferIds(null, List.of("1001"))
        );
        mockDataCamp(98, partnerId, 2, new DataCampMockRequest(null, List.of("1001")),
                DataCampMockResponse.ofOfferIds(null, List.of())
        );
        doReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .when(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());
        willAnswer(this::successMergeAnswer).given(mbocGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(mdmGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(pppGrpcService).merge(any(), any());
        b2bOffersCopier.generate("3", new CopyOffersParams(99, 98, partnerId));
        verify(dataCampMigrationClient).searchBusinessOffers(refEq(SearchBusinessOffersRequest.builder()
                .setBusinessId(99L)
                .setPartnerId(partnerId)
                .setPageRequest(SeekSliceRequest.firstNAfter(1, "1000"))
                .setIsFull(true)
                .build()));
        verify(dataCampMigrationClient, times(2)).searchBusinessOffers(any());
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(98L, partnerId,
                List.of(offerBuilder(98, (int) partnerId, "1001").build())
        );
        verify(mbocGrpcService).merge(any(), any());
        verify(mdmGrpcService, times(0)).merge(any(), any());
        verify(pppGrpcService, times(0)).merge(any(), any());
    }

    @Test
    @DisplayName("У репорта нету CopierState в БД, новая таска стартует с первой страницы.")
    @DbUnitDataSet(
            before = "B2BOffersCopierTest.noLastState.before.csv",
            after = "B2BOffersCopierTest.fail1.after.csv"
    )
    void reportWithNoLastState_startsFromFirstPage() {
        checkTwoPagesFor("2", 101);
    }

    @Test
    @DisplayName("У CopierState репорта из БД нету 'lastKey', новая таска стартует с первой страницы.")
    @DbUnitDataSet(
            before = "B2BOffersCopierTest.noLastKey.before.csv",
            after = "B2BOffersCopierTest.fail1.after.csv"
    )
    void reportWithNoLastKey_startsFromFirstPage() {
        checkTwoPagesFor("2", 101);
    }

    @Test
    @DisplayName("У репорта не десериализовывается CopierState из БД, новая таска стартует с первой страницы.")
    @DbUnitDataSet(
            before = "B2BOffersCopierTest.invalidLastState.before.csv",
            after = "B2BOffersCopierTest.fail1.after.csv"
    )
    void reportWithInvalidLastState_startsFromFirstPage() {
        checkTwoPagesFor("2", 101);
    }

    /**
     * В сервисе 2 оферра. В целевом бизнесе есть оба sku, но один из них с другим маппингом (ssku 1001).
     * Тест проверяет, что офер с конфликтующим маппингом записывается со скрытием.
     */
    @Test
    void testConflict() {
        environmentService.setValue("business.migration.batch.size", "10");
        UnitedOfferBuilder.clock = clock;
        long partnerId = 101L;
        mockDataCamp(99, partnerId, 3, new DataCampMockRequest(null, List.of()),
                DataCampMockResponse.ofOffers(null, List.of(
                        offerBuilder(99, (int) partnerId, "1000")
                                .withMapping(10000)
                                .withName("test1").build(),
                        offerBuilder(99, (int) partnerId, "1001")
                                .withMapping(10001)
                                .withName("strange-name")
                                .build(),
                        offerBuilder(99, (int) partnerId, "1002")
                                .withMapping(10002)
                                .withName("good-name")
                                .build()))
        );
        mockDataCamp(98, partnerId, 3, new DataCampMockRequest(null, List.of("1000", "1001", "1002")),
                DataCampMockResponse.ofOffers(null, List.of(
                        // Другой marketsku в матчинге, проверяем название товара
                        offerBuilder(98, null, "1000").withUcMapping(20000).withName("test2").build(),
                        // Одинаковые marketsku, до проверки названия дело не дойдет
                        offerBuilder(98, null, "1001").withMapping(10001).withName("non-conflicted-name").build(),
                        // Другой marketsku в матчинге, но название то же. Так можно, но берем маппинг
                        offerBuilder(99, null, "1002").withUcMapping(20002).withName("good-name").build())));

        doReturn(OffersBatch.UnitedOffersBatchResponse.newBuilder().build())
                .when(dataCampMigrationClient).changeBusinessUnitedOffers(any(), any(), any());
        willAnswer(this::successMergeAnswer).given(mbocGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(mdmGrpcService).merge(any(), any());
        willAnswer(this::successMergeAnswer).given(pppGrpcService).merge(any(), any());
        List<OfferInfo> capturedOffers = new ArrayList<>();

        var result = b2bOffersCopier.generate("1", new CopyOffersParams(99, 98, partnerId));

        //noinspection unchecked
        ArgumentCaptor<List<DataCampUnitedOffer.UnitedOffer>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataCampMigrationClient).changeBusinessUnitedOffers(eq(98L), eq(partnerId), captor.capture());

        var actual = captor.getValue();
        Assertions.assertAll(
                () -> assertThat(actual.get(0),
                        equalTo(offerBuilder(98, (int) partnerId, "1000")
                                .withUcMapping(20000)
                                .withName("test2")
                                .withBasicVerdict("sourceName", "test1", "targetName", "test2")
                                .build())),
                () -> assertThat(actual.get(1),
                        equalTo(offerBuilder(98, (int) partnerId, "1001")
                                .withMapping(10001)
                                .withName("non-conflicted-name")
                                .build())),
                () -> assertThat(actual.get(2),
                        equalTo(offerBuilder(98, (int) partnerId, "1002")
                                .withMapping(10002)
                                .withName("good-name")
                                .build()))
        );
        verify(mbocGrpcService).merge(any(), any());
        verify(mdmGrpcService, times(0)).merge(any(), any());
        verify(pppGrpcService, times(0)).merge(any(), any());

        assertEquals(ReportResult.done(null, "Report done. Processed objects: 3"), result);
    }

    @Test
    @DisplayName("Если партнер не {синий, белый} -> ReportState.FAILED")
    @DbUnitDataSet(before = "B2BOffersCopier.generateReport.fail.before.csv")
    void generateReport_partnerIsNeitherBlueNorWhite_fail() {
        ReportResult result = b2bOffersCopier.generate("3", new CopyOffersParams(99, 98, 103));
        assertEquals(result.getNewState(), ReportState.FAILED);
        assertNotNull(result.getReportGenerationInfo().getDescription());
    }

}
