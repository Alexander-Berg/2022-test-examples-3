package ru.yandex.market.billing.fulfillment.installment

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.billing.model.billing.BillingServiceType
import ru.yandex.market.common.test.db.DbUnitDataSet

/**
 * Тесты для [InstallmentBillingDao].
 */
internal class InstallmentBillingDaoTest : FunctionalTest() {

    @Autowired
    private lateinit var installmentBillingDao: InstallmentBillingDao

    @Test
    @DisplayName("Получить список обилленных значений рассрочки по списку itemIds и по одному serviceType")
    @DbUnitDataSet(before = ["InstallmentBillingDaoTest.testInstallmentBilledAmount.before.csv"])
    fun testInstallmentBilledAmountByItemIdsAndServiceType() {
        val result = installmentBillingDao.getInstallmentBilledAmountByItemIdsAndServiceType(
            setOf(1L, 2L, 3L), BillingServiceType.INSTALLMENT
        )
        Assertions.assertEquals(2, result.size)
        Assertions.assertTrue(result.any { it.orderItemId == 1L })
        Assertions.assertTrue(result.any { it.orderItemId == 3L })
    }
}
