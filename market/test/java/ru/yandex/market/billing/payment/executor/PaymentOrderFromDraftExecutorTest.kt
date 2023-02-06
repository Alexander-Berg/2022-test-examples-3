package ru.yandex.market.billing.payment.executor

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.billing.payment.services.PaymentOrderFromDraftExecutor
import ru.yandex.market.common.test.db.DbUnitDataSet

@ExtendWith(MockitoExtension::class)
class PaymentOrderFromDraftExecutorTest : FunctionalTest() {
    @Autowired
    private val paymentOrderFromDraftExecutor: PaymentOrderFromDraftExecutor? = null

    @Test
    @DisplayName("Обработка только одного дня")
    @DbUnitDataSet(
        before = ["PaymentOrderFromDraftExecutorTest.testOneDayProcessing.before.csv"],
        after = ["PaymentOrderFromDraftExecutorTest.testOneDayProcessing.after.csv"]
    )
    fun testOneDayProcessing() {
        paymentOrderFromDraftExecutor!!.doJob()
    }

    @Test
    @DisplayName("Обычная обработка нескольких дней подряд")
    @DbUnitDataSet(
        before = ["PaymentOrderFromDraftExecutorTest.testRegularProcessing.before.csv"],
        after = ["PaymentOrderFromDraftExecutorTest.testRegularProcessing.after.csv"]
    )
    fun testRegularProcessing() {
        paymentOrderFromDraftExecutor!!.doJob()
    }

}
