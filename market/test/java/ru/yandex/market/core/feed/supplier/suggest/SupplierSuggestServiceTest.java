package ru.yandex.market.core.feed.supplier.suggest;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.amazonaws.util.StringInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.AsyncReports;
import ru.yandex.market.core.asyncreport.ReportsDao;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportRequest;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.feed.assortment.model.FeedProcessingResult;
import ru.yandex.market.core.feed.exception.FeedException;
import ru.yandex.market.core.feed.supplier.model.FeedSuggestInfo;
import ru.yandex.market.core.offer.mapping.AvailabilityStatus;
import ru.yandex.market.core.offer.mapping.OfferProcessingStatus;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Тесты на логику работы {@link SupplierSuggestService}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "SupplierSuggestServiceTest.csv")
class SupplierSuggestServiceTest extends FunctionalTest {

    private static final long SUPPLIER_ID = 774L;

    @Autowired
    private SupplierSuggestService suggestService;

    @Autowired
    @Qualifier(value = "marketReportService")
    private AsyncMarketReportService marketReportService;

    @Autowired
    private AsyncReports<ReportsType> asyncReportsService;

    @Autowired
    private ReportsDao reportsDao;

    @BeforeEach
    void initMock() {
        when(marketReportService.async(Mockito.any(), Mockito.any()))
                .then(invocation -> {
                    LiteInputStreamParser<?> parser = invocation.getArgument(1);
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    Object result = parser == null ? null : parser.parse(new StringInputStream("{\"results\":[]}"));
                    future.complete(result);
                    return future;
                });

    }

    /**
     * Тест на {@link SupplierSuggestService#getFeedSuggestInfo(long) получение информации} о результате завершения
     * процеса обогащения (саджеста) ассортимента поставщика.
     */
    @Test
    @DbUnitDataSet(before = "testGetSuggestInfo.csv")
    void testGetSuggestInfo() {
        try {
            suggestService.getFeedSuggestInfo(200L);
            Assertions.fail("Ожидалось, что выбросится FeedException");
        } catch (FeedException actual) {
            ReflectionAssert.assertReflectionEquals(
                    new FeedException("SUGGEST_ID", "Can not find suggest info by suggest id: 200"),
                    actual
            );
        }
        FeedSuggestInfo.Builder expectedBuilder = new FeedSuggestInfo.Builder()
                .setUploadId(10L)
                .setSupplierId(SUPPLIER_ID)
                .setFeedSuggestResult(FeedProcessingResult.WARNING)
                .setTotalOffers(10L)
                .setProcessedOffers(7L)
                .setDeclinedOffers(3L)
                .setSuggestId(10L)
                .setSuggestUploadId(11L);
        suggestService.requireSuggestIdCorrespondsSupplierId(10L, SUPPLIER_ID);
        FeedSuggestInfo actual = suggestService.getFeedSuggestInfo(10L);
        expectedBuilder.setRequestedAt(actual.getRequestedAt());
        ReflectionAssert.assertReflectionEquals(expectedBuilder.build(), actual);
    }

    @Test
    void testReportGenerationRegistration() {
        asyncReportsService.requestReportGeneration(ReportRequest.<ReportsType>builder()
                .setEntityId(774L)
                .setReportType(ReportsType.ASSORTMENT)
                .setEntityName(ReportsType.ASSORTMENT.getEntityName())
                .addParam("partnerId", 774L)
                .addParam("offerStatuses", Collections.singletonList(OfferProcessingStatus.IN_WORK))
                .addParam("availabilityStatuses", Collections.singletonList(AvailabilityStatus.ACTIVE))
                .addParam("useSuggesting", true)
                .addParam("suggestId", 41595L)
                .build());

        ReportInfo<ReportsType> reportInfo = reportsDao.getPendingReportWithLock(
                Collections.singleton(ReportsType.ASSORTMENT));
        Assertions.assertNotNull(reportInfo);
        Assertions.assertEquals("777", reportInfo.getId());
        Assertions.assertEquals(774L, reportInfo.getReportRequest().getEntityId());
        Assertions.assertEquals(ReportsType.ASSORTMENT, reportInfo.getReportRequest().getReportType());
        Assertions.assertIterableEquals(Collections.singletonList("ACTIVE"),
                (Iterable<?>) reportInfo.getReportRequest().getParams().get("availabilityStatuses"));
        Assertions.assertIterableEquals(Collections.singletonList("IN_WORK"),
                (Iterable<?>) reportInfo.getReportRequest().getParams().get("offerStatuses"));

        Assertions.assertEquals(true, reportInfo.getReportRequest().getParams().get("useSuggesting"));
        Assertions.assertEquals(41595, reportInfo.getReportRequest().getParams().get("suggestId"));
    }
}
