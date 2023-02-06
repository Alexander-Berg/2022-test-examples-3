package ru.yandex.market.tpl.courier.presentation.feature.task.delivery

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import ru.yandex.market.tpl.courier.arch.coroutine.PresentationDispatchers
import ru.yandex.market.tpl.courier.arch.fp.nonEmptySetOf
import ru.yandex.market.tpl.courier.arch.fp.requireNonEmptySet
import ru.yandex.market.tpl.courier.arch.fp.requireNotEmpty
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.arch.navigation.ChainRouter
import ru.yandex.market.tpl.courier.arch.navigation.MainRouter
import ru.yandex.market.tpl.courier.data.errors.ErrorsClassifier
import ru.yandex.market.tpl.courier.domain.feature.order.orderIdTestInstance
import ru.yandex.market.tpl.courier.domain.feature.order.orderTestInstance
import ru.yandex.market.tpl.courier.domain.feature.point.routePointIdTestInstance
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.taskOrderIdTestInstance
import ru.yandex.market.tpl.courier.domain.feature.task.deliveryTaskIdTestInstance
import ru.yandex.market.tpl.courier.domain.feature.task.orderDeliveryTaskTestInstance
import ru.yandex.market.tpl.courier.presentation.feature.camera.CameraNavTarget
import ru.yandex.market.tpl.courier.presentation.feature.task.delivery.dashboard.DeliveryDashboardNavTarget
import ru.yandex.market.tpl.courier.presentation.feature.text.ResourcesStorage

class DeliveryPresenterTest {

    private val dispatchers: PresentationDispatchers = mockk {
        every { main } returns TestCoroutineDispatcher()
        every { worker } returns TestCoroutineDispatcher()
    }
    private val hub: DeliveryHub = mockk {
        every { scannedTasks } returns MutableStateFlow(null)
        every { takePhotoCloseFlow } returns MutableSharedFlow()
        every { requestAddPhotoFlow } returns MutableSharedFlow()
        every { photoSkipFlow } returns MutableSharedFlow()
        every { photoSkipCompleteFlow } returns MutableSharedFlow()
        justRun { setPhotoHandler(any()) }
    }
    private val chainRouter: ChainRouter = mockk {
        justRun { replaceStack(any()) }
    }
    private val configuration: DeliveryPresenter.Configuration = mockk {
        every { routePointId } returns routePointIdTestInstance()
    }
    private val errorsClassifier: ErrorsClassifier = mockk()
    private val useCases: DeliveryUseCases = mockk {
        coEvery { getScheduledTasksFlow() } returns flowOf(emptyList())
    }
    private val view: DeliveryView = mockk {
        justRun { showContent() }
        justRun { setForegroundProgressVisible(any()) }
        justRun { showBlockingError(any()) }
    }
    private val mainRouter: MainRouter = mockk {
        justRun { popTop() }
    }
    private val analytics: DeliveryAnalytics = mockk {
        justRun { reportDeliveryCancellationConfirmed() }
        justRun { reportDeliveryCancellationCancelled() }
    }
    private val resourcesStorage: ResourcesStorage = mockk {
        every { getString(any()) } returns "ABC"
    }
    private val presenter = DeliveryPresenter(
        dispatchers,
        hub,
        chainRouter,
        configuration,
        errorsClassifier,
        useCases,
        mainRouter,
        analytics,
        resourcesStorage,
    )

    @Test
    fun `Сразу открывает дашборд если сканнер выключен и заказ не требует фото`() = runBlockingTest {
        val tasks = nonEmptySetOf(
            orderDeliveryTaskTestInstance(
                photos = emptyList(),
                noPhotoComment = "",
                order = orderTestInstance(
                    isPhotoRequired = false,
                    paymentType = PaymentType.Prepaid,
                )
            )
        )
        every { configuration.isScannerEnabled } returns false
        val id = taskOrderIdTestInstance()
        every { configuration.tasks } returns nonEmptySetOf(id)
        coEvery { useCases.getCachedOrLoadDeliveryTasks(any()) } returns success(tasks)
        coEvery { useCases.getCachedTasksFlow(any()) } returns flowOf(tasks.toList())

        presenter.attachView(view)
        
        verify { chainRouter.replaceStack(any<DeliveryDashboardNavTarget>()) }
    }

    @Test
    fun `Скипает фотографии для заказов, доставляемых вместе с заказами с пост-оплатой`() = runBlockingTest {
        every { configuration.isScannerEnabled } returns false
        val ids = (0L..1L).map {
            taskOrderIdTestInstance(
                taskId = deliveryTaskIdTestInstance(it),
                orderId = orderIdTestInstance(it.toString().requireNotEmpty()),
            )
        }
        every { configuration.tasks } returns ids.requireNonEmptySet()
        coEvery { useCases.getCachedOrLoadDeliveryTasks(any()) } returns success(
            setOf(
                orderDeliveryTaskTestInstance(
                    id = ids[0].taskId,
                    photos = emptyList(),
                    noPhotoComment = "",
                    order = orderTestInstance(
                        isPhotoRequired = true,
                        paymentType = PaymentType.Prepaid,
                        externalOrderId = ids[0].orderId,
                    )
                ),
                orderDeliveryTaskTestInstance(
                    id = ids[1].taskId,
                    photos = emptyList(),
                    noPhotoComment = "",
                    order = orderTestInstance(
                        isPhotoRequired = false,
                        paymentType = PaymentType.Cash,
                        externalOrderId = ids[1].orderId,
                    )
                )
            ).requireNonEmptySet()
        )
        coEvery { useCases.skipOrdersPhotoWithPostPaidDeliveryOrSchedule(any(), any()) } returns success()

        presenter.attachView(view)

        coVerify {
            useCases.skipOrdersPhotoWithPostPaidDeliveryOrSchedule(
                skipTaskIds = nonEmptySetOf(ids[0]),
                postPaidOrderIds = nonEmptySetOf(ids[1].orderId),
            )
        }
    }

    @Test
    fun `Открывает экран фото если все заказы предоплачены и хотя бы один требует фото`() = runBlockingTest {
        every { configuration.isScannerEnabled } returns false
        val ids = (0L..1L).map {
            taskOrderIdTestInstance(
                taskId = deliveryTaskIdTestInstance(it),
                orderId = orderIdTestInstance(it.toString().requireNotEmpty()),
            )
        }
        every { configuration.tasks } returns ids.requireNonEmptySet()
        coEvery { useCases.getCachedOrLoadDeliveryTasks(any()) } returns success(
            setOf(
                orderDeliveryTaskTestInstance(
                    id = ids[0].taskId,
                    photos = emptyList(),
                    noPhotoComment = "",
                    order = orderTestInstance(
                        isPhotoRequired = true,
                        paymentType = PaymentType.Prepaid,
                        externalOrderId = ids[0].orderId,
                    )
                ),
                orderDeliveryTaskTestInstance(
                    id = ids[1].taskId,
                    photos = emptyList(),
                    noPhotoComment = "",
                    order = orderTestInstance(
                        isPhotoRequired = false,
                        paymentType = PaymentType.Prepaid,
                        externalOrderId = ids[1].orderId,
                    )
                )
            ).requireNonEmptySet()
        )

        presenter.attachView(view)

        verify { chainRouter.replaceStack(any<CameraNavTarget>()) }
    }
}