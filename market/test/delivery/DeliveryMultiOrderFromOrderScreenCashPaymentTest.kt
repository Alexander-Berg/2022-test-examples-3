package ru.yandex.market.tpl.courier.test.delivery

import androidx.test.filters.LargeTest
import com.agoda.kakao.common.utilities.getResourceString
import com.agoda.kakao.screen.Screen
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Issue
import io.qameta.allure.kotlin.Story
import io.qameta.allure.kotlin.TmsLink
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import ru.yandex.market.tpl.courier.R
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.arch.fp.requireNotEmpty
import ru.yandex.market.tpl.courier.domain.feature.order.OrderId
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePointId
import ru.yandex.market.tpl.courier.domain.feature.task.MultiOrderId
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryDashboardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.PaymentDrawerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.dashboardOrderCard
import ru.yandex.market.tpl.courier.presentation.feature.screen.paymentButton
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Выдача заказов")
@DisplayName("Выдача заказов")
class DeliveryMultiOrderFromOrderScreenCashPaymentTest : BaseTest() {
    private lateinit var multiOrderId: String
    private lateinit var orderIds: List<String>

    private fun prepareDeliveryTasks(): Triple<RoutePointId, MultiOrderId, List<OrderId>> {
        testDataRepository.createShift(uid)
        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000000",
            isPaid = false,
            paymentType = PaymentType.Cash
        )

        val tasks = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000000",
            isPaid = false,
            paymentType = PaymentType.Cash
        ).tasks.toList() as List<OrderDeliveryTask>

        val multiOrderId = checkNotNull((routePoint.tasks.first() as OrderDeliveryTask).multiOrderId)

        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = false,
            paymentType = PaymentType.CreditCard
        )

        return Triple(routePoint.id, multiOrderId, tasks.map { it.orderId })
    }

    override fun prepareData() {
        val (routePointId, multiOrder, ids) = prepareDeliveryTasks()
        multiOrderId = multiOrder.unwrap()
        orderIds = ids.map { it.unwrap() }

        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePointId, uid)
        testDataRepository.arriveAtRoutePoint(routePointId)
    }

    @Test
    @Issue("MARKETTPLAPP-764")
    @TmsLink("courier-app-130")
    @Story("Выдача заказов")
    @DisplayName("Выдача мульта из карточки в дровере - оплата наличными")
    @Description("Выдача мульта из карточки в дровере с оплатой наличными")
    fun deliveryMultiOrderFromOrderScreenCashPaymentTest() {
        val multiOrderId = multiOrderId.requireNotEmpty().unwrap()
        val orderIds = orderIds.requireNotEmpty()

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(multiOrderId)

            step("Дождаться появление мульта в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
            }

            step("Проскролить дровер до заказа") {
                swipeUp { rootSwipeView } until {
                    drawerOrderItem { isCompletelyDisplayed() }
                }
            }

            step("Нажать на заказ в дровере") {
                drawerOrderItem { click() }
            }
        }

        Screen.onScreen<MultiOrderScreen> {
            step("Дождаться появления карточки заказа") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            step("Проскролить экран до кнопки выдачи заказа") {
                swipeUp { rootSwipeView } until {
                    giveOutOrderButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку выдачи заказа") {
                giveOutOrderButton { click() }
            }
        }

        Screen.onScreen<DeliveryScanScreen> {
            waitUntilDrawerAndManualInputButtonIsDisplayed()

            openDrawerAndWaitUntilOpened()

            checkThatOrdersIsNotScanned(orderIds = orderIds)

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
            step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }
            }

            step("Кнопки \"Принять оплату\", \"Подтвердить выдачу\", \"Закрыть\" (крестик) и карточки заказов присутствуют на экране ") {
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

            step("Нажать на кнопку \"Принять оплату\"") {
                contentRecyclerView {
                    paymentButton {
                        click()
                    }
                }
            }
        }

        Screen.onScreen<PaymentDrawerScreen> {
            step(
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

            step(
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
            step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                    confirmDeliveryButton {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("Кнопка \"Подтвердить выдачу\" становится активной, кнопка \"Принять оплату\" пропадает") {
                confirmDeliveryButton {
                    isEnabled()
                }

                contentRecyclerView {
                    paymentButton {
                        doesNotExist()
                    }
                }
            }

            step("Нажать на кнопку \"Подтвердить выдачу\"") {
                confirmDeliveryButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            step("Дождаться, когда задание пропадет из bottom sheet'а") {
                wait until {
                    viewContainsText(multiOrderId) perform { doesNotExist() }
                }
            }
        }
    }
}