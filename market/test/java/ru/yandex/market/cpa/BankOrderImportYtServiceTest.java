package ru.yandex.market.cpa;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.payment.BankOrder;
import ru.yandex.market.core.order.payment.BankOrderStatus;
import ru.yandex.market.core.order.payment.OebsPaymentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link BankOrderImportYtService}
 */
@ExtendWith(MockitoExtension.class)
class BankOrderImportYtServiceTest extends FunctionalTest {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final LocalDateTime START_IMPORT = LocalDateTime.of(2020, 11, 10, 0, 0);
    private static final LocalDateTime END_IMPORT = LocalDateTime.of(2020, 11, 11, 0, 0);

    @Value("${mbi.billing.import.xibp.yt.table.path}")
    private String xibpYtTablePath;
    @Value("${mbi.billing.import.xdps.yt.table.path}")
    private String xdpsYtTablePath;
    @Value("${mbi.billing.import.xapb.yt.table.path}")
    private String xapbYtTablePath;
    @Autowired
    private Yt yt;
    @Autowired
    private BankOrderDao bankOrderDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private BankOrderImportYtService importYtService;
    @Mock
    private Cypress cypress;

    @BeforeEach
    void initYt() {
        when(yt.cypress()).thenReturn(cypress);
    }

    @Test
    @DisplayName("Импорт при несуществующих таблицах в YT (негативный тест)")
    void testImportNotExistTables() {
        mockCypressAnswer(List.of(xibpYtTablePath, xdpsYtTablePath));
        assertThatThrownBy(() -> importYtService.importBankOrders(START_IMPORT, END_IMPORT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YT tables: " + xapbYtTablePath + "; not found");
    }

    @Test
    @DisplayName("Импорт при несуществующих таблицах (всех) в YT (негативный тест)")
    void testImportNotExistAllTables() {
        mockCypressAnswer(List.of());
        assertThatThrownBy(() -> importYtService.importBankOrders(START_IMPORT, END_IMPORT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "YT tables: " + xibpYtTablePath + "; " + xdpsYtTablePath + "; " + xapbYtTablePath + "; not found"
                );
    }

    @Test
    @DisplayName("Импорт п/п из YT")
    @DbUnitDataSet(
            before = "BankOrderImportYtServiceTest.testProcess.before.csv",
            after = "BankOrderImportYtServiceTest.testProcess.after.csv"
    )
    void testProcess() {
        BankOrderImportYtDao bankOrderImportYtDao = mockBankOrderImportYtDao(getBankOrders());
        BankOrderImportYtService bankOrderImportYtService = new BankOrderImportYtService(
                bankOrderImportYtDao, bankOrderDao, transactionTemplate
        );
        bankOrderImportYtService.importBankOrders(START_IMPORT, END_IMPORT);

        verify(bankOrderImportYtDao, Mockito.times(1)).checkTablesExist();
        verify(bankOrderImportYtDao, Mockito.times(1)).processBankOrder(any(), any(), any());
        verifyNoMoreInteractions(bankOrderImportYtDao);
    }

    @Test
    @DisplayName("Импорт п/п из YT с дубликатами")
    @DbUnitDataSet(
            before = "BankOrderImportYtServiceTest.testProcessWithDuplicates.before.csv",
            after = "BankOrderImportYtServiceTest.testProcessWithDuplicates.after.csv"
    )
    void testProcessWithDuplicates() {
        BankOrderImportYtDao bankOrderImportYtDao = mockBankOrderImportYtDao(getBankOrdersWithDuplicate());
        BankOrderImportYtService bankOrderImportYtService = new BankOrderImportYtService(
                bankOrderImportYtDao, bankOrderDao, transactionTemplate
        );
        bankOrderImportYtService.importBankOrders(START_IMPORT, END_IMPORT);

        verify(bankOrderImportYtDao, Mockito.times(1)).checkTablesExist();
        verify(bankOrderImportYtDao, Mockito.times(1)).processBankOrder(any(), any(), any());
        verifyNoMoreInteractions(bankOrderImportYtDao);
    }

    private BankOrderImportYtDao mockBankOrderImportYtDao(List<BankOrder> bankOrders) {
        BankOrderImportYtDao mockBankOrderImportYtDao = mock(BankOrderImportYtDao.class);
        doAnswer(invocation -> {
            Consumer<List<BankOrder>> consumer = invocation.getArgument(2);
            consumer.accept(bankOrders);
            return null;
        }).when(mockBankOrderImportYtDao).processBankOrder(any(LocalDateTime.class), any(LocalDateTime.class), any());
        doAnswer(invocation -> true).when(mockBankOrderImportYtDao).checkTablesExist();
        return mockBankOrderImportYtDao;
    }

    private List<BankOrder> getBankOrders() {
        return List.of(
                BankOrder.builder()
                        .setServiceId(610L)
                        .setPaymentBatchId("123")
                        .setTrantime(LocalDateTime.of(2020, 11, 10, 5, 0).atZone(ZONE_ID).toInstant())
                        .setEventtime(START_IMPORT.toLocalDate())
                        .setStatus(BankOrderStatus.DONE)
                        .setBankOrderId("456")
                        .setSum(1000L)
                        .setOebsPaymentStatus(OebsPaymentStatus.RECONCILED)
                        .build(),
                BankOrder.builder()
                        .setServiceId(609L)
                        .setPaymentBatchId("321")
                        .setTrantime(LocalDateTime.of(2020, 11, 10, 5, 0).atZone(ZONE_ID).toInstant())
                        .setEventtime(START_IMPORT.toLocalDate())
                        .setStatus(BankOrderStatus.CANCELLED)
                        .setBankOrderId("654")
                        .setSum(3000L)
                        .setOebsPaymentStatus(OebsPaymentStatus.RETURNED)
                        .build()
        );
    }

    private List<BankOrder> getBankOrdersWithDuplicate() {
        return List.of(
                BankOrder.builder()
                        .setServiceId(610L)
                        .setPaymentBatchId("123")
                        .setTrantime(LocalDateTime.of(2020, 11, 10, 5, 0).atZone(ZONE_ID).toInstant())
                        .setEventtime(START_IMPORT.toLocalDate())
                        .setStatus(BankOrderStatus.DONE)
                        .setBankOrderId("456")
                        .setSum(1000L)
                        .setOebsPaymentStatus(OebsPaymentStatus.RECONCILED)
                        .build(),
                BankOrder.builder()
                        .setServiceId(610L)
                        .setPaymentBatchId("331669895")
                        .setTrantime(LocalDateTime.of(2020, 11, 10, 5, 0).atZone(ZONE_ID).toInstant())
                        .setEventtime(START_IMPORT.toLocalDate())
                        .setStatus(BankOrderStatus.DONE)
                        .setBankOrderId("456")
                        .setSum(1000L)
                        .setOebsPaymentStatus(OebsPaymentStatus.RETURNED)
                        .build(),
                BankOrder.builder()
                        .setServiceId(610L)
                        .setPaymentBatchId("331669895")
                        .setTrantime(LocalDateTime.of(2020, 11, 10, 5, 0).atZone(ZONE_ID).toInstant())
                        .setEventtime(START_IMPORT.toLocalDate())
                        .setStatus(BankOrderStatus.DONE)
                        .setBankOrderId("654")
                        .setSum(3000L)
                        .setOebsPaymentStatus(OebsPaymentStatus.RETURNED)
                        .build(),
                BankOrder.builder()
                        .setServiceId(610L)
                        .setPaymentBatchId("331669895")
                        .setTrantime(LocalDateTime.of(2020, 11, 10, 5, 0).atZone(ZONE_ID).toInstant())
                        .setEventtime(START_IMPORT.plusDays(1).toLocalDate())
                        .setStatus(BankOrderStatus.DONE)
                        .setBankOrderId("768")
                        .setSum(3000L)
                        .setOebsPaymentStatus(OebsPaymentStatus.RETURNED)
                        .build(),
                BankOrder.builder()
                        .setServiceId(610L)
                        .setPaymentBatchId("331669895")
                        .setTrantime(LocalDateTime.of(2020, 11, 10, 5, 0).atZone(ZONE_ID).toInstant())
                        .setEventtime(START_IMPORT.plusDays(2).toLocalDate())
                        .setStatus(BankOrderStatus.DONE)
                        .setBankOrderId("890")
                        .setSum(3000L)
                        .setOebsPaymentStatus(OebsPaymentStatus.RETURNED)
                        .build()
        );
    }

    private void mockCypressAnswer(List<String> existingTables) {
        doAnswer(invocation -> {
            final YPath tablePath = invocation.getArgument(0);
            return existingTables.contains(tablePath.toString());
        }).when(cypress).exists(any(YPath.class));
    }

}
