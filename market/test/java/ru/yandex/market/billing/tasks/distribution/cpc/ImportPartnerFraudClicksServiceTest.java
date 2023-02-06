package ru.yandex.market.billing.tasks.distribution.cpc;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportPartnerFraudClicksServiceTest extends FunctionalTest {

    private static final String ENV_DO_REIMPORT_FOR_TARGET_DATE =
            "market.billing.distribution.fraud_clicks.do_reimport_for_target_date";

    private static final LocalDate TEST_IMPORT_DATE = LocalDate.of(2020, Month.JANUARY, 1);

    @Mock
    private PartnerFraudClicksYtDao partnerFraudClicksYtDao;

    @Mock
    private PartnerFraudClicksDbDao partnerFraudClicksDbDao;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvironmentService environmentService;

    private ImportPartnerFraudClicksService importPartnerFraudClicksService;

    @BeforeEach
    void init() {
        importPartnerFraudClicksService = new ImportPartnerFraudClicksService(
                partnerFraudClicksYtDao,
                partnerFraudClicksDbDao,
                transactionTemplate,
                environmentService
        );
        when(partnerFraudClicksYtDao.existsYtTable(TEST_IMPORT_DATE)).thenReturn(true);

        doAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);
            callback.doInTransaction(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).execute(any());
    }

    @Test
    void testImportClicks() {
        List<String> rowIds = List.of("clickRowId1", "clickRowId2");
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);
            rowIds.forEach(consumer);
            return null;
        }).when(partnerFraudClicksYtDao).fetchClickRowIds(eq(TEST_IMPORT_DATE), any());

        importPartnerFraudClicksService.importClicks(TEST_IMPORT_DATE);

        verify(partnerFraudClicksDbDao).deleteClickRowIds(TEST_IMPORT_DATE);
        verify(partnerFraudClicksDbDao).persistClickRowIds(rowIds, TEST_IMPORT_DATE);
    }

    @Test
    void testBatchImportClicks() {
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);
            IntStream.range(0, 1200).mapToObj(i -> "clickRowId").forEach(consumer);
            return null;
        }).when(partnerFraudClicksYtDao).fetchClickRowIds(eq(TEST_IMPORT_DATE), any());

        importPartnerFraudClicksService.importClicks(TEST_IMPORT_DATE);

        verify(partnerFraudClicksDbDao, times(1)).deleteClickRowIds(TEST_IMPORT_DATE);
        verify(partnerFraudClicksDbDao, times(3)).persistClickRowIds(anyList(), eq(TEST_IMPORT_DATE));
    }

    @Test
    void testAlreadyImportedClicks() {
        List<String> rowIds = List.of("clickRowId1", "clickRowId2");
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);
            rowIds.forEach(consumer);
            return null;
        }).when(partnerFraudClicksYtDao).fetchClickRowIds(eq(TEST_IMPORT_DATE), any());

        when(partnerFraudClicksDbDao.getLastInsertDate()).thenReturn(TEST_IMPORT_DATE);

        importPartnerFraudClicksService.importClicks(TEST_IMPORT_DATE);

        verify(partnerFraudClicksDbDao, times(0)).deleteClickRowIds(TEST_IMPORT_DATE);
        verify(partnerFraudClicksDbDao, times(0)).persistClickRowIds(rowIds, TEST_IMPORT_DATE);
    }

    @Test
    void testReimportTargetDateForAlreadyImportedClicks() {
        List<String> rowIds = List.of("clickRowId1", "clickRowId2");
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(1);
            rowIds.forEach(consumer);
            return null;
        }).when(partnerFraudClicksYtDao).fetchClickRowIds(eq(TEST_IMPORT_DATE), any());

        when(partnerFraudClicksDbDao.getLastInsertDate()).thenReturn(TEST_IMPORT_DATE);
        environmentService.setValue(ENV_DO_REIMPORT_FOR_TARGET_DATE, "true");

        importPartnerFraudClicksService.importClicks(TEST_IMPORT_DATE);

        verify(partnerFraudClicksDbDao).deleteClickRowIds(TEST_IMPORT_DATE);
        verify(partnerFraudClicksDbDao).persistClickRowIds(rowIds, TEST_IMPORT_DATE);
    }
}
