package ru.yandex.market.billing.payout.control;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.payout.control.dao.AccrualDao;
import ru.yandex.market.billing.payout.control.dao.PaymentDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MoneyFlowProcessingServiceTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    PaymentDao paymentDao;

    @Autowired
    TransactionProcessService transactionProcessService;

    @Autowired
    private EnvironmentService environmentService;

    @DbUnitDataSet(
            before = {"AccrualTrantimesProcessingServiceTest.before.csv", "TrantimesProcessingServiceTest.before.csv"},
            after = "AccrualTrantimesProcessingServiceTest.after.csv"
    )
    @DisplayName("Тест на создание начислений по трантаймам")
    @Test
    void testCreateAccruals() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testCreateAccrualsForSubsidy.before.csv",
            after = "MoneyFlowProcessingServiceTest.testCreateAccrualsForSubsidy.after.csv"
    )
    @DisplayName("Начислений по трантаймам для субсидий создаются")
    @Test
    void testDontCreateAccrualsForSubsidy() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testCreateAccrualsForPlus.before.csv",
            after = "MoneyFlowProcessingServiceTest.testCreateAccrualsForPlus.after.csv"
    )
    @DisplayName("Начислений по трантаймам для плюсов создаются")
    @Test
    void testDontCreateAccrualsForPlus() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testAccrualDeliveryPartnerForDbs.before.csv",
            after = "MoneyFlowProcessingServiceTest.testAccrualDeliveryPartnerForDbs.after.csv"
    )
    @DisplayName("При доставке дсбс деньги получает партнер а не яндекс")
    @Test
    void testAccrualDeliveryPartnerForDbs() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testDontCreateAccrualsForMbiControlNotEnabled.before.csv",
            after = "MoneyFlowProcessingServiceTest.testDontCreateAccrualsForMbiControlNotEnabled.after.csv"
    )
    @DisplayName("Начисления не создаются по трантаймам с транзакциями у которых признак MbiControlEnabled = false")
    @Test
    void testDontCreateAccrualsForMbiControlNotEnabled() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "AccrualCessionTrantimesProcessingServiceTest.before.csv",
            after = "AccrualCessionTrantimesProcessingServiceTest.after.csv"
    )
    @DisplayName("Тест на создание начислений по трантаймам для переуступки")
    @Test
    void testCreateAccrualsCession() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testSkipProblemAccruals.before.csv",
            after = "MoneyFlowProcessingServiceTest.testSkipProblemAccruals.after.csv"
    )
    @DisplayName("Тест на пропуск проблемных начислений по трантаймам")
    @Test
    void testSkipProblemAccruals() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        IllegalArgumentException actual = assertThrows(
                IllegalArgumentException.class,
                accrualTrantimesProcessingService::process
        );

        assertEquals(
                "Can't get PaysysTypeCc for paymentMethod = EXTERNAL_CERTIFICATE paymentGoal = ORDER_POSTPAY",
                actual.getMessage()
        );
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testTinkoffInstallmentsAccruals.before.csv",
            after = "MoneyFlowProcessingServiceTest.testTinkoffInstallmentsAccruals.after.csv"
    )
    @DisplayName("Тест на создание начислений по рассрочкам Тинькофф")
    @Test
    void testTinkoffInstallmentsAccruals() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testProcessAccrualRefundWithPayment.before.csv",
            after = "MoneyFlowProcessingServiceTest.testProcessAccrualRefundWithPayment.after.csv"
    )
    @DisplayName("Тест, что refund, пришедший вместе с payment, обработается")
    @Test
    void testProcessAccrualRefundWithPayment() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testProcessAccrualRefundWhenPaymentExists.before.csv",
            after = "MoneyFlowProcessingServiceTest.testProcessAccrualRefundWhenPaymentExists.after.csv"
    )
    @DisplayName("Тест, что refund обработается, если в accrual есть соответствующий payment")
    @Test
    void testProcessAccrualRefundWhenPaymentExists() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testSkipAccrualRefundWhenPaymentNotExists.before.csv",
            after = "MoneyFlowProcessingServiceTest.testSkipAccrualRefundWhenPaymentNotExists.after.csv"
    )
    @DisplayName("Тест, что refund не обрабатывается, если в accrual нет соответствующего payment")
    @Test
    void testSkipAccrualRefundWhenPaymentNotExists() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testRefundAccrualWithServiceFeePartitions.before.csv",
            after = "MoneyFlowProcessingServiceTest.testRefundAccrualWithServiceFeePartitions.after.csv"
    )
    @DisplayName("Тест на создание начисления по refund'у с service_fee_partitions")
    @Test
    void testRefundAccrualWithServiceFeePartitions() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao
        );

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testRefundAccrualWithIgnoredServiceFeePartitions.before.csv",
            after = "MoneyFlowProcessingServiceTest.testRefundAccrualWithIgnoredServiceFeePartitions.after.csv"
    )
    @DisplayName("Тест на создание начисления по refund'у с игнорируемыми service_fee_partitions")
    @Test
    void testRefundAccrualWithIgnoredServiceFeePartitions() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao
        );

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testRefundAccrualWithPartialServiceFeePartitions.before.csv",
            after = "MoneyFlowProcessingServiceTest.testRefundAccrualWithPartialServiceFeePartitions.after.csv"
    )
    @DisplayName("Тест на создание начисления по refund'у с service_fee_partitions на часть позиций заказа")
    @Test
    void testRefundAccrualWithPartialServiceFeePartitions() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao
        );

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testTwoRefundAccrualWithServiceFeePartitions.before.csv",
            after = "MoneyFlowProcessingServiceTest.testTwoRefundAccrualWithServiceFeePartitions.after.csv"
    )
    @DisplayName("Тест на создание начислений по двум refund'ам с service_fee_partitions на одну позицию заказа")
    @Test
    void testTwoRefundAccrualWithServiceFeePartitions() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao
        );

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testSubsidyRefundAccrualWithServiceFeePartitions.before.csv",
            after = "MoneyFlowProcessingServiceTest.testSubsidyRefundAccrualWithServiceFeePartitions.after.csv"
    )
    @DisplayName("Тест на создание начисления по refund'у с service_fee_partitions при наличии субсидийного возврата")
    @Test
    void testSubsidyRefundAccrualWithServiceFeePartitions() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao
        );

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testCreateAccrualsWithIgnoredPartner.before.csv",
            after = "MoneyFlowProcessingServiceTest.testCreateAccrualsWithIgnoredPartner.after.csv"
    )
    @DisplayName("Тест при создании начислений игнорируемые партнёры не учитываются")
    @Test
    void testCreateAccrualsWithIgnoredPartner() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @Test
    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testCreateAccrualsForIgnoredPartners.before.csv",
            after = "MoneyFlowProcessingServiceTest.testCreateAccrualsForIgnoredPartners.after.csv"
    )
    @DisplayName("Тест на создание accrual с payout_status = 'ignored' для  создания выплат по трантаймам для игнорируемых партнёров")
    void testCreateAccrualsForIgnoredPartners() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testPaymentAccrualWithDifferentDeliveryId.before.csv",
            after = "MoneyFlowProcessingServiceTest.testPaymentAccrualWithDifferentDeliveryId.after.csv"
    )
    @DisplayName("Тест на создание начисления по payment'у для доставки с разными delivery_id")
    @Test
    void testPaymentAccrualWithDifferentDeliveryId() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao
        );

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testRefundAccrualWithDifferentDeliveryId.before.csv",
            after = "MoneyFlowProcessingServiceTest.testRefundAccrualWithDifferentDeliveryId.after.csv"
    )
    @DisplayName("Тест на создание начисления по refund'у для доставки с разными delivery_id")
    @Test
    void testRefundAccrualWithDifferentDeliveryId() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao
        );

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testRefundAccrualWithServiceFeeAndDifferentDeliveryId.before.csv",
            after = "MoneyFlowProcessingServiceTest.testRefundAccrualWithServiceFeeAndDifferentDeliveryId.after.csv"
    )
    @DisplayName("Тест на создание начисления по refund'у для доставки с service_fee и разными delivery_id")
    @Test
    void testRefundAccrualWithServiceFeeAndDifferentDeliveryId() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao
        );

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = {"AccrualTrantimesProcessingServiceTest.testYaCard.before.csv"},
            after = "AccrualTrantimesProcessingServiceTest.testYaCard.after.csv"
    )
    @DisplayName("Тест на создание начислений с я. картой")
    @Test
    void testCreateAccrualsYaCard() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = {"AccrualTrantimesProcessingServiceTest.testB2b.before.csv"},
            after = "AccrualTrantimesProcessingServiceTest.testB2b.after.csv"
    )
    @DisplayName("Тест на создание начислений с B2B оплатой (оплата счетом)")
    @Test
    void testCreateAccrualsB2b() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }

    @DbUnitDataSet(
            before = "MoneyFlowProcessingServiceTest.testCreateAccrualsInOldTable.before.csv",
            after = "MoneyFlowProcessingServiceTest.testCreateAccrualsInOldTable.after.csv"
    )
    @DisplayName("Тест на создание начислений по трантаймам в табличку accrual_old")
    @Test
    void testCreateAccrualsInOldTable() {
        AccrualDao accrualDao = new AccrualDao(namedParameterJdbcTemplate);

        AccrualTrantimesProcessingService accrualTrantimesProcessingService = new AccrualTrantimesProcessingService(
                transactionProcessService,
                environmentService,
                transactionTemplate,
                paymentDao,
                accrualDao);

        accrualTrantimesProcessingService.process();
    }
}
