package ru.yandex.market.tpl.courier.domain.feature.task.delivery

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.arch.fp.Exceptional
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.domain.feature.offline.RunOrScheduleOfflineTaskSpecification
import ru.yandex.market.tpl.courier.domain.feature.offline.ScheduleConfirmPaymentAndRegisterChequeUseCase

class ConfirmPaymentAndRegisterChequeOrScheduleUseCaseTest {

    private val specification: RunOrScheduleOfflineTaskSpecification = mockk()
    private val scheduleTaskUseCase: ScheduleConfirmPaymentAndRegisterChequeUseCase = mockk()
    private val confirmPaymentAndRegisterChequeUseCase: ConfirmPaymentAndRegisterChequeUseCase = mockk()
    private val useCase = ConfirmPaymentAndRegisterChequeOrScheduleUseCase(
        specification,
        scheduleTaskUseCase,
        confirmPaymentAndRegisterChequeUseCase,
    )

    @Test
    fun `Использует спецификацию`() = runBlockingTest {
        coEvery {
            specification.execute(any(), any(), any<suspend () -> Exceptional<Unit>>(), any())
        } returns success()

        val result = useCase.confirmPaymentAndRegisterChequeOrSchedule(
            id = taskOrderIdTestInstance(),
            paymentType = PaymentType.Cash,
        )

        result shouldBe success()
        coVerify {
            specification.execute(any(), any(), any<suspend () -> Exceptional<Unit>>(), any())
        }
    }
}