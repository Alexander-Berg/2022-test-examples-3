package ru.yandex.market.billing.payment.services;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.payment.dao.PayoutDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тест для {@link PaymentOrderDraftService}
 */
class PaymentOrderDraftServiceTest extends FunctionalTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2021, 7, 21);

    @Autowired
    public PayoutDao payoutDao;
    @Autowired
    public TransactionTemplate transactionTemplate;

    @Autowired
    public PaymentOrderDraftService paymentOrderDraftService;

    @DisplayName("Все пусто")
    @Test
    public void testProcessEmpty() {
        paymentOrderDraftService.process(TEST_DATE);
    }

    @DisplayName("Одно начисление (payment), одна корректировка (payment)")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcess.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcess.after.csv"
    )
    public void testProcess() {
        paymentOrderDraftService.process(TEST_DATE);
    }

    @DisplayName("Начисление (payment) + корректировка (payment)")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessWithCorrection.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessWithCorrection.after.csv"
    )
    public void testProcessWithCorrection() {
        paymentOrderDraftService.process(TEST_DATE);
    }

    @DisplayName("Начисление (payment) + корректировка (refund)")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessWithCorrectionRefund.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessWithCorrectionRefund.after.csv"
    )
    public void testProcessWithCorrectionRefund() {
        paymentOrderDraftService.process(TEST_DATE);
    }

    @DisplayName("Черновики команд на выплату отрицательные и нулевые")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessAmountPaymentOrderDraft.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessAmountPaymentOrderDraft.after.csv"
    )
    public void testProcessAmountPaymentOrderDraft() {
        paymentOrderDraftService.process(TEST_DATE);
    }

    @DisplayName("Черновики команд на выплату уже обработаны")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessProcessedPaymentOrderDraft.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessProcessedPaymentOrderDraft.after.csv"
    )
    public void testProcessProcessedPaymentOrderDraft() {
        paymentOrderDraftService.process(TEST_DATE);
    }

    @DisplayName("Разные PaysysType")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessPaysysType.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessPaysysType.after.csv"
    )
    public void testProcessPaysysType() {
        paymentOrderDraftService.process(TEST_DATE);
    }

    @DisplayName("Нет contract_id")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessContractIdIsNull.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessContractIdIsNull.after.csv"
    )
    public void testProcessContractIdIsNull() {
        paymentOrderDraftService.process(TEST_DATE);
    }

    @DisplayName("Разные PaymentMethod")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessPaymentMethod.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessPaymentMethod.after.csv"
    )
    public void testProcessPaymentMethod() {
        paymentOrderDraftService.process(TEST_DATE);
    }

    @DisplayName("Черновик только для выплат с датой <= даты работы Process")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessFilterByDate.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessFilterByDate.after.csv"
    )
    public void testProcessFilterByDate() {
        paymentOrderDraftService.process(LocalDate.of(2021, 8, 1));
    }

    @DisplayName("Черновики для сегодняшней даты")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessToday.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessToday.after.csv"
    )
    public void testProcessToday() {
        paymentOrderDraftService.process(LocalDate.of(2021, 8, 1));
    }

    @DisplayName("Черновики для сегодняшней даты, в котором есть несколько партнеров на один и тот же договор")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessTodayManyPartnersToOneContract.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessTodayManyPartnersToOneContract.after.csv"
    )
    public void testProcessTodayManyPartnersToOneContract() {
        paymentOrderDraftService.process(LocalDate.of(2021, 8, 1));
    }

    @DisplayName("Черновики по датам")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessFilterByDates.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessFilterByDates.after.csv"
    )
    public void testProcessFilterByDates() {
        paymentOrderDraftService.process(LocalDate.of(2021, 8, 2));
    }

    @DisplayName("Начисления с YaDeliverySubsidy")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessYaDeliverySubsidy.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessYaDeliverySubsidy.after.csv"
    )
    public void testProcessYaDeliverySubsidy() {
        paymentOrderDraftService.process(LocalDate.of(2021, 7, 1));
    }

    @DisplayName("При подборе драфта учитывается платформа")
    @Test
    @DbUnitDataSet(
            before = "PaymentOrderDraftServiceTest.testProcessPlatform.before.csv",
            after = "PaymentOrderDraftServiceTest.testProcessPlatform.after.csv"
    )
    public void testProcessPlatform() {
        paymentOrderDraftService.process(LocalDate.of(2021, 8, 1));
    }
}
