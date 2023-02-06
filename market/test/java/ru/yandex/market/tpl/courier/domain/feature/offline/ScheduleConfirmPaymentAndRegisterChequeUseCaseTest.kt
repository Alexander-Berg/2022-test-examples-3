package ru.yandex.market.tpl.courier.domain.feature.offline

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.domain.feature.order.ChequeType
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.taskOrderIdTestInstance

class ScheduleConfirmPaymentAndRegisterChequeUseCaseTest {

    private val scheduleConfirmPaymentUseCase: ScheduleConfirmPaymentUseCase = mockk()
    private val scheduleRegisterChequeUseCase: ScheduleRegisterChequeTaskUseCase = mockk()
    private val useCase = ScheduleConfirmPaymentAndRegisterChequeUseCase(
        scheduleConfirmPaymentUseCase, scheduleRegisterChequeUseCase
    )

    @Test
    fun `Вызывает вложенные задания по-очереди`() = runBlockingTest {
        coEvery { scheduleConfirmPaymentUseCase.scheduleConfirmPaymentTask(any(), any()) } returns success()
        coEvery { scheduleRegisterChequeUseCase.scheduleRegisterChequeTask(any(), any(), any()) } returns success()

        val result = useCase.scheduleConfirmPaymentAndRegisterChequeTask(
            id = taskOrderIdTestInstance(),
            chequeType = ChequeType.Sell,
            paymentType = PaymentType.Cash,
        )

        result shouldBe success()
        coVerifyOrder {
            scheduleConfirmPaymentUseCase.scheduleConfirmPaymentTask(any(), any())
            scheduleRegisterChequeUseCase.scheduleRegisterChequeTask(any(), any(), any())
        }
    }
}