package ru.yandex.market.billing.fulfillment.installment;


import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.model.billing.BillingServiceType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Test for {@link InstallmentBillingService}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InstallmentBillingServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2021_11_26 = LocalDate.of(2021, 11, 26);

    @Autowired
    private InstallmentBillingService installmentBillingService;

    @Test
    @DisplayName("Холостое обилливание нужной даты")
    @DbUnitDataSet(before = "InstallmentBillingServiceTest.empty.before.csv",
            after = "InstallmentBillingServiceTest.empty.before.csv")
    void whenGivenBillDateShouldBillThatDate() {
        installmentBillingService.bill(BillingServiceType.INSTALLMENT, DATE_2021_11_26, Set.of());
        installmentBillingService.bill(BillingServiceType.INSTALLMENT_FINE, DATE_2021_11_26, Set.of());
        installmentBillingService.bill(BillingServiceType.INSTALLMENT_CANCELLATION, DATE_2021_11_26, Set.of());
    }

    @Test
    @DisplayName("Обилливание только INSTALLMENT")
    @DbUnitDataSet(before = "InstallmentBillingServiceTest.installments.before.csv",
            after = "InstallmentBillingServiceTest.installment.after.csv")
    void whenGivenInstallmentServiceShouldBillOnlyIt() {
        installmentBillingService.bill(BillingServiceType.INSTALLMENT, DATE_2021_11_26, Set.of());
    }

    @Test
    @DisplayName("Обилливание только INSTALLMENT_FINE")
    @DbUnitDataSet(before = "InstallmentBillingServiceTest.installments.before.csv",
            after = "InstallmentBillingServiceTest.installment_fine.after.csv")
    void whenGivenInstallmentFineServiceShouldBillOnlyIt() {
        installmentBillingService.bill(BillingServiceType.INSTALLMENT_FINE, DATE_2021_11_26, Set.of());
    }

    @Test
    @DisplayName("Обилливание только INSTALLMENT_CANCELLATION")
    @DbUnitDataSet(before = "InstallmentBillingServiceTest.installments.before.csv",
            after = "InstallmentBillingServiceTest.installment_cancellation.after.csv")
    void whenGivenInstallmentCancellationServiceShouldBillOnlyIt() {
        installmentBillingService.bill(BillingServiceType.INSTALLMENT_CANCELLATION, DATE_2021_11_26, Set.of());
    }
}
