package ru.yandex.market.mboc.app.logisticsparams;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.app.importexcel.BackgroundImportService;
import ru.yandex.market.mboc.common.TransactionTemplateMock;
import ru.yandex.market.mboc.common.config.LogisticParamsConfig;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.SkuLogisticsParams;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.WhLogisticsParams;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundAction;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionRepositoryMock;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionService;
import ru.yandex.market.mboc.common.dict.backgroundaction.BackgroundActionServiceImpl;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.logisticsparams.enrichment.LogisticParamsEnrichmentService;
import ru.yandex.market.mboc.common.logisticsparams.repository.SkuLogisticParamsRepository;
import ru.yandex.market.mboc.common.logisticsparams.repository.WhLogisticParamsRepository;
import ru.yandex.market.mboc.common.offers.ExcelS3ServiceMock;
import ru.yandex.market.mboc.common.services.converter.LineWith;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.ThrowingConsumer;
import ru.yandex.market.mboc.common.web.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BackgroundImportLogisticParamsServiceTest {

    private static final int SUPPLIER_ID = 42;
    private static final String SSKU_FROM_FILE = "12221";
    private static final String WH_SHEET_NAME = "testWh";

    private static final String FILE_NAME = "some file.xlsx";
    private static final String DESCRIPTION = "some text";
    private static final String LOGIN = "test";

    private BackgroundActionServiceImpl backgroundActionService;
    private BackgroundImportService backgroundImportService;
    private LogisticParamsEnrichmentService enrichmentService;
    private BackgroundActionRepositoryMock actionRepository;
    private ImportLogisticParamsService importLogisticParamsService;

    @Before
    public void setup() {
        actionRepository = new BackgroundActionRepositoryMock();
        backgroundActionService = new BackgroundActionServiceImpl(
            actionRepository, new TransactionTemplateMock(), 1);
        backgroundActionService.init();
        enrichmentService = mock(LogisticParamsEnrichmentService.class);

        SupplierRepositoryMock supplierRepository = new SupplierRepositoryMock();
        supplierRepository.insert(OfferTestUtils.simpleSupplier());

        LogisticParamsConfig config = new LogisticParamsConfig();

        importLogisticParamsService = new ImportLogisticParamsService(
            config.skuParamsExcelFileConverter(),
            config.whParamsExcelFileConverter(),
            new ExcelS3ServiceMock(),
            supplierRepository,
            TransactionHelper.MOCK,
            enrichmentService,
            mock(SkuLogisticParamsRepository.class),
            mock(WhLogisticParamsRepository.class));

        backgroundImportService = new BackgroundImportService(backgroundActionService);
    }

    @After
    public void tearDown() throws InterruptedException {
        backgroundActionService.stop();
    }

    @Test
    public void testException() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/CorrectLPSample.xls");
        String exceptionMsg = "Fine exception for test purposes";

        when(enrichmentService.mergeSkuLogisticParams(anyList(), anyList(), anyString()))
            .thenAnswer(c -> {
                throw new IllegalStateException(exceptionMsg);
            });

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importLogisticParamsService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN));
        backgroundActionService.stop();

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("IllegalStateException: " + exceptionMsg);
    }

    @Test
    public void testExceptionWithoutMessage() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/CorrectLPSample.xls");

        when(enrichmentService.mergeSkuLogisticParams(anyList(), anyList(), anyString()))
            .thenAnswer(c -> {
                throw new NullPointerException();
            });

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importLogisticParamsService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN));
        backgroundActionService.stop();

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("NullPointerException: ");
    }

    @Test
    public void testError() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/BrokenLPSample.xls");

        when(enrichmentService.mergeSkuLogisticParams(anyList(), anyList(), anyString()))
            .thenAnswer(invocation -> Collections.emptyList());
        when(enrichmentService.mergeWhLogisticParams(anyList(), anyList(), anyString()))
            .thenAnswer(invocation -> Collections.emptyList());

        int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
            actionHandle -> importLogisticParamsService
                .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN));
        backgroundActionService.stop();

        BackgroundAction action = actionRepository.findById(actionId);
        assertThat(action.isActionFinished()).isTrue();
        assertThat(action.getState()).isEqualTo("Errors in import");

        Result result = (Result) action.getResult();
        assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.ERROR);
        assertThat(result.getMessage()).
            isEqualTo("Файл не содержит обязательного листа 'ЛП Софьино'\n" +
                "Файл не содержит обязательного листа 'ЛП Томилино'\n" +
                "Файл не содержит обязательного листа 'ЛП Ростов (прямая)'\n" +
                "Файл не содержит обязательного листа 'ЛП КД Ростов'");
    }

    @Test
    public void testCorrect() throws IOException, InterruptedException {
        byte[] bytes = readResource("excel/CorrectLPSample.xls");

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch atEnrichment = new CountDownLatch(1);
        CountDownLatch passEnrichment = new CountDownLatch(1);
        backgroundActionService.startAction(BackgroundActionService.Context.builder().build(),
            ThrowingConsumer.rethrow(a -> start.await()));

        when(enrichmentService.mergeSkuLogisticParams(anyList(), anyList(), anyString()))
            .thenAnswer(invocation -> {
                atEnrichment.countDown();
                passEnrichment.await();
                SkuLogisticsParams logisticSkuParams = new SkuLogisticsParams();
                logisticSkuParams.setSupplierId(SUPPLIER_ID);
                logisticSkuParams.setShopSku(SSKU_FROM_FILE);
                return Collections.singletonList(new LineWith<>(0, logisticSkuParams));
            });

        when(enrichmentService.mergeWhLogisticParams(anyList(), anyList(), anyString()))
            .thenAnswer(invocation -> {
                WhLogisticsParams logisticWhParams = new WhLogisticsParams();
                logisticWhParams.setSupplierId(SUPPLIER_ID);
                logisticWhParams.setShopSku(SSKU_FROM_FILE);
                logisticWhParams.setDestination(WH_SHEET_NAME);
                return Collections.singletonList(new LineWith<>(0, logisticWhParams));
            });

        try {
            int actionId = backgroundImportService.startImportExcel(FILE_NAME, DESCRIPTION, LOGIN,
                actionHandle -> importLogisticParamsService
                    .importExcel(SUPPLIER_ID, FILE_NAME, bytes, actionHandle::updateState, LOGIN));

            // Ещё не начали:
            BackgroundAction action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState()).isNull();

            start.countDown(); // Запустили
            atEnrichment.await(); // и ждём, пока дойдёт до enrichment

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isFalse();
            assertThat(action.getState())
                .contains("Прочитано строк в листах: Параметры товара : 5, ЛП Софьино : 0, ЛП Томилино : 0, " +
                    "ЛП Ростов (прямая) : 0, ЛП Маршрут : 5, ЛП КД Ростов : 0,\n" +
                    "ошибки: ,\n" +
                    "время: ");

            passEnrichment.countDown();

            backgroundActionService.stop(); // Теперь ждём конца

            action = actionRepository.findById(actionId);
            assertThat(action.isActionFinished()).isTrue();
            assertThat(action.getState()).isEqualTo("Done");

            Result result = (Result) action.getResult();
            assertThat(result.getStatus()).isEqualTo(Result.ResultStatus.SUCCESS);
        } finally {
            // В случае ошибки может и не отпустить.
            start.countDown();
            atEnrichment.countDown();
            passEnrichment.countDown();
        }
    }

    private byte[] readResource(String fileName) throws IOException {
        return ByteStreams.toByteArray(
            getClass().getClassLoader().getResourceAsStream(fileName));
    }
}

