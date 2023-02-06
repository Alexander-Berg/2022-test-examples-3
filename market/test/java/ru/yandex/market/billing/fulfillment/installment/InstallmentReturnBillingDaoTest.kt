package ru.yandex.market.billing.fulfillment.installment

import java.time.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.billing.fulfillment.installment.model.InstallmentReturnBilledAmount
import ru.yandex.market.billing.fulfillment.installment.model.InstallmentType
import ru.yandex.market.billing.model.billing.BillingServiceType
import ru.yandex.market.billing.core.order.model.ValueType
import ru.yandex.market.common.test.db.DbUnitDataSet

/**
 * Тесты для [InstallmentReturnBillingDao].
 */
internal class InstallmentReturnBillingDaoTest : FunctionalTest() {

    @Autowired
    private lateinit var installmentReturnBillingDao: InstallmentReturnBillingDao

    @Test
    @DisplayName("Обнулить начисления за 2021-11-26")
    @DbUnitDataSet(
        before = ["InstallmentReturnBillingDaoTest.testResetBillingDate.before.csv"],
        after = ["InstallmentReturnBillingDaoTest.testResetBillingDate.after.csv"]
    )
    fun testResetBillingDate() {
        installmentReturnBillingDao.resetBillingDate(LocalDate.of(2021, 11, 26))
    }

    @Test
    @DisplayName("Вставить и обновить обиленные значения")
    @DbUnitDataSet(
        before = ["InstallmentReturnBillingDaoTest.testPersist.before.csv"],
        after = ["InstallmentReturnBillingDaoTest.testPersist.after.csv"]
    )
    fun testPersist() {
        installmentReturnBillingDao.persist(
            listOf(
                getBilledAmount(1L, 1L, 1L, 124L),
                getBilledAmount(2L, 2L, 2L, 125L),
                getBilledAmount(3L, 3L, 2L, 126L),
                getBilledAmount(4L, 4L, 4L, 127L),
            )
        )
    }

    private fun getBilledAmount(orderId: Long, orderItemId: Long, partnerId: Long, returnItemId: Long) =
        InstallmentReturnBilledAmount(
            orderId = orderId,
            orderItemId = orderItemId,
            partnerId = partnerId,
            serviceType = BillingServiceType.INSTALLMENT_RETURN_CANCELLATION,
            installmentType = InstallmentType.INSTALLMENT_6,
            returnItemId = returnItemId,
            trantime = LocalDate.of(2021, 11, 26).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            count = partnerId.toInt(),
            tariffValue = 100,
            tariffValueType = ValueType.RELATIVE,
            rawAmount = -100L * partnerId,
            amount = -100L * partnerId
        )
}
