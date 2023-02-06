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
import ru.yandex.market.tpl.courier.arch.android.camera.Barcode
import ru.yandex.market.tpl.courier.arch.common.millis
import ru.yandex.market.tpl.courier.arch.coroutine.delay
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.arch.fp.Exceptional
import ru.yandex.market.tpl.courier.arch.fp.NonEmptySet
import ru.yandex.market.tpl.courier.arch.fp.failure
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.arch.fp.success
import ru.yandex.market.tpl.courier.arch.logs.d
import ru.yandex.market.tpl.courier.arch.logs.e
import ru.yandex.market.tpl.courier.domain.feature.order.OrderId
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePointId
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
class DeliveryMultiPlaceOrderFromOrderScreenTest : BaseTest() {
    private lateinit var barcodes: NonEmptySet<Barcode>
    private lateinit var orderIdToDelivery: OrderId

    private fun prepareDeliveryTask(maxAttempts: Int = MAX_RETRY_ATTEMPTS): Exceptional<Pair<RoutePointId, OrderDeliveryTask>> {
        testDataRepository.createShift(uid)

        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000000",
            isPaid = true,
            paymentType = PaymentType.Prepaid,
            orderPlaceCount = 2
        )

        val task = routePoint.tasks.first() as OrderDeliveryTask

        if (task.totalPrice.amount > MAX_MULTI_ORDER_TOTAL_PRICE.toBigDecimal()) {
            if (maxAttempts > 0) {
                e("DeliveryMultiPlaceOrderFromOrderScreenTest: создан заказ дороже 50000руб, пробуем еще раз")
                runBlocking {
                    delay(RETRY_DELAY.millis)
                }
                return prepareDeliveryTask(maxAttempts = maxAttempts - 1)
            }
            return failure(IllegalStateException("DeliveryMultiPlaceOrderFromOrderScreenTest: не удалось получить заказ с ценой меньше 50000 рублей"))
        }

        d("DeliveryMultiPlaceOrderFromOrderScreenTest: получили заказ стоимостью: ${task.totalPrice.amount}")

        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = true,
            paymentType = PaymentType.Prepaid
        )

        return success(Pair(routePoint.id, task))
    }

    private fun insertBarcode(barcode: String) {
        Screen.onScreen<DeliveryScanScreen> {
            manuallyScanOrderId(barcode)
        }
    }

    override fun prepareData() {
        val (routePointId, task) = prepareDeliveryTask().orThrow()
        orderIdToDelivery = task.orderId
        barcodes = task.barcodes
        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePointId, uid)
        testDataRepository.arriveAtRoutePoint(routePointId)
    }

    @Test
    @Issue("MARKETTPLAPP-1249")
    @TmsLink("courier-app-217")
    @Story("Выдача заказов")
    @DisplayName("Выдача оплаченного многоместа из карточки")
    @Description("Выдача многоместа из карточки в дровере")
    fun deliveryMultiPlaceOrderFromOrderScreen() {
        val orderIdToDelivery = checkNotNull(orderIdToDelivery.unwrap())

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(orderIdToDelivery)

            step("Дождаться появление заказа в дровере") {
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

        barcodes.forEach {
            insertBarcode(it.unwrap())
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

                    dashboardOrderCard(orderIdToDelivery) {
                        isDisplayed()
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
                    viewContainsText(orderIdToDelivery) perform { doesNotExist() }
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