package ru.yandex.market.rg.asyncreport.finance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.FileContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.fulfillment.mds.ReportsMdsStorage;
import ru.yandex.market.rg.asyncreport.finance.model.ReconciliationReportParams;
import ru.yandex.market.rg.client.yadoc.rr.YadocRRServiceClient;
import ru.yandex.market.rg.config.FunctionalTest;
import ru.yandex.yadoc.reports.model.ReportRequest;
import ru.yandex.yadoc.reports.model.ReportStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.rg.asyncreport.finance.ReconciliationReportGenerator.MARKET_ORGANIZATION_ID;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 */
public class ReconciliationReportManualTest extends FunctionalTest {

    private static final long INCOME_CONTRACT_ID = 123L;
    private static final String YA_DOC_REPORT_ID = "yaDocReportId1";
    private static final String YA_DOC_RR_URL = "yaDocRRUrl";
    private static final String REPORT_URL = "http://example.com";
    @Autowired
    ReportsMdsStorage<ReportsType> reportsMdsStorage;
    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private YadocRRServiceClient yadocRRServiceClient;

    @Autowired
    private ReportsService<ReportsType> reportsService;


    private ReconciliationReportGenerator reconciliationReportGenerator;
    private ReportRequest yaDocReportRequest;
    private ReconciliationReportParams reconciliationReportParams;

    @BeforeEach
    void init() throws MalformedURLException {
        reconciliationReportGenerator =
                new ReconciliationReportGenerator(reportsMdsStorage, "pdf", reportsService, yadocRRServiceClient,
                        s -> new ByteArrayInputStream(s.getBytes()), () -> {
                });
        LocalDate dateFrom = LocalDate.of(2022, 5, 1);
        LocalDate dateTo = LocalDate.of(2022, 5, 31);

        reconciliationReportParams = new ReconciliationReportParams();
        reconciliationReportParams.setIncomeContractId(INCOME_CONTRACT_ID);
        reconciliationReportParams.setDateFrom(dateFrom);
        reconciliationReportParams.setDateTo(dateTo);

        yaDocReportRequest = new ReportRequest()
                .organizationId(MARKET_ORGANIZATION_ID)
                .personId(null)
                .contractId(INCOME_CONTRACT_ID)
                .email(null)
                .periodFrom(dateFrom)
                .periodTo(dateTo);

        when(yadocRRServiceClient.newReportRequest(yaDocReportRequest)).thenReturn(YA_DOC_REPORT_ID);
        when(yadocRRServiceClient.reportStatus(YA_DOC_REPORT_ID))
                .thenReturn(new ReportStatus().status(ReportStatus.StatusEnum.IN_PROGRESS))
                .thenReturn(new ReportStatus().status(ReportStatus.StatusEnum.COMPLETED));
        when(yadocRRServiceClient.reportPdf(YA_DOC_REPORT_ID)).thenReturn(YA_DOC_RR_URL);


    }

    @Test
    @DisplayName("Выгрузка отчёта при положительном развитии событий")
    @Description("Стандартная схема, когда добавляется новый отчёт и через одну итерацию запросов к YaDocRR получает " +
            "ответ")
    @DbUnitDataSet(before = "ReconciliationReportManualTest.simple.before.csv")
    void simpleTest() throws IOException {
        doReturn(new URL(REPORT_URL)).when(mdsS3Client).getUrl(any());
        ArgumentCaptor<ContentProvider> contentProviderCaptor = ArgumentCaptor.forClass(ContentProvider.class);
        doNothing().when(mdsS3Client).upload(any(), contentProviderCaptor.capture());

        reportsService.setGenerationStartedParams(YA_DOC_REPORT_ID, Instant.now(), "");
        ReportResult reportResult = reconciliationReportGenerator.generate(YA_DOC_REPORT_ID,
                reconciliationReportParams);

        Assertions.assertEquals(ReportState.DONE, reportResult.getNewState());
        Assertions.assertEquals(REPORT_URL, reportResult.getReportGenerationInfo().getUrlToDownload());
        verify(yadocRRServiceClient, times(1)).newReportRequest(yaDocReportRequest);
        verify(yadocRRServiceClient, times(2)).reportStatus(YA_DOC_REPORT_ID);
        verify(yadocRRServiceClient, times(1)).reportPdf(YA_DOC_REPORT_ID);
        ContentProvider contentProvider = contentProviderCaptor.getValue();
        Assertions.assertNotNull(contentProvider);
        Assertions.assertTrue(contentProvider instanceof FileContentProvider);
        Assertions.assertTrue(((FileContentProvider) contentProvider).getFile().getName().contains("tmp"));
        verify(mdsS3Client).upload(any(), any());
        verify(mdsS3Client).getUrl(any());
        verifyNoMoreInteractions(mdsS3Client, yadocRRServiceClient);
    }

    @Test
    @Description("Эмулируем ситуацию, когда генерация отчёта уже запускалась, был отправлен запрос на получение " +
            "отчёта в yaDoc, который сохранили в extendedState, а потом, например, машинку целиком выключили." +
            "Тогда задача должна была через некоторое время снова уйти в PENDING и попасть на разбор заново")
    @DbUnitDataSet(before = "ReconciliationReportManualTest.afterEnexpectedEnd.before.csv")
    void rerunAfterUnexpectedEndTest() throws MalformedURLException {
        ArgumentCaptor<ContentProvider> contentProviderCaptor = ArgumentCaptor.forClass(ContentProvider.class);
        doNothing().when(mdsS3Client).upload(any(), contentProviderCaptor.capture());
        doReturn(new URL(REPORT_URL)).when(mdsS3Client).getUrl(any());

        reportsService.setGenerationStartedParams(YA_DOC_REPORT_ID, Instant.now().minus(20, ChronoUnit.MINUTES), "");
        ReportResult reportResult = reconciliationReportGenerator.generate(YA_DOC_REPORT_ID,
                reconciliationReportParams);

        // checking
        Assertions.assertEquals(ReportState.DONE, reportResult.getNewState());
        Assertions.assertEquals(REPORT_URL, reportResult.getReportGenerationInfo().getUrlToDownload());
        verify(yadocRRServiceClient, times(2)).reportStatus(YA_DOC_REPORT_ID);
        verify(yadocRRServiceClient).reportPdf(YA_DOC_REPORT_ID);
        ContentProvider contentProvider = contentProviderCaptor.getValue();
        Assertions.assertNotNull(contentProvider);
        Assertions.assertTrue(contentProvider instanceof FileContentProvider);
        Assertions.assertTrue(((FileContentProvider) contentProvider).getFile().getName().contains("tmp"));
        verify(mdsS3Client).upload(any(), any());
        verify(mdsS3Client).getUrl(any());
        verifyNoMoreInteractions(mdsS3Client, yadocRRServiceClient);
    }
}
