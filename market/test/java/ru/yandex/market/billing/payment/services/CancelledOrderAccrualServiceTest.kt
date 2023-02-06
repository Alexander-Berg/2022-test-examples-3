package ru.yandex.market.billing.payment.services

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.common.test.db.DbUnitDataSet

class CancelledOrderAccrualServiceTest : FunctionalTest() {
    @Autowired
    private lateinit var cancelledOrderAccrualService: CancelledOrderAccrualService

    @Test
    @DisplayName("Успешно обновляем статус")
    @DbUnitDataSet(
        before = ["CancelledOrderAccrualService.testSuccessfulUpdatePayoutStatus.before.csv"],
        after = ["CancelledOrderAccrualService.testSuccessfulUpdatePayoutStatus.after.csv"]
    )
    fun testSuccessfulUpdatePayoutStatus() {
        cancelledOrderAccrualService.updateAccrualPayoutStatusForCancelledOrders()
    }

    @Test
    @DisplayName("Не меняем статус - нет отменённых заказов")
    @DbUnitDataSet(
        before = ["CancelledOrderAccrualService.testSkipCancelForNoCancelOrders.before.csv"],
        after = ["CancelledOrderAccrualService.testSkipCancelForNoCancelOrders.after.csv"]
    )
    fun testSkipCancelForNoCancelOrders() {
        cancelledOrderAccrualService.updateAccrualPayoutStatusForCancelledOrders()
    }

    @Test
    @DisplayName("Не меняем статус - есть запись в order_payout_trantime")
    @DbUnitDataSet(
        before = ["CancelledOrderAccrualService.testSkipCancelWithOrderPayoutTrantime.before.csv"],
        after = ["CancelledOrderAccrualService.testSkipCancelWithOrderPayoutTrantime.after.csv"]
    )
    fun testSkipCancelWithOrderPayoutTrantime() {
        cancelledOrderAccrualService.updateAccrualPayoutStatusForCancelledOrders()
    }
}
