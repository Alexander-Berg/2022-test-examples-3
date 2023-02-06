package ru.yandex.market.tpl.courier.test.fashion

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.yandex.market.tpl.courier.R
import ru.yandex.market.tpl.courier.arch.ext.getQuantityString
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.domain.feature.order.OrderId
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.PaymentDrawerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionClientReturnScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionPaymentScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Fashion")
@DisplayName("Fashion")
class SingleOrderPostPaymentWithoutPartialReturnTest : BaseTest() {

    private lateinit var orderIdToDelivery: OrderId

    override fun prepareData() {
        testDataRepository.createShift(uid, 47819L)
        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            isFashion = true,
            uid = uid,
            isPaid = false,
            paymentType = PaymentType.CreditCard
        )
        val task = routePoint.tasks.first() as OrderDeliveryTask
        orderIdToDelivery = task.orderId
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePoint.id, uid)
        testDataRepository.arriveAtRoutePoint(routePoint.id)
    }

    @Test
    @Issue("MARKETTPLAPP-1130")
    @TmsLink("courier-app-209")
    @Story("Fashion")
    @DisplayName("Одиночный заказ. Постоплата. Пропуск таймера. Пропуск примерки. Оплата")
    @Description("Одиночный заказ. Постоплата. Пропуск таймера. Пропуск примерки. Оплата")
    fun runTest() {
        val startFittingButtonText = getQuantityString(R.plurals.start_x_fitting_minutes, 0) // 3 секунды дадут 0 минут

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(orderIdToDelivery.unwrap())

            step("Дождаться появление заказа в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
                runBlocking {
                    delay(1000) // ждем пока дровер опустится
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

            manuallyScanOrderId(orderIdToDelivery.unwrap())
        }
        Screen.onScreen<FashionClientReturnScreen> {
            step("Дождаться открытия дашборда примерки - оформления возврата") {
                wait until {
                    partialReturnDashboard {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("Проверить, что кнопки [$startFittingButtonText] и [Пропустить примерку] присутствуют на экране") {
                fittingNotStartedActions {
                    isVisible()
                }
                startFittingButton {
                    isCompletelyDisplayed()
                    hasText(startFittingButtonText)
                }
                skipFittingButton {
                    isCompletelyDisplayed()
                }
            }


            step("Нажать на кнопку [Пропустить примерку]") {
                skipFittingButton {
                    click()
                }
                wait until {
                    fittingInProgressActions {
                        isGone()
                    }
                    returnActions {
                        isVisible()
                    }
                }
            }

            step("Проверить, что кнопки сменились на [Сканировать отказы] и [Перейти к оплате]") {
                fittingNotStartedActions {
                    isGone()
                }
                returnActions {
                    isVisible()
                }
                returnScannerButton {
                    isCompletelyDisplayed()
                }
                paymentButton {
                    isCompletelyDisplayed()
                }
            }

            step("Нажать на кнопку [Перейти к оплате]") {
                paymentButton {
                    click()
                }
            }
        }

        Screen.onScreen<FashionPaymentScreen> {
            step("Дождаться открытия экрана оплаты") {
                wait until {
                    rootSwipeView { isVisible() }
                    contentRecyclerView {
                        isVisible()
                    }
                }
            }

            step("Проверить, что присутствует карточка заказа и кнопка [Принять оплату]") {
                hasOrderCard(orderIdToDelivery.unwrap())
                hasPaymentButton()
            }

            step("Нажать на кнопку [Принять оплату]") {
                clickPaymentButton()
            }

        }

        Screen.onScreen<PaymentDrawerScreen> {
            step(
                "Дождаться открытия дровера выбора типа оплаты и нажать кнопку \"${
                    getResourceString(
                        R.string.with_credit_card
                    )
                }\""
            ) {
                wait until {
                    creditCardButton { isCompletelyDisplayed() }
                    cashButton { isCompletelyDisplayed() }
                }

                creditCardButton { click() }
            }

            step(
                "Дождаться появления кнопки \"${
                    getResourceString(
                        R.string.payment_confirmed
                    )
                }\""
            ) {
                wait until { confirmButton { isCompletelyDisplayed() } }
                confirmButton { click() }
            }
        }

        Screen.onScreen<FashionPaymentScreen> {
            step("Дождаться открытия экрана оплаты") {
                wait until {
                    rootSwipeView { isVisible() }
                    contentRecyclerView {
                        isVisible()
                    }
                }
            }

            step("Проверить, что присутствует кнопка [Подтвердить выдачу]") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                }
            }

            step("Нажать на кнопку [Подтвердить выдачу]") {
                confirmDeliveryButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            step("В дровере появилось новое задание и кнопка [Я на месте]") {
                wait until {
                    arriveButton {
                        isCompletelyDisplayed()
                    }
                }
            }
        }

    }
}