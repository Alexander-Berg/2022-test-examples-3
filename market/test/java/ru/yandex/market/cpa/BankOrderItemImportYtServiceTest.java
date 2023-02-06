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
import ru.yandex.market.core.currency.Currency;
import ru.yandex.market.core.order.payment.BankOrderItem;
import ru.yandex.market.core.order.payment.TransactionType;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.mbi.environment.EnvironmentService;

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
 * Тесты для {@link BankOrderItemImportYtService}
 */
@ExtendWith(MockitoExtension.class)
class BankOrderItemImportYtServiceTest extends FunctionalTest {

    @Value("${mbi.billing.import.xard.yt.table.path}")
    private String xardYtTablePath;
    @Value("${mbi.billing.import.okh.yt.table.path}")
    private String okhYtTablePath;
    @Value("${mbi.billing.import.xcpta.yt.table.path}")
    private String xcptaYtTablePath;
    @Autowired
    private Yt yt;
    @Autowired
    private BankOrderDao bankOrderDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private BankOrderItemImportYtService importYtService;
    @Autowired
    private PartnerContractDao supplierContractDao;
    @Autowired
    private EnvironmentService environmentService;
    @Mock
    private Cypress cypress;

    @BeforeEach
    void initYt() {
        when(yt.cypress()).thenReturn(cypress);
    }

    private void mockCypressAnswer(List<String> existingTables) {
        doAnswer(invocation -> {
            final YPath tablePath = invocation.getArgument(0);
            return existingTables.contains(tablePath.toString());
        }).when(cypress).exists(any(YPath.class));
    }

    @Test
    @DisplayName("Пустой список для импорта")
    void testNothingImport() {
        BankOrderItemImportYtDao bankOrderItemImportYtDao = mockBankOrderItemImportYtDao();
        importYtService.importBankOrderItems();
        Mockito.verifyNoInteractions(bankOrderItemImportYtDao);
    }

    @Test
    @DisplayName("Импорт при несуществующих таблицах в YT (негативный тест)")
    @DbUnitDataSet(
            before = "BankOrderItemImportYtServiceTest.testImportBankOrderItems.before.csv",
            after = "BankOrderItemImportYtServiceTest.testImportBankOrderItems.before.csv"
    )
    void testImportNotExistTables() {
        mockCypressAnswer(List.of(xardYtTablePath, okhYtTablePath));
        assertThatThrownBy(() -> importYtService.importBankOrderItems())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("YT tables: " + xcptaYtTablePath + "; not found");
    }

    @Test
    @DisplayName("Импорт при несуществующих таблицах (всех) в YT (негативный тест)")
    @DbUnitDataSet(
            before = "BankOrderItemImportYtServiceTest.testImportBankOrderItems.before.csv",
            after = "BankOrderItemImportYtServiceTest.testImportBankOrderItems.before.csv"
    )
    void testImportNotExistAllTables() {
        mockCypressAnswer(List.of());
        assertThatThrownBy(() -> importYtService.importBankOrderItems())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "YT tables: " + xardYtTablePath + "; " + okhYtTablePath + "; " + xcptaYtTablePath + "; not found"
                );
    }

    @Test
    @DisplayName("Импорт детализации п/п из YT")
    @DbUnitDataSet(
            before = "BankOrderItemImportYtServiceTest.testImportBankOrderItems.before.csv",
            after = "BankOrderItemImportYtServiceTest.testImportBankOrderItems.after.csv"
    )
    void testImportBankOrderItems() {
        BankOrderItemImportYtDao bankOrderItemImportYtDao = mockBankOrderItemImportYtDao();
        BankOrderItemImportYtService bankOrderItemImportYtService = new BankOrderItemImportYtService(
                bankOrderItemImportYtDao, bankOrderDao, transactionTemplate, supplierContractDao, environmentService
        );
        bankOrderItemImportYtService.importBankOrderItems();

        verify(bankOrderItemImportYtDao, Mockito.times(1)).checkTablesExist();
        verify(bankOrderItemImportYtDao, Mockito.times(1)).importBankOrderItems(any(), any(), any());
        verifyNoMoreInteractions(bankOrderItemImportYtDao);
    }

    private BankOrderItemImportYtDao mockBankOrderItemImportYtDao() {
        BankOrderItemImportYtDao bankOrderItemImportYtDao = mock(BankOrderItemImportYtDao.class);
        doAnswer(invocation -> {
            Consumer<List<BankOrderItem>> consumer = invocation.getArgument(2);
            consumer.accept(getBankOrderItems());
            return null;
        }).when(bankOrderItemImportYtDao).importBankOrderItems(any(), any(), any());
        doAnswer(invocation -> true).when(bankOrderItemImportYtDao).checkTablesExist();
        return bankOrderItemImportYtDao;
    }

    private List<BankOrderItem> getBankOrderItems() {
        return List.of(
                BankOrderItem.builder()
                        .setPaymentBatchId("123")
                        .setTransactionType(TransactionType.PAYMENT)
                        .setTrustId("-1")
                        .setServiceOrderId("35624363-item-35624364")
                        .setOrderId(35624363L)
                        .setOrderItemId(35624364L)
                        .setReturnId(null)
                        .setPaymentOrderId(null)
                        .setSum(9000L)
                        .setCurrency(Currency.RUR)
                        .setPaymentType("spasibo")
                        .setHandlingTime(LocalDateTime.of(2020, 11, 10, 10, 30, 0).atZone(ZoneId.systemDefault()).toInstant())
                        .setPaymentTime(LocalDateTime.of(2020, 11, 10, 10, 30, 0).atZone(ZoneId.systemDefault()).toInstant())
                        .setContractId(123L)
                        .setPartnerId(234L)
                        .setAgencyCommission(10)
                        .build()
        );
    }

}
