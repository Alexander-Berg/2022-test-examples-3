package ru.yandex.market.billing.payment.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.billing.payment.services.AccrualTrantimesProcessingService.ENV_ACCRUAL_ORIGINAL_TABLE
import ru.yandex.market.billing.service.environment.EnvironmentService
import ru.yandex.market.common.test.db.DbUnitDataSet

class AccrualTrantimesProcessingServiceTest : FunctionalTest() {
    @Autowired
    private lateinit var accrualTrantimesProcessingService: AccrualTrantimesProcessingService
    @Autowired
    private lateinit var environmentService: EnvironmentService

    @DbUnitDataSet(
        before = ["AccrualTrantimesProcessingServiceTest.before.csv", "TrantimesProcessingServiceTest.before.csv"],
        after = ["AccrualTrantimesProcessingServiceTest.after.csv"]
    )
    @DisplayName("Тест на создание начислений по трантаймам")
    @Test
    fun testCreateAccruals() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["AccrualTrantimesProcessingServiceTest.before.csv", "TrantimesProcessingServiceTest.before.csv"],
        after = ["AccrualTrantimesProcessingServiceTest.after.csv"]
    )
    @DisplayName("Тест на создание начислений по трантаймам с флагом is_original_table = false")
    @Test
    fun testCreateAccrualsWhenOriginalTableIsFalse() {
        environmentService.setValue(ENV_ACCRUAL_ORIGINAL_TABLE, "false");
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["AccrualTrantimesProcessingServiceTest.before.csv", "TrantimesProcessingServiceTest.before.csv"],
        after = ["AccrualTrantimesProcessingServiceTest.isOriginalTableIsTrue.after.csv"]
    )
    @DisplayName("Тест на создание начислений по трантаймам с флагом is_original_table = true")
    @Test
    fun testCreateAccrualsWhenOriginalTableIsTrue() {
        environmentService.setValue(ENV_ACCRUAL_ORIGINAL_TABLE, "true");
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testCreateAccrualsForSubsidy.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testCreateAccrualsForSubsidy.after.csv"]
    )
    @DisplayName("Начислений по трантаймам для субсидий создаются")
    @Test
    fun testDontCreateAccrualsForSubsidy() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testCreateAccrualsForPlus.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testCreateAccrualsForPlus.after.csv"]
    )
    @DisplayName("Начислений по трантаймам для плюсов создаются")
    @Test
    fun testDontCreateAccrualsForPlus() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testAccrualDeliveryPartnerForDbs.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testAccrualDeliveryPartnerForDbs.after.csv"]
    )
    @DisplayName("При доставке дсбс деньги получает партнер а не яндекс")
    @Test
    fun testAccrualDeliveryPartnerForDbs() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testDontCreateAccrualsForMbiControlNotEnabled.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testDontCreateAccrualsForMbiControlNotEnabled.after.csv"]
    )
    @DisplayName("Начисления не создаются по трантаймам с транзакциями у которых признак MbiControlEnabled = false")
    @Test
    fun testDontCreateAccrualsForMbiControlNotEnabled() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["AccrualCessionTrantimesProcessingServiceTest.before.csv"],
        after = ["AccrualCessionTrantimesProcessingServiceTest.after.csv"]
    )
    @DisplayName("Тест на создание начислений по трантаймам для переуступки")
    @Test
    fun testCreateAccrualsCession() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testSkipProblemAccruals.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testSkipProblemAccruals.after.csv"]
    )
    @DisplayName("Тест на пропуск проблемных начислений по трантаймам")
    @Test
    fun testSkipProblemAccruals() {
        val actual = assertThrows(
            IllegalArgumentException::class.java,
                accrualTrantimesProcessingService::process
            );
        if (actual is IllegalArgumentException) {
            assertEquals(
                "Can't get PaysysTypeCc for paymentMethod = EXTERNAL_CERTIFICATE paymentGoal = ORDER_POSTPAY",
                actual.message
            );
        }
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testTinkoffInstallmentsAccruals.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testTinkoffInstallmentsAccruals.after.csv"]
    )
    @DisplayName("Тест на создание начислений по рассрочкам Тинькофф")
    @Test
    fun testTinkoffInstallmentsAccruals() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testProcessAccrualRefundWithPayment.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testProcessAccrualRefundWithPayment.after.csv"]
    )
    @DisplayName("Тест, что refund, пришедший вместе с payment, обработается")
    @Test
    fun testProcessAccrualRefundWithPayment() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testProcessAccrualRefundWhenPaymentExists.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testProcessAccrualRefundWhenPaymentExists.after.csv"]
    )
    @DisplayName("Тест, что refund обработается, если в accrual есть соответствующий payment")
    @Test
    fun testProcessAccrualRefundWhenPaymentExists() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testSkipAccrualRefundWhenPaymentNotExists.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testSkipAccrualRefundWhenPaymentNotExists.after.csv"]
    )
    @DisplayName("Тест, что refund не обрабатывается, если в accrual нет соответствующего payment")
    @Test
    fun testSkipAccrualRefundWhenPaymentNotExists() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithServiceFeePartitions.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithServiceFeePartitions.after.csv"]
    )
    @DisplayName("Тест на создание начисления по refund'у с service_fee_partitions")
    @Test
    fun testRefundAccrualWithServiceFeePartitions() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithIgnoredServiceFeePartitions.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithIgnoredServiceFeePartitions.after.csv"]
    )
    @DisplayName("Тест на создание начисления по refund'у с игнорируемыми service_fee_partitions")
    @Test
    fun testRefundAccrualWithIgnoredServiceFeePartitions() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithPartialServiceFeePartitions.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithPartialServiceFeePartitions.after.csv"]
    )
    @DisplayName("Тест на создание начисления по refund'у с service_fee_partitions на часть позиций заказа")
    @Test
    fun testRefundAccrualWithPartialServiceFeePartitions() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testTwoRefundAccrualWithServiceFeePartitions.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testTwoRefundAccrualWithServiceFeePartitions.after.csv"]
    )
    @DisplayName("Тест на создание начислений по двум refund'ам с service_fee_partitions на одну позицию заказа")
    @Test
    fun testTwoRefundAccrualWithServiceFeePartitions() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testSubsidyRefundAccrualWithServiceFeePartitions.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testSubsidyRefundAccrualWithServiceFeePartitions.after.csv"]
    )
    @DisplayName("Тест на создание начисления по refund'у с service_fee_partitions при наличии субсидийного возврата")
    @Test
    fun testSubsidyRefundAccrualWithServiceFeePartitions() {
        accrualTrantimesProcessingService.process()
    }

    @Test
    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testCreateAccrualsForIgnoredPartners.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testCreateAccrualsForIgnoredPartners.after.csv"]
    )
    @DisplayName("Тест на создание accrual с payout_status = 'ignored' для  создания выплат по трантаймам для игнорируемых партнёров")
    fun testCreateAccrualsForIgnoredPartners() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithDifferentDeliveryId.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithDifferentDeliveryId.after.csv"]
    )
    @DisplayName("Тест на создание начисления по refund'у для доставки с разными delivery_id")
    @Test
    fun testRefundAccrualWithDifferentDeliveryId() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithServiceFeeAndDifferentDeliveryId.before.csv"],
        after = ["MoneyFlowProcessingServiceTest.testRefundAccrualWithServiceFeeAndDifferentDeliveryId.after.csv"]
    )
    @DisplayName("Тест на создание начисления по refund'у для доставки с service_fee и разными delivery_id")
    @Test
    fun testRefundAccrualWithServiceFeeAndDifferentDeliveryId() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["AccrualTrantimesProcessingServiceTest.testYaCard.before.csv"],
        after = ["AccrualTrantimesProcessingServiceTest.testYaCard.after.csv"]
    )
    @DisplayName("Тест на создание начислений с я. картой")
    @Test
    fun testCreateAccrualsYaCard() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["AccrualTrantimesProcessingServiceTest.testB2b.before.csv"],
        after = ["AccrualTrantimesProcessingServiceTest.testB2b.after.csv"]
    )
    @DisplayName("Тест на создание начислений с B2B оплатой (оплата счетом)")
    @Test
    fun testCreateAccrualsB2b() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["AccrualTrantimesProcessingServiceTest.test1p3pOld.before.csv"],
        after = ["AccrualTrantimesProcessingServiceTest.test1p3pOld.after.csv"]
    )
    @DisplayName("Тест на создание начислений с 1p как 3p ДО даты X")
    @Test
    fun testCreateAccrualsFor1p3pOld() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["AccrualTrantimesProcessingServiceTest.test1p3pNew.before.csv"],
        after = ["AccrualTrantimesProcessingServiceTest.test1p3pNew.after.csv"]
    )
    @DisplayName("Тест на создание начислений с 1p как 3p ПОСЛЕ даты X")
    @Test
    fun testCreateAccrualsFor1p3pNew() {
        accrualTrantimesProcessingService.process()
    }

    @DbUnitDataSet(
        before = ["AccrualTrantimesProcessingServiceTest.test1p3pPaymentOldRefundNew.before.csv"],
        after = ["AccrualTrantimesProcessingServiceTest.test1p3pPaymentOldRefundNew.after.csv"]
    )
    @DisplayName("Тест на создание начислений с 1p как 3p payment ДО даты X, а refund ПОСЛЕ")
    @Test
    fun testCreateAccrualsFor1p3pPaymentOldRefundNew() {
        accrualTrantimesProcessingService.process()
    }
}
