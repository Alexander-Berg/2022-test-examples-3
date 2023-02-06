package ru.yandex.market.tpl.courier.test.delivery

import androidx.test.filters.LargeTest
import com.agoda.kakao.screen.Screen
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Issue
import io.qameta.allure.kotlin.Story
import io.qameta.allure.kotlin.TmsLink
import io.qameta.allure.kotlin.junit4.DisplayName
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.yandex.market.tpl.courier.arch.common.millis
import ru.yandex.market.tpl.courier.arch.coroutine.delay
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.arch.fp.Exceptional
import ru.yandex.market.tpl.courier.arch.fp.failure
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.arch.fp.requireNotEmpty
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.arch.logs.d
import ru.yandex.market.tpl.courier.arch.logs.e
import ru.yandex.market.tpl.courier.domain.feature.order.OrderId
import ru.yandex.market.tpl.courier.domain.feature.order.sum
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePointId
import ru.yandex.market.tpl.courier.domain.feature.task.MultiOrderId
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryDashboardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.addPhotoButton
import ru.yandex.market.tpl.courier.presentation.feature.screen.dashboardOrderCard
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Выдача заказов")
@DisplayName("Выдача заказов")
class DeliveryMultiOrderFromOrderScreenTest : BaseTest() {
    private lateinit var multiOrderId: String
    private lateinit var orderIds: List<String>

    private fun prepareDeliveryTasks(
        maxAttempts: Int = MAX_RETRY_ATTEMPTS,
    ): Exceptional<Triple<RoutePointId, MultiOrderId, List<OrderId>>> {
        testDataRepository.createShift(uid)
        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000000",
            isPaid = true,
            paymentType = PaymentType.Prepaid
        )

        val tasks = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000000",
            isPaid = true,
            paymentType = PaymentType.Prepaid
        ).tasks.toList() as List<OrderDeliveryTask>

        if (tasks.map { it.totalPrice }.sum().orThrow().amount > MAX_MULTI_ORDER_TOTAL_PRICE.toBigDecimal()) {
            if (maxAttempts > 0) {
                e("DeliveryMultiOrderFromOrderScreenTest: создан заказ дороже 50000руб, пробуем еще раз")
                runBlocking {
                    delay(RETRY_DELAY.millis)
                }
                return prepareDeliveryTasks(maxAttempts = maxAttempts - 1)
            }
            return failure(IllegalStateException("DeliveryMultiOrderFromOrderScreenTest: не удалось получить мульт с ценой меньше 50000 рублей"))
        }

        val multiOrderId = checkNotNull((routePoint.tasks.first() as OrderDeliveryTask).multiOrderId)

        d(
            "DeliveryMultiOrderFromOrderScreenTest: получили мульт стоимостью: ${
                tasks.map { it.totalPrice }.sum().orThrow()
            }"
        )
        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = true,
            paymentType = PaymentType.Prepaid
        )

        return success(Triple(routePoint.id, multiOrderId, tasks.map { it.orderId }))
    }

    override fun prepareData() {
        val (routePointId, multiOrder, ids) = prepareDeliveryTasks().orThrow()
        multiOrderId = multiOrder.unwrap()
        orderIds = ids.map { it.unwrap() }

        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePointId, uid)
        testDataRepository.arriveAtRoutePoint(routePointId)
    }

    @Test
    @Issue("MARKETTPLAPP-760")
    @TmsLink("courier-app-122")
    @Story("Выдача заказов")
    @DisplayName("Выдача оплаченного мульта из карточки в дровере")
    @Description("Выдача оплаченного мульта из карточки в дровере")
    fun deliveryMultiOrderFromOrderScreen() {
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
            step("Дождаться открытия дашборда выдачи") {
                wait until {
                    contentRecyclerView {
                        isDisplayed()
                    }
                }
            }

            step("Кнопки \"Добавить фото\", \"Добавить фото\", \"Закрыть\" (крестик) и карточки заказов присутствуют на экране ") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                heresyIcon {
                    isCompletelyDisplayed()
                    isEnabled()
                }

                contentRecyclerView {
                    addPhotoButton {
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

    companion object {
        private const val MAX_MULTI_ORDER_TOTAL_PRICE = 50000
        private const val RETRY_DELAY = 2000
        private const val MAX_RETRY_ATTEMPTS = 20
    }
}