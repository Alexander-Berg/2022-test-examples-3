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
import ru.yandex.market.tpl.courier.presentation.feature.screen.refundCardButton
import ru.yandex.market.tpl.courier.presentation.feature.screen.refundCashButton
import ru.yandex.market.tpl.courier.presentation.feature.screen.refundDashboardOrderCard
import ru.yandex.market.tpl.courier.presentation.feature.screen.scanOrderLine
import ru.yandex.market.tpl.courier.test.BaseTest
import java.lang.IllegalStateException

@LargeTest
@Epic("Возврат заказов")
@DisplayName("Возврат заказов")
class RefundPaidMultiOrderWithDifferentPaymentTypeTest : BaseTest() {

    private lateinit var orderIds: List<String>
    private lateinit var multiOrderId: String
    private lateinit var cashOrderId: String
    private lateinit var cardOrderId: String

    private fun prepareDeliveryTasks(): Exceptional<RoutePointId> {
        testDataRepository.createShift(uid)
        var routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000011",
            isPaid = false,
            paymentType = PaymentType.Cash,
            itemsPrice = 1000,
        )

        cashOrderId =
            (routePoint.tasks.toList() as List<OrderDeliveryTask>).first().orderId.unwrap()

        routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000011",
            isPaid = false,
            paymentType = PaymentType.Cash,
            itemsPrice = 1000,
        )

        val rotePointId = routePoint.id
        val tasks = routePoint.tasks.toList() as List<OrderDeliveryTask>

        if (tasks.isEmpty())
            throw IllegalStateException("Нет заданий на точке, хотя мы их создавали!")

        cardOrderId = tasks.find { it.orderId.unwrap() != cashOrderId }?.orderId?.unwrap().orEmpty()
        orderIds = tasks.map { it.orderId.unwrap() }
        multiOrderId = tasks.first().multiOrderId.unwrap()

        if (cardOrderId.isEmpty())
            throw IllegalStateException("Нет заказа на оплату картой, хотя мы его создавали!")

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
    @Issue("MARKETTPLAPP-1390")
    @TmsLink("courier-app-252")
    @Story("Возврат заказов")
    @DisplayName("Полный возврат после выдачи предоплаченного мульта с разными способами оплаты посылок")
    @Description("Возврат после выдачи мульта оплаченного разными способами")
    fun refundPaidMultiOrderWithDifferentPaymentTypeTest() {
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

            Allure.step("Кнопки \"Принять оплату\", \"Подтвердить выдачу\", \"Закрыть\" (крестик) и карточки заказов присутствуют на экране ") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    isDisabled()
                }

                heresyIcon {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                contentRecyclerView {
                    paymentButton {
                        isCompletelyDisplayed()
                        isEnabled()
                    }

                    orderIds.forEach {
                        dashboardOrderCard(it) {
                            isDisplayed()
                        }
                    }
                }
            }

            Allure.step("Начинаем оплату") {
                contentRecyclerView {
                    paymentButton { click() }
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

                checkThatOrdersIsSelected(orderIds = orderIds)

                ordersLineRecyclerView.scanOrderLine(cardOrderId) {
                    click()
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

            Allure.step("Начинаем оплату") {
                contentRecyclerView {
                    paymentButton { click() }
                }
            }
        }

        Screen.onScreen<PaymentDrawerScreen> {
            Allure.step(
                "Дождаться открытия дровера выбора типа оплаты и нажать кнопку \"${
                    getResourceString(
                        R.string.with_credit_card
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
                creditCardButton { click() }
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
                    viewContainsText(multiOrderId) perform { doesNotExist() }
                }
            }

            runBlocking { delay(5000) }

            Allure.step("Идем в меню") {
                wait(30.seconds) until {
                    navigationDrawerButton { isCompletelyDisplayed() }
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
                    isDisabled()
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

                    refundCardButton { isCompletelyDisplayed() }
                    refundCashButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Начинаем возврат наличными") {
                contentRecyclerView {
                    refundCashButton { click() }
                }
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

            checkThatOrdersIsSelected(orderIds = listOf(cashOrderId))

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

                contentRecyclerView {
                    orderIds.forEach {
                        refundDashboardOrderCard(it) {
                            isDisplayed()

                            if (it == cashOrderId) {
                                hasDescendant {
                                    containsText("Оплачено наличными")
                                }

                                hasDescendant {
                                    containsText("Возврат")
                                }
                            }
                        }
                    }

                    refundCardButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Начинаем возврат картой") {
                contentRecyclerView {
                    refundCardButton { click() }
                }
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

            checkThatOrdersIsSelected(orderIds = listOf(cardOrderId))

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

            Allure.step("Кнопки \"Подтвердить возврат\", \"Закрыть\" (крестик) и карточки заказа присутствуют на экране") {
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

                            hasDescendant {
                                containsText("Возврат")
                            }

                            hasDescendant {
                                containsText(if (it == cashOrderId) "Оплачено наличными" else "Оплачено картой")
                            }
                        }
                    }
                }

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