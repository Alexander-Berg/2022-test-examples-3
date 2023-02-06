package ru.yandex.market.billing.payment.services;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тест для {@link PaymentOrderDraftService} с TplDraftDao внутри
 */
public class TplPaymentOrderDraftServiceTest extends FunctionalTest {
    @Autowired
    public PaymentOrderDraftService tplPaymentOrderDraftService;

    @DisplayName("Нет драфтов и пэйаутов")
    @Test
    public void testProcessEmpty() {
        tplPaymentOrderDraftService.process(LocalDate.of(2021, 8, 1));
    }

    @DisplayName("Начисления + рефанд")
    @Test
    @DbUnitDataSet(
            before = "TplPaymentOrderDraftServiceTest.testProcessToday.before.csv",
            after = "TplPaymentOrderDraftServiceTest.testProcessToday.after.csv"
    )
    public void testProcessToday() {
        tplPaymentOrderDraftService.process(LocalDate.of(2021, 8, 1));
    }

    @DisplayName("При подборе драфта учитывается платформа")
    @Test
    @DbUnitDataSet(
            before = "TplPaymentOrderDraftServiceTest.testProcessPlatform.before.csv",
            after = "TplPaymentOrderDraftServiceTest.testProcessPlatform.after.csv"
    )
    public void testProcessPlatform() {
        tplPaymentOrderDraftService.process(LocalDate.of(2021, 8, 1));
    }

    @DisplayName("При сборе драфта учитываются корректировки")
    @Test
    @DbUnitDataSet(
            before = "TplPaymentOrderDraftServiceTest.testProcessCorrections.before.csv",
            after = "TplPaymentOrderDraftServiceTest.testProcessCorrections.after.csv"
    )
    public void testProcessCorrections() {
        tplPaymentOrderDraftService.process(LocalDate.of(2021, 8, 1));
    }
}
