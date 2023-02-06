package ru.yandex.market.tpl.courier.test.delivery

import androidx.test.filters.LargeTest
import com.agoda.kakao.screen.Screen
import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Issue
import io.qameta.allure.kotlin.Story
import io.qameta.allure.kotlin.TmsLink
import io.qameta.allure.kotlin.junit4.DisplayName
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.yandex.market.tpl.courier.arch.common.seconds
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.arch.fp.Exceptional
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePointId
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryDashboardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.PhotoViewerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.SkipPhotoCommentScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.SkipPhotoReasonScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.TakePhotoScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.addPhotoButton
import ru.yandex.market.tpl.courier.presentation.feature.screen.orderPhoto
import ru.yandex.market.tpl.courier.test.BaseTest
import java.lang.IllegalStateException

@LargeTest
@Epic("Выдача заказов")
@DisplayName("Выдача заказов")
class SkipPhotoFlowWithDifferentReasonTest: BaseTest() {

    private lateinit var orderIds: List<String>
    private var currentOrderId: String = ""

    private fun prepareDeliveryTasks(): Exceptional<RoutePointId> {
        testDataRepository.createShift(uid)
        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000000",
            isPaid = true,
            paymentType = PaymentType.Prepaid,
            recipientNotes = "Оставить у двери",
            itemsPrice = 10000,
        )

        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = true,
            paymentType = PaymentType.Prepaid,
            recipientNotes = "Оставить у двери",
            itemsPrice = 2000,
        )

        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000002",
            isPaid = true,
            paymentType = PaymentType.Prepaid,
            recipientNotes = "Оставить у двери",
            itemsPrice = 5000,
        )

        val rotePointId = routePoint.id
        val tasks = routePoint.tasks.toList() as List<OrderDeliveryTask>

        if (tasks.isEmpty())
            throw IllegalStateException("Нет заданий на точке, хотя мы их создавали!")

        if (tasks.size != 3)
            throw IllegalStateException("Нет заданий на точке, хотя мы их создавали!")

        orderIds = tasks.map { it.orderId.unwrap() }

        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000005",
            isPaid = false,
            paymentType = PaymentType.CreditCard
        )

        return success(rotePointId)
    }

    override fun prepareData() {
        val routePointId = prepareDeliveryTasks().orThrow()

        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePointId, uid)
        testDataRepository.arriveAtRoutePoint(routePointId)
    }

    @Test
    @Issue("MARKETTPLAPP-1368")
    @TmsLink("courier-app-231")
    @Story("Выдача заказов")
    @DisplayName("Добавление, удаление и пропуск фото в заказе\n")
    @Description("Выдача одиночных заказов на сумму менее 50 000р с опцией 'Оставить у двери' без фотографий")
    fun skipPhotoFlowWithDifferentReasonTest() {
        Screen.onScreen<MainScreen> {
            val firstOrderView = orderCard(orderIds.first())

            Allure.step("Дождаться появление заказов в дровере") {
                wait until {
                    firstOrderView { isVisible() }
                }
            }

            orderIds.forEach { orderId ->
                val orderView = orderCard(orderId)

                Allure.step("Проверяем заказ в дровере") {
                    orderView { isVisible() }
                }

                Allure.step("Проверить наличие тега 'Оставить у двери' на превьюшке") {
                    orderView {
                        hasDescendant {
                            containsText(IS_NEAR_THE_DOOR_TAG)
                        }
                    }
                }
            }
        }

        startOrderFlow(0)

        scanOrderAndDeletePhoto()

        Screen.onScreen<SkipPhotoReasonScreen> {
            Allure.step("Выбираем причину 'Отдал посылку в руки' и подтверждаем") {
                gaveToClientButton { click() }
                wait until {
                    confirmButton { isEnabled() }
                }

                confirmButton { click() }
            }
        }

        completeOrderFlow()

        runBlocking { delay(3000) }

        startOrderFlow(1)

        scanOrderAndDeletePhoto()

        Screen.onScreen<SkipPhotoReasonScreen> {
            Allure.step("Выбираем причину 'Не работает камера' и подтверждаем") {
                cameraDoesNotWorkButton { click() }
                wait until {
                    confirmButton { isEnabled() }
                }

                confirmButton { click() }
            }
        }

        completeOrderFlow()

        runBlocking { delay(3000) }

        startOrderFlow(2)

        scanOrderAndDeletePhoto()

        Screen.onScreen<SkipPhotoReasonScreen> {
            Allure.step("Выбираем причину 'Другое' и подтверждаем") {
                otherReasonButton { click() }
                wait until {
                    confirmButton { isEnabled() }
                }

                confirmButton { click() }
            }
        }

        Screen.onScreen<SkipPhotoCommentScreen> {
            Allure.step("Дожидаемся отрисовки экрана с комментарием") {
                wait until {
                    commentRootView { isVisible() }
                }

                commentInputLayout { isCompletelyDisplayed() }
                confirmButton {
                    isCompletelyDisplayed()
                    isDisabled()
                }
            }

            Allure.step("Вводим комментарий и подтверждаем причину") {
                commentInputLayout.typeText("comment_for_courier-app-231")

                confirmButton {
                    isEnabled()
                    click()
                }
            }
        }

        completeOrderFlow()
    }

    private fun scanOrderAndDeletePhoto() {
        Screen.onScreen<MultiOrderScreen> {
            Allure.step("Дождаться появления карточки заказа") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            Allure.step("Проверить наличие тега 'Оставить у двери' в карточке") {
                rootSwipeView {
                    hasDescendant {
                        containsText(IS_NEAR_THE_DOOR_TAG)
                    }
                }
            }

            Allure.step("Проскролить экран до кнопки выдачи заказа") {
                swipeUp { rootSwipeView } until {
                    nearTheDoorButton { isCompletelyDisplayed() }
                }

                Allure.step("Проверить, что на кнопке текст 'Оставить у двери'") {
                    nearTheDoorButton {
                        hasDescendant {
                            containsText(IS_NEAR_THE_DOOR_TAG)
                        }
                    }
                }

                Allure.step("Нажать на кнопку выдачи заказа") {
                    nearTheDoorButton { click() }
                }
            }
        }

        Screen.onScreen<DeliveryScanScreen> {
            waitUntilDrawerAndManualInputButtonIsDisplayed()

            openDrawerAndWaitUntilOpened()

            checkThatOrdersIsNotScanned(orderIds = listOf(currentOrderId))

            closeDrawerAndWaitUntilClosed()

            manuallyScanOrderId(currentOrderId)
        }

        Screen.onScreen<TakePhotoScreen> {
            Allure.step("Дождаться появления экрана добавления фотографии") {
                wait until {
                    takePhotoButton { isVisible() }
                }
            }

            Allure.step("Проверить наличие всех кнопок на экране") {
                takePhotoButton { isCompletelyDisplayed() }
                extraButton { isCompletelyDisplayed() }
                galleryButton { isCompletelyDisplayed() }
                flashButton { isCompletelyDisplayed() }
            }

            Allure.step("Делаем фотку") {
                takePhotoButton { click() }
            }
        }

        Screen.onScreen<DeliveryDashboardScreen> {
            Allure.step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }
            }

            Allure.step("Кнопки \"Добавить фото\", \"Подтвердить выдачу\", \"Закрыть\" (крестик) и карточка заказа присутствуют на экране") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                heresyIcon {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                contentRecyclerView.addPhotoButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }
            }

            Allure.step("На экране есть фотка и возможность удалить ее") {
                contentRecyclerView.orderPhoto {
                    isCompletelyDisplayed()

                    photo { isCompletelyDisplayed() }
                    closeButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Проваливаемся в фотку") {
                contentRecyclerView.orderPhoto {
                    photo { click() }
                }
            }
        }

        Screen.onScreen<PhotoViewerScreen> {
            Allure.step("Дождаться открытия экрана просмотра фото") {
                wait until {
                    viewPager {
                        isCompletelyDisplayed()
                    }
                }
            }

            Allure.step("Кнопки удаления нет, нет таба с фотками") {
                tabLayout { isGone() }

                heresyIcon { doesNotExist() }

                backIcon { isCompletelyDisplayed() }
            }

            Allure.step("Возвращаемся на дашборд") {
                backIcon { click() }
            }
        }

        Screen.onScreen<DeliveryDashboardScreen> {
            Allure.step("Нажимаем удалить фото") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }

                contentRecyclerView.orderPhoto {
                    isCompletelyDisplayed()

                    closeButton { click() }
                }
            }

            Allure.step("Проверяем открытие диалога и подтверждаем удаление") {
                wait until {
                    confirmDeletePhotoButton {
                        isCompletelyDisplayed()
                    }
                    cancelDeletePhotoButton {
                        isCompletelyDisplayed()
                    }
                }

                confirmDeletePhotoButton { click() }
            }
        }

        Screen.onScreen<TakePhotoScreen> {
            Allure.step("Дождаться появления экрана добавления фотографии") {
                wait until {
                    takePhotoButton { isVisible() }
                }
            }

            Allure.step("Выбираем 'Пропустить'") {
                extraButton {
                    isCompletelyDisplayed()
                    click()
                }
            }
        }

        Screen.onScreen<SkipPhotoReasonScreen> {
            Allure.step("Дождаться появления экрана списка причин для пропуска фото") {
                wait until {
                    skipPhotoReasonContentContainer {
                        isDisplayed()
                    }
                }
            }

            Allure.step("Проверяем отрисовку всех причин, а также кнопок подтверждения и закрытия") {
                closeButton { isCompletelyDisplayed() }
                confirmButton {
                    isCompletelyDisplayed()
                    isDisabled()
                }
                gaveToClientButton { isCompletelyDisplayed() }
                cameraDoesNotWorkButton { isCompletelyDisplayed() }
                otherReasonButton { isCompletelyDisplayed() }
            }
        }
    }

    private fun startOrderFlow(index: Int) {
        val count = index + 1

        Screen.onScreen<MainScreen> {
            Allure.step("Скроллим и тапаем $count заказ") {
                currentOrderId = orderIds[index]
                val orderView = orderCard(currentOrderId)

                Allure.step("Проскролить дровер до заказа") {
                    swipeUp { rootSwipeView } until {
                        orderView { isCompletelyDisplayed() }
                    }
                }

                Allure.step("Нажать на заказ в дровере") {
                    orderView { click() }
                }
            }
        }
    }

    private fun completeOrderFlow() {
        Screen.onScreen<DeliveryDashboardScreen> {
            Allure.step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }
            }

            Allure.step("Нажать на кнопку \"Подтвердить выдачу\"") {
                confirmDeliveryButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            Allure.step("Дождаться, когда заказ пропадет из bottom sheet'а") {
                wait until {
                    viewContainsText(currentOrderId) perform { doesNotExist() }
                }
            }
        }
    }

    companion object {
        private const val IS_NEAR_THE_DOOR_TAG = "Оставить у двери"
    }
}