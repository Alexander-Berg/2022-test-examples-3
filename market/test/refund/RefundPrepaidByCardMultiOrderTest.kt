package ru.yandex.market.tpl.courier.test.refund

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
import ru.yandex.market.tpl.courier.presentation.feature.screen.DrawerNavigationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.FinishedTaskListScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.RefundConfirmationDrawerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.RefundDashboardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.dashboardOrderCard
import ru.yandex.market.tpl.courier.presentation.feature.screen.refundDashboardOrderCard
import ru.yandex.market.tpl.courier.test.BaseTest
import java.lang.IllegalStateException

@LargeTest
@Epic("Возврат заказов")
@DisplayName("Возврат заказов")
class RefundPrepaidByCardMultiOrderTest: BaseTest() {

    private lateinit var orderIds: List<String>
    private lateinit var multiOrderId: String

    private fun prepareDeliveryTasks(): Exceptional<RoutePointId> {
        testDataRepository.createShift(uid)
        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000011",
            isPaid = true,
            paymentType = PaymentType.CreditCard,
            itemsPrice = 6000,
        )

        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000011",
            isPaid = true,
            paymentType = PaymentType.CreditCard,
            itemsPrice = 6000,
        )

        val rotePointId = routePoint.id
        val tasks = routePoint.tasks.toList() as List<OrderDeliveryTask>

        if (tasks.isEmpty())
            throw IllegalStateException("Нет заданий на точке, хотя мы их создавали!")

        orderIds = tasks.map { it.orderId.unwrap() }
        multiOrderId = tasks.first().multiOrderId.unwrap()

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
    @Issue("MARKETTPLAPP-1389")
    @TmsLink("courier-app-250")
    @Story("Возврат заказов")
    @DisplayName("Полный возврат после выдачи предоплаченного картой мульта")
    @Description("Возврат после выдачи мульта предоплаченного картой")
    fun refundPrepaidByCardMultiOrderTest() {
        Screen.onScreen<MainScreen> {
            val orderView = orderCard(multiOrderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    orderView {
                        isVisible()
                    }
                }
            }

            Allure.step("Проскролить дровер до заказа") {
                swipeUp { rootSwipeView } until {
                    orderView { isCompletelyDisplayed() }
                }
            }

            Allure.step("Нажать на заказ в дровере") {
                orderView { click() }
            }
        }

        Screen.onScreen<MultiOrderScreen> {
            Allure.step("Дождаться появления карточки заказа") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            Allure.step("Проскролить экран до кнопки выдачи заказа") {
                swipeUp { rootSwipeView } until {
                    giveOutOrderButton { isCompletelyDisplayed() }
                }

                Allure.step("Нажать на кнопку выдачи заказа") {
                    giveOutOrderButton { click() }
                }
            }
        }

        Screen.onScreen<DeliveryScanScreen> {
            waitUntilDrawerAndManualInputButtonIsDisplayed()

            openDrawerAndWaitUntilOpened()

            checkThatOrdersIsNotScanned(orderIds)

            closeDrawerAndWaitUntilClosed()

            manuallyScanOrderId(orderIds.first())

            waitUntilDrawerAndManualInputButtonIsDisplayed("Дождаться закрытия экрана ручного ввода и появления дровера")

            openDrawerAndWaitUntilOpened()

            checkThatCompleteButtonIsEnabled()

            checkThatOrderIsScanned(orderIds.first())

            closeDrawerAndWaitUntilClosed()

            manuallyScanOrderId(orderIds.last())
        }

        Screen.onScreen<DeliveryDashboardScreen> {
            Allure.step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }
            }

            Allure.step("Кнопки \"Подтвердить выдачу\", \"Закрыть\" (крестик) и карточки заказов присутствуют на экране") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                heresyIcon {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                contentRecyclerView {
                    orderIds.forEach {
                        dashboardOrderCard(it) {
                            isDisplayed()
                        }
                    }
                }
            }

            Allure.step("Нажать на кнопку \"Подтвердить выдачу\"") {
                confirmDeliveryButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            Allure.step("Дождаться, когда задание пропадет из bottom sheet'а") {
                wait until {
                    viewContainsText(multiOrderId) perform { doesNotExist() }
                }
            }

            runBlocking { delay(5000) }

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

                val taskView = taskView(multiOrderId)

                wait until {
                    taskView { isCompletelyDisplayed() }
                }

                taskView { click() }
            }
        }

        Screen.onScreen<MultiOrderScreen> {
            Allure.step("Дождаться появления карточки заказа") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            Allure.step("Проскролить экран до кнопки возврата заказа и клацнуть") {
                swipeUp { rootSwipeView } until {
                    refundOrderButton { isCompletelyDisplayed() }
                }

                refundOrderButton { click() }
            }
        }

        Screen.onScreen<RefundConfirmationDrawerScreen> {
            Allure.step("Проверяем, что экран отрисовался") {
                wait until {
                    rootSceneView { isVisible() }
                }

                val currentTitle = title("Возврат картой")

                wait until {
                    currentTitle { isCompletelyDisplayed() }
                }

                wait until {
                    ordersLineRecyclerView { isCompletelyDisplayed() }
                }

                closeButton { isCompletelyDisplayed() }
                confirmButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }
            }

            checkThatOrdersIsSelected(orderIds = orderIds)

            Allure.step("Проводим и подтверждаем возврат") {
                confirmButton { click() }

                wait until {
                    backButton { isCompletelyDisplayed() }
                    confirmCardRefundButton { isCompletelyDisplayed() }
                }

                confirmCardRefundButton { click() }
            }
        }

        Screen.onScreen<RefundDashboardScreen> {
            Allure.step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }
            }

            Allure.step("Кнопки \"Подтвердить возврат\", \"Закрыть\" (крестик) и карточка заказа присутствуют на экране") {
                completeButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                heresyIcon {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                contentRecyclerView {
                    orderIds.forEach {
                        refundDashboardOrderCard(it) {
                            isDisplayed()
                        }
                    }
                }
            }

            Allure.step("Нажать на кнопку \"Подтвердить возврат\"") {
                completeButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = orderCard(multiOrderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
            }

            runBlocking { delay(10000) }

            Allure.step("Проскролить дровер до заказа") {
                swipeUp { rootSwipeView } until {
                    drawerOrderItem { isCompletelyDisplayed() }
                }
            }

            Allure.step("Проверить наличие тега 'Возврат' и провалиться в карточку") {
                drawerOrderItem {
                    hasDescendant {
                        containsText(HAS_REFUND_TAG)
                    }
                }
            }
        }
    }

    companion object {
        private const val HAS_REFUND_TAG = "Возврат"
    }
}