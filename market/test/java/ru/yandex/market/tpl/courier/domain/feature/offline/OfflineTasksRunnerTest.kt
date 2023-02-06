package ru.yandex.market.tpl.courier.domain.feature.offline

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.arch.fp.nonEmptySetOf
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.domain.feature.order.ChequeType
import ru.yandex.market.tpl.courier.domain.feature.order.CompleteOrderUseCase
import ru.yandex.market.tpl.courier.domain.feature.photo.SkipOrderPhotoUseCase
import ru.yandex.market.tpl.courier.domain.feature.photo.photoUploadReportTestInstance
import ru.yandex.market.tpl.courier.domain.feature.task.RegisterChequeUseCase
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.ConfirmOrderDeliveryPaymentUseCase
import ru.yandex.market.tpl.courier.domain.feature.task.orderDeliveryTaskTestInstance

class OfflineTasksRunnerTest {
    private val registerChequeUseCase: RegisterChequeUseCase = mockk {
        coEvery { registerCheque(any(), any(), any()) } returns success(orderDeliveryTaskTestInstance())
    }
    private val uploadPhotoUseCase: ExecuteUploadOrderPhotoOfflineTaskUseCase = mockk {
        coEvery { execute(any()) } returns success(photoUploadReportTestInstance())
    }
    private val uploadPhotosUseCase: ExecuteUploadOrderPhotosOfflineTaskUseCase = mockk {
        coEvery { execute(any()) } returns success(nonEmptySetOf(photoUploadReportTestInstance()))
    }
    private val completeOrderUseCase: CompleteOrderUseCase = mockk {
        coEvery { reportOrderCompletedAndChequePrinted(any(), any()) } returns success()
    }
    private val skipPhotoUseCase: SkipOrderPhotoUseCase = mockk {
        coEvery { skipOrderPhoto(any(), any()) } returns success()
    }
    private val confirmPaymentAndConfirmPaymentAndRegisterChequeUseCase: ru.yandex.market.tpl.courier.domain.feature.task.delivery.ConfirmPaymentAndRegisterChequeUseCase =
        mockk {
            coEvery { confirmPaymentAndRegisterCheque(any(), any()) } returns success()
        }
    private val confirmPaymentUseCase: ConfirmOrderDeliveryPaymentUseCase = mockk {
        coEvery { confirmOrderDeliveryPayment(any(), any()) } returns success()
    }
    private val uploadGroupOrderPhotosUseCase: ExecuteUploadGroupOrderPhotosOfflineTaskUseCase = mockk {
        coEvery { execute(any()) } returns success(nonEmptySetOf(photoUploadReportTestInstance()))
    }
    private val runner = OfflineTasksRunner(
        registerChequeUseCase,
        uploadPhotoUseCase,
        uploadPhotosUseCase,
        completeOrderUseCase,
        skipPhotoUseCase,
        confirmPaymentAndConfirmPaymentAndRegisterChequeUseCase,
        confirmPaymentUseCase,
        uploadGroupOrderPhotosUseCase,
    )

    @Test
    fun `Вызывает правильный метод для задания отправки чека`() = runBlockingTest {
        val task = offlineTask_RegisterChequeTestInstance(chequeType = ChequeType.Sell)
        runner.runRegularTask(task)

        coVerify { registerChequeUseCase.registerCheque(task.taskId, task.paymentType, task.chequeType) }
    }

    @Test
    fun `Вызывает правильный метод для задания загрузки фото`() = runBlockingTest {
        val task = offlineTask_UploadPhotoTestInstance()
        runner.runRegularTask(task)

        coVerify { uploadPhotoUseCase.execute(task) }
    }

    @Test
    fun `Вызывает правильный метод для задания завершения заказа`() = runBlockingTest {
        val task = offlineTask_CompleteOrderTestInstance()
        runner.runRegularTask(task)

        coVerify { completeOrderUseCase.reportOrderCompletedAndChequePrinted(task.taskId, task.cheque) }
    }

    @Test
    fun `Вызывает правильный метод для задания пропуска фото`() = runBlockingTest {
        val task = offlineTask_SkipPhotoTestInstance()
        runner.runRegularTask(task)

        coVerify { skipPhotoUseCase.skipOrderPhoto(task.taskId, task.comment) }
    }

    @Test
    fun `Вызывает правильный метод для задания подтверждения платежа и печати чека`() = runBlockingTest {
        val task = compositeOfflineTask_ConfirmPaymentAndRegisterChequeTestInstance()
        runner.runCompositeTask(task)

        coVerify {
            confirmPaymentAndConfirmPaymentAndRegisterChequeUseCase.confirmPaymentAndRegisterCheque(task.taskId,
                task.paymentType)
        }
    }

    @Test
    fun `Вызывает правильный метод для задания подтверждения платежа`() = runBlockingTest {
        val task = offlineTask_ConfirmPaymentTestInstance()
        runner.runRegularTask(task)

        coVerify { confirmPaymentUseCase.confirmOrderDeliveryPayment(task.taskId, task.paymentType) }
    }
}