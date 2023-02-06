package ru.yandex.market.billing.fulfillment.installment

import java.lang.IllegalStateException
import java.time.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.common.test.db.DbUnitDataSet

/**
 * Тесты для [InstallmentReturnBillingService].
 */
internal class InstallmentReturnBillingServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var installmentReturnBillingService: InstallmentReturnBillingService

    @Test
    @DisplayName("Дата в прошлом")
    fun testProcessDateCannotBeUsed() {
        try {
            installmentReturnBillingService.process(LocalDate.of(2021, 12, 1))
        } catch (ex: IllegalStateException) {
            Assertions.assertTrue(ex.message == "Required date 2021-12-01 cannot be used.")
        }
    }

    @Test
    @DisplayName("Биллить нечего")
    @DbUnitDataSet(
        before = ["InstallmentReturnBillingServiceTest.testProcessNothing.before.csv"],
        after = ["InstallmentReturnBillingServiceTest.testProcessNothing.after.csv"]
    )
    fun testProcessNothing() {
        installmentReturnBillingService.process(LocalDate.of(2021, 12, 1))
    }

    @Test
    @DisplayName("Биллим с игнором по item_ids")
    @DbUnitDataSet(
        before = ["InstallmentReturnBillingServiceTest.testProcessWithIgnore.before.csv"],
        after = ["InstallmentReturnBillingServiceTest.testProcessWithIgnore.after.csv"]
    )
    fun testProcessWithIgnoreItemIds() {
        installmentReturnBillingService.process(LocalDate.of(2021, 11, 26))
    }

    @Test
    @DisplayName("Биллим без игнора по item_ids")
    @DbUnitDataSet(
        before = ["InstallmentReturnBillingServiceTest.testProcessWithoutIgnore.before.csv"],
        after = ["InstallmentReturnBillingServiceTest.testProcessWithoutIgnore.after.csv"]
    )
    fun testProcessWithoutIgnore() {
        installmentReturnBillingService.process(LocalDate.of(2021, 11, 26))
    }

    @Test
    @DisplayName("Биллим с датой возврата > 30 дней")
    @DbUnitDataSet(
        before = ["InstallmentReturnBillingServiceTest.testProcessEndOfTariff.before.csv"],
        after = ["InstallmentReturnBillingServiceTest.testProcessEndOfTariff.after.csv"]
    )
    fun testProcessEndOfTariff() {
        installmentReturnBillingService.process(LocalDate.of(2022, 1, 30))
        installmentReturnBillingService.process(LocalDate.of(2022, 1, 31))
        installmentReturnBillingService.process(LocalDate.of(2022, 2, 1))
    }
}
