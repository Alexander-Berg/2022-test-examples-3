package ru.yandex.market.billing.tlog.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.billing.tlog.collection.PayoutsTransactionLogCollectionService;
import ru.yandex.market.billing.tlog.config.PayoutsExpensesTransactionLogConfig;
import ru.yandex.market.billing.tlog.config.PayoutsPaymentsTransactionLogConfig;
import ru.yandex.market.billing.tlog.dao.PayoutsTransactionLogCollectionDao;
import ru.yandex.market.billing.tlog.dao.PayoutsTransactionLogDao;
import ru.yandex.market.billing.tlog.model.PayoutsTables;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.PartnerContractServiceFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author phillippko
 * Test for {@link PayoutsTransactionLogCollectionService}
 */
@ParametersAreNonnullByDefault
class PayoutsTransactionLogCollectionServiceTest extends FunctionalTest {

    //Время в UTC timezone
    private static final LocalDateTime TEST_TIME = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    private static final Clock CLOCK = Clock.fixed(TEST_TIME.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    @Autowired
    private PayoutsTransactionLogDao paymentsPayoutsTransactionLogDao;
    @Autowired
    private PayoutsTransactionLogDao expensesPayoutsTransactionLogDao;

    @Autowired
    private PayoutsTransactionLogCollectionDao payoutsTransactionLogCollectionDao;
    @Autowired
    private TransactionTemplate pgTransactionTemplate;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private PartnerContractServiceFactory contractServiceFactory;

    private PayoutsTransactionLogCollectionService payoutsPaymentsTransactionLogCollectionService;
    private PayoutsTransactionLogCollectionService payoutsExpensesTransactionLogCollectionService;

    @BeforeEach
    void init() {
        payoutsPaymentsTransactionLogCollectionService = new PayoutsTransactionLogCollectionService(
                PayoutsPaymentsTransactionLogConfig.getPayoutsPaymentsTransactionLogConfig(),
                paymentsPayoutsTransactionLogDao,
                payoutsTransactionLogCollectionDao,
                contractServiceFactory,
                environmentService,
                pgTransactionTemplate,
                PayoutsTables.PAYMENTS_PAYOUTS_TABLES,
                CLOCK
        );
        payoutsExpensesTransactionLogCollectionService = new PayoutsTransactionLogCollectionService(
                PayoutsExpensesTransactionLogConfig.getPayoutsExpensesTransactionLogConfig(),
                expensesPayoutsTransactionLogDao,
                payoutsTransactionLogCollectionDao,
                contractServiceFactory,
                environmentService,
                pgTransactionTemplate,
                PayoutsTables.EXPENSES_PAYOUTS_TABLES,
                CLOCK
        );
    }

    @Test
    @DisplayName("Сбор accrual по 610 сервису в payments_payouts_transaction_log")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testAccrual.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testPaymentsAccrual.after.csv"
    )
    void testAccrualPayments() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор accrual по 609 сервису в expenses_payouts_transaction_log")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testAccrual.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testExpensesAccrual.after.csv"
    )
    void testAccrualExpenses() {
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор accrual_correction по 610 сервису в payments_payouts_transaction_log")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testAccrualCorrections.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testPaymentsAccrualCorrections.after.csv"
    )
    void testAccrualCorrectionsPayments() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор accrual_correction по 609 сервису в expenses_payouts_transaction_log")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testAccrualCorrections.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testExpensesAccrualCorrections.after.csv"
    )
    void testAccrualCorrectionsExpenses() {
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор payment_order по 610 сервису в payments_payouts_transaction_log")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testPaymentsOrders.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testPaymentsPaymentsOrders.after.csv"
    )
    void testPaymentOrdersPayments() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор payment_order по 609 сервису в expenses_payouts_transaction_log")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testPaymentsOrders.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testExpensesPaymentsOrders.after.csv"
    )
    void testPaymentOrdersExpenses() {
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор accrual по 610 сервису в payments_payouts_transaction_log с только INCOME контрактом")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testAccrualContract.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testPaymentsAccrualContract.after.csv"
    )
    void testAccrualPaymentsContract() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор payment_order по 610 сервису в payments_payouts_transaction_log для 1p поставщика")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testPaymentOrdersPayments1pSupplier.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testPaymentOrdersPayments1pSupplier.after.csv"
    )
    void testPaymentOrdersPayments1pSupplier() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор payment_order по 609 сервису в expenses_payouts_transaction_log для 1p поставщика")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testPaymentOrdersExpenses1pSupplier.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testPaymentOrdersExpenses1pSupplier.after.csv"
    )
    void testPaymentOrdersExpenses1pSupplier() {
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор accrual для различных типов orgId")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testAccrualOrgId.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testAccrualOrgId.after.csv"
    )
    void testAccrualOrgId() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор accrual_correction для различных типов orgId")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testAccrualCorrectionsOrgId.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testAccrualCorrectionsOrgId.after.csv"
    )
    void testAccrualCorrectionOrgId() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор payment_order для различных типов orgId")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testPaymentOrdersOrgId.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testPaymentOrdersOrgId.after.csv"
    )
    void testPaymentOrderOrgId() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("orgId по partnerId не найден")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testOrgIdByPartnerIdNotFound.before.csv"
    )
    void failOrgIdByPartnerIdNotFound() {
        Exception exception = assertThrows(
                RuntimeException.class,
                () -> payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables()
        );
        assertEquals(
                "Contract for partnerId 1000000 not found in table supplier_contract",
                exception.getCause().getMessage()
        );
    }

    @Test
    @DisplayName("orgId по contractId не найден")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testOrgIdByContractIdNotFound.before.csv"
    )
    void failOrgIdByContractIdNotFound() {
        Exception exception = assertThrows(
                RuntimeException.class,
                () -> payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables()
        );
        assertEquals(
                "OrgId for contractId 19 not found",
                exception.getCause().getCause().getMessage()
        );
    }

    @Test
    @DisplayName("Сбор accrual по 609 сервису в expenses_payouts_transaction_log с признаком ignore_in_oebs")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testExpensesAccrualIgnoreInOebs.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testExpensesAccrualIgnoreInOebs.after.csv"
    )
    void testAccrualExpensesIgnoreInOebs() {
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор payment_order и accrual по 609 и 610 сервису в expenses_payouts_transaction_log для поставщика" +
            " Яндекс.Станций")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testPaymentOrdersPaymentsYndxStationSupplier.before" +
                    ".csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testPaymentOrdersPaymentsYndxStationSupplier.after.csv"
    )
    void testPaymentOrdersExpensesYndxStationSupplier() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор claims_compensation")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testClamsCompensation.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testClamsCompensation.after.csv"
    )
    void testClamsCompensation() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Сбор accrual_compensation и payout_compensation")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testAutoCompensation.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testAutoCompensation.after.csv"
    )
    void testAutoCompensation() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Проверка, что учитывается фильтр платформы для payment_order")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testPlatformSpecificity.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testPlatformSpecificity.after.csv"
    )
    void testPlatformSpecificity() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Проверка, что прокидываем поля Я.Карты в тлог")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testYaCard.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testYaCard.after.csv"
    )
    void testYaCardFields() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Ignored supplier ids")
    @DbUnitDataSet(
            before = "PayoutsTransactionLogCollectionServiceTest.testIgnoredSupplierIds.before.csv",
            after = "PayoutsTransactionLogCollectionServiceTest.testIgnoredSupplierIds.after.csv"
    )
    void testIgnoredSupplierIds() {
        payoutsPaymentsTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
        payoutsExpensesTransactionLogCollectionService.collectTransactionLogItemsForAllTables();
    }
}

