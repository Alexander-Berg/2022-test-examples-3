package ru.yandex.market.tpl.courier.test.delivery

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
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.PaymentDrawerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.dashboardOrderCard
import ru.yandex.market.tpl.courier.presentation.feature.screen.paymentButton
import ru.yandex.market.tpl.courier.test.BaseTest
import java.lang.IllegalStateException

@LargeTest
@Epic("Выдача заказов")
@DisplayName("Выдача заказов")
class DeliverySingleOrderWithAgeTagCardPaymentTest : BaseTest() {

    private lateinit var orderId: String

    private fun prepareDeliveryTasks(): Exceptional<RoutePointId> {
        testDataRepository.createShift(uid)
        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000000",
            isPaid = false,
            paymentType = PaymentType.CreditCard,
            isRatingR = true,
        )

        val rotePointId = routePoint.id
        val tasks = routePoint.tasks.toList() as List<OrderDeliveryTask>

        if (tasks.isEmpty())
            throw IllegalStateException("Нет заданий на точке, хотя мы их создавали!")

        orderId = tasks.map { it.orderId }.first().unwrap()

        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
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
    @TmsLink("courier-app-233")
    @Story("Выдача заказов")
    @DisplayName("Наличие тега и значка у одиночного неоплаченного заказа с тегом 18+")
    @Description("Выдача одиночного заказа (18+) с оплатой картой")
    fun deliverySingleOrderWithAgeTagCardPaymentTest() {
        Screen.onScreen<MainScreen> {
            val orderView = orderCard(orderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    orderView { isVisible() }
                }
            }

            Allure.step("Проверить наличие тега 'Проверьте паспорт' на превьюшке в дровере") {
                wait(10.seconds) until {
                    orderView {
                        hasDescendant {
                            containsText(CHECK_DOCUMENTS)
                        }
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

            Allure.step("Проверить наличие тега 'Проверьте паспорт' в карточке") {
                rootSwipeView {
                    hasDescendant {
                        containsText(CHECK_DOCUMENTS)
                    }
                }
            }

            Allure.step("Проскролить экран до кнопки выдачи заказа") {
                swipeUp { rootSwipeView } until {
                    giveOutOrderButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Проверяем наличие иконки 18+ у заказа") {
                val orderContent = orderContent(orderId)

                orderContent {
                    isVisible()
                    hasDescendant {
                        withContentDescription("RatingR")
                    }
                }
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
                    contentRecyclerView { isDisplayed() }
                }
            }

            Allure.step("Кнопки \"Принять оплату\", \"Подтвердить выдачу\", \"Закрыть\" (крестик) и карточка заказа присутствуют на экране") {
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

                    dashboardOrderCard(orderId) {
                        isDisplayed()
                    }
                }
            }

            Allure.step("Нажать на кнопку \"Принять оплату\"") {
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
                    paymentButton { doesNotExist() }
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
        }
    }

    companion object {
        private const val CHECK_DOCUMENTS = "Проверьте паспорт"
    }

}