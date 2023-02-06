package ru.yandex.market.tpl.courier.test.refund

import androidx.test.filters.LargeTest
import com.agoda.kakao.common.utilities.getResourceString
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
import ru.yandex.market.tpl.courier.presentation.feature.screen.PaymentDrawerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.RefundConfirmationDrawerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.RefundDashboardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.dashboardOrderCard
import ru.yandex.market.tpl.courier.presentation.feature.screen.paymentButton
import ru.yandex.market.tpl.courier.test.BaseTest
import java.lang.IllegalStateException

@LargeTest
@Epic("Возврат заказов")
@DisplayName("Возврат заказов")
class RefundPaidByCashSingleOrderTest: BaseTest() {

    private lateinit var orderId: String

    private fun prepareDeliveryTasks(): Exceptional<RoutePointId> {
        testDataRepository.createShift(uid)
        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000010",
            isPaid = false,
            paymentType = PaymentType.Cash,
        )

        val rotePointId = routePoint.id
        val tasks = routePoint.tasks.toList() as List<OrderDeliveryTask>

        if (tasks.isEmpty())
            throw IllegalStateException("Нет заданий на точке, хотя мы их создавали!")

        orderId = tasks.map { it.orderId }.first().unwrap()

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
    @TmsLink("courier-app-249")
    @Story("Возврат заказов")
    @DisplayName("Возврат после выдачи одиночного оплаченного налом заказа")
    @Description("Возврат после выдачи одиночного оплаченного налом заказа")
    fun refundPaidByCashSingleOrderTest() {
        Screen.onScreen<MainScreen> {
            val orderView = orderCard(orderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    orderView { isVisible() }
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
            }

            Allure.step("Проверяем наличие карточки") {
                val orderContent = orderContent(orderId)

                orderContent { isVisible() }
            }

            Allure.step("Нажать на кнопку выдачи заказа") {
                giveOutOrderButton { click() }
            }
        }

        Screen.onScreen<DeliveryScanScreen> {
            waitUntilDrawerAndManualInputButtonIsDisplayed()

            openDrawerAndWaitUntilOpened()

            checkThatOrdersIsNotScanned(orderIds = listOf(orderId))

            closeDrawerAndWaitUntilClosed()

            manuallyScanOrderId(orderId)
        }

        Screen.onScreen<DeliveryDashboardScreen> {
            Allure.step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }
            }

            Allure.step("Кнопки \"Подтвердить выдачу\", \"Принять оплату\", \"Закрыть\" (крестик) и карточки заказов присутствуют на экране ") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    isDisabled()
                }

                heresyIcon {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                contentRecyclerView {
                    dashboardOrderCard(orderId) {
                        isDisplayed()
                    }
                    paymentButton {
                        isDisplayed()
                        isEnabled()
                    }
                }
            }

            Allure.step("Нажать на кнопку \"Принять оплату\"") {
                contentRecyclerView {
                    paymentButton {
                        click()
                    }
                }
            }
        }

        Screen.onScreen<PaymentDrawerScreen> {
            Allure.step(
                "Дождаться открытия дровера выбора типа оплаты и нажать кнопку \"${
                    getResourceString(
                        R.string.with_cash
                    )
                }\""
            ) {
                creditCardButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }
                cashButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }
                closeButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }
                cashButton { click() }
            }

            Allure.step(
                "Дождаться появления кнопки \"${
                    getResourceString(
                        R.string.payment_confirmed
                    )
                }\""
            ) {
                closeButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }
                backButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }
                confirmButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }
                confirmButton { click() }
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

            Allure.step("Кнопка \"Подтвердить выдачу\" становится активной, кнопка \"Принять оплату\" пропадает") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                contentRecyclerView {
                    paymentButton {
                        doesNotExist()
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
                    viewContainsText(orderId) perform { doesNotExist() }
                }
            }

            runBlocking { delay(10000) }

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

                val taskView = taskView(orderId)

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

                val currentTitle = title("Возврат наличными")

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

            checkThatOrdersIsSelected(orderIds = listOf(orderId))

            Allure.step("Проводим и подтверждаем возврат") {
                confirmButton { click() }
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
            }

            Allure.step("Нажать на кнопку \"Подтвердить возврат\"") {
                completeButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = orderCard(orderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
            }

            runBlocking { delay(5000) }

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