package ru.yandex.market.tpl.courier.test.cancel

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
import ru.yandex.market.tpl.courier.R
import ru.yandex.market.tpl.courier.arch.common.seconds
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.presentation.feature.screen.CancellationReasonsScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.DrawerNavigationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.FinishedTaskListScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.TaskSelectionDrawerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.checkOrderLine
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Отмена заказов")
@DisplayName("Отмена заказов")
class CancelAndReopenPartOfPrepaidMultiOrderTest: BaseTest() {

    private lateinit var multiOrderId: String
    private var orderPlaces: MutableList<String> = mutableListOf()
    private var orderIds: MutableList<String> = mutableListOf()
    private lateinit var notCancelOrderId: String
    private lateinit var notReopenOrderId: String

    override fun prepareData() {
        testDataRepository.createShift(uid)
        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = true,
            paymentType = PaymentType.Prepaid
        )
        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = true,
            paymentType = PaymentType.Prepaid
        )

        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = true,
            paymentType = PaymentType.Prepaid
        )
        val routePointId = routePoint.id

        multiOrderId = (routePoint.tasks.first() as OrderDeliveryTask).multiOrderId.unwrap()
        routePoint.tasks.forEachIndexed { index, task ->
            val order = (task as OrderDeliveryTask).order
            orderIds.add(order.externalOrderId.unwrap())

            if (index == 0) {
                notCancelOrderId = order.externalOrderId.unwrap()
            } else {
                order.places.forEach { place ->
                    orderPlaces.add(place.barcode.unwrap())
                }
                notReopenOrderId = order.externalOrderId.unwrap()
            }
        }

        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePointId, uid)
        testDataRepository.arriveAtRoutePoint(routePointId)
    }

    @Test
    @Issue("MARKETTPLAPP-1380")
    @TmsLink("courier-app-244")
    @Story("Отмена заказов")
    @DisplayName("Частичная отмена и восстановление предоплаченного мульта")
    @Description("Частичная отмена предоплаченного мультизаказа по причине 'Клиент отказался от заказа' и частичное восстановление")
    fun cancelAndReopenPartOfPrepaidMultiOrderTest() {
        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(multiOrderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
            }

            Allure.step("Проверяем заголовок с количеством посылок") {
                deliveryTitle {
                    isVisible()
                    containsText("3 товара")
                }
            }

            Allure.step("Проскролить дровер до заказа") {
                swipeUp { rootSwipeView } until {
                    drawerOrderItem { isCompletelyDisplayed() }
                }
            }

            Allure.step("Нажать на заказ в дровере") {
                drawerOrderItem { click() }
            }
        }

        Screen.onScreen<MultiOrderScreen> {
            Allure.step("Дождаться появления карточки заказа") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            Allure.step("Проскролить экран до кнопки отмены заказа") {
                swipeUp { rootSwipeView } until {
                    cancelOrderButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Нажать на кнопку отмены заказа") {
                cancelOrderButton { click() }
            }
        }

        Screen.onScreen<TaskSelectionDrawerScreen> {
            Allure.step("Проверяем, что экран отрисовался") {
                wait until {
                    container { isCompletelyDisplayed() }
                }
            }

            Allure.step("Видна кнопка отмены") {
                confirmButton {
                    isCompletelyDisplayed()
                    hasText(R.string.cancel)
                    isEnabled()
                }
            }

            Allure.step("Видны заказы и все по дефолту отмечены") {
                checkThatOrdersIsSelected(orderIds)
            }

            Allure.step("Снимаем чекбокс с первого заказа") {
                ordersLineRecyclerView.checkOrderLine(notCancelOrderId) {
                    checkbox {
                        click()
                    }
                }

                ordersLineRecyclerView.checkOrderLine(notCancelOrderId) {
                    checkbox {
                        isNotChecked()
                    }
                }
            }

            confirmButton { click() }
        }

        Screen.onScreen<CancellationReasonsScreen> {
            Allure.step("Дождаться появления списка причин отмены") {
                wait until {
                    cancellationReasonList { isDisplayed() }
                }
            }

            cancellationReasonList {
                Allure.step("Выбор причины отмены") {
                    childWith<CancellationReasonsScreen.Item> { withText("Не могу дозвониться") } perform {
                        Allure.step("Проверить наличие причины отмены \"Не могу дозвониться\"") {
                            isDisplayed()
                        }
                    }

                    childWith<CancellationReasonsScreen.Item> { withText("Клиент отказался от заказа") } perform {
                        Allure.step("Проверить наличие причины отмены \"Клиент отказался от заказа\"") {
                            isDisplayed()
                        }
                    }

                    childWith<CancellationReasonsScreen.Item> { withText("Клиент хочет доставку на другой адрес") } perform {
                        Allure.step("Проверить наличие причины отмены \"Клиент хочет доставку на другой адрес\"") {
                            isDisplayed()
                        }
                    }

                    childWith<CancellationReasonsScreen.Item> { withText("Неверные координаты") } perform {
                        Allure.step("Проверить наличие причины отмены \"Неверные координаты\"") {
                            isDisplayed()
                        }
                    }

                    childWith<CancellationReasonsScreen.Item> { withText("Заказ повреждён") } perform {
                        Allure.step("Проверить наличие причины отмены \"Заказ повреждён\"") {
                            isDisplayed()
                        }
                    }


                    childWith<CancellationReasonsScreen.Item> { withText("Клиент отказался от заказа") } perform {
                        Allure.step("Выбрать причину отмены \"Клиент отказался от заказа\"") {
                            click()
                        }
                    }
                }
            }

            Allure.step("Нажать кнопку \"Далее\"") {
                continueButton { click() }
            }

            Allure.step("Дождаться появления кнопки \"Подтвердить\"") {
                wait until {
                    confirmButton { isDisplayed() }
                }
            }

            Allure.step("Проверить отображение посылки") {
                orderPlaces.forEach { orderPlace ->
                    val order = orderContent(orderPlace)

                    order { isDisplayed() }
                }

                val reason = orderContent("Клиент отказался от заказа")
                reason { isDisplayed() }
            }

            Allure.step("Нажать кнопку \"Подтвердить\"") {
                confirmButton { click() }
            }

            Allure.step("Дождаться закрытия списка причин отмены") {
                wait until {
                    cancellationReasonList { doesNotExist() }
                }
            }
        }

        Screen.onScreen<MainScreen> {
            runBlocking { delay(10000) }

            Allure.step("Проверяем заголовок с количеством посылок") {
                deliveryTitle {
                    isVisible()
                    containsText("3 товара")
                }
            }

            Allure.step("Идем в меню") {
                wait(30.seconds) until {
                    navigationDrawerButton  { isCompletelyDisplayed() }
                }
                navigationDrawerButton { click() }
            }
        }

        Screen.onScreen<DrawerNavigationScreen> {
            Allure.step("Идем в завершенные задания") {
                wait(30.seconds) until {
                    rootSwipeView { isCompletelyDisplayed() }
                }

                finishedTaskListButton { click() }
            }
        }

        Screen.onScreen<FinishedTaskListScreen> {
            Allure.step("Дожидаемся отрисовки экрана и проваливаемся в заказ") {
                wait until {
                    tasksList { isCompletelyDisplayed() }
                }

                wait until {
                    val taskView = taskView(multiOrderId)
                    taskView { isCompletelyDisplayed() }
                }

                val taskView = taskView(multiOrderId)
                taskView { click() }
            }
        }

        Screen.onScreen<MultiOrderScreen> {
            Allure.step("Дождаться появления карточки заказа") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            Allure.step("Проскролить экран до кнопки возобновление заказа") {
                swipeUp { rootSwipeView } until {
                    reopenOrderButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Возобновляем") {
                reopenOrderButton { click() }
            }
        }

        Screen.onScreen<TaskSelectionDrawerScreen> {
            Allure.step("Проверяем, что экран отрисовался") {
                wait until {
                    container { isCompletelyDisplayed() }
                }
            }

            Allure.step("Видна кнопка возобновления") {
                confirmButton {
                    isCompletelyDisplayed()
                    hasText(R.string.reopen)
                    isEnabled()
                }
            }

            Allure.step("Видны заказы и все по дефолту отмечены") {
                checkThatOrdersIsSelected(orderIds.filter { it != notCancelOrderId })
            }

            Allure.step("Отменяем выделение с заказа") {
                ordersLineRecyclerView.checkOrderLine(notReopenOrderId) {
                    checkbox {
                        click()
                    }
                }

                ordersLineRecyclerView.checkOrderLine(notReopenOrderId) {
                    checkbox {
                        isNotChecked()
                    }
                }
            }

            confirmButton { click() }
        }

        Screen.onScreen<MainScreen> {
            runBlocking { delay(10000) }

            val drawerOrderItem = viewContainsText(multiOrderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
            }

            Allure.step("Проверяем заголовок с количеством посылок") {
                deliveryTitle {
                    isVisible()
                    containsText("3 товара")
                }
            }
        }
    }
}
