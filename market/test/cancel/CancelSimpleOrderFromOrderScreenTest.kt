package ru.yandex.market.tpl.courier.test.cancel

import androidx.test.filters.LargeTest
import com.agoda.kakao.screen.Screen
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Issue
import io.qameta.allure.kotlin.Story
import io.qameta.allure.kotlin.TmsLink
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.domain.feature.order.OrderId
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.presentation.feature.screen.CancellationReasonsScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Отмена заказов")
@DisplayName("Отмена заказов")
class CancelSimpleOrderFromOrderScreenTest : BaseTest() {
    private var orderIdToCancel: OrderId? = null

    override fun prepareData() {
        testDataRepository.createShift(uid)
        val routePointId = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000000"
        ).id
        orderIdToCancel =
            (testDataRepository.createRoutePointWithDeliveryTask(
                uid = uid,
                phone = "+79000000001"
            ).tasks.first() as OrderDeliveryTask).orderId
        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePointId, uid)
        testDataRepository.arriveAtRoutePoint(routePointId)
    }

    @Test
    @Issue("MARKETTPLAPP-667")
    @TmsLink("courier-app-123")
    @Story("Отмена заказов")
    @DisplayName("Отмена простого заказа из карточки в дровере")
    @Description("Отмена обычного заказа из карточки заказа в дровере с последующим выбором причины отмены")
    fun cancelSimpleOrderFromOrderScreen() {
        val orderIdToCancel = checkNotNull(orderIdToCancel?.unwrap())

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(orderIdToCancel)

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

            step("Проскролить экран до кнопки отмены заказа") {
                swipeUp { rootSwipeView } until {
                    cancelOrderButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку отмены заказа") {
                cancelOrderButton { click() }
            }
        }

        Screen.onScreen<CancellationReasonsScreen> {
            step("Дождаться появления списка причин отмены") {
                wait until {
                    cancellationReasonList { isDisplayed() }
                }
            }

            cancellationReasonList {
                step("Выбор причины отмены") {
                    childWith<CancellationReasonsScreen.Item> { withText("Не могу дозвониться") } perform {
                        step("Проверить наличие причины отмены \"Не могу дозвониться\"") {
                            isDisplayed()
                        }

                        step("Выбрать причину отмены \"Не могу дозвониться\"") {
                            click()
                        }
                    }
                }
            }

            step("Нажать кнопку \"Далее\"") {
                continueButton { click() }
            }

            step("Дождаться появления кнопки \"Подтвердить\"") {
                wait until {
                    confirmButton { isDisplayed() }
                }
            }

            step("Нажать кнопку \"Подтвердить\"") {
                confirmButton { click() }
            }

            step("Дождаться закрытия списка причин отмены") {
                wait until {
                    cancellationReasonList { doesNotExist() }
                }
            }
        }

        Screen.onScreen<MainScreen> {
            step("Дождаться, когда задание пропадет из bottom sheet'а") {
                wait until {
                    viewContainsText(orderIdToCancel) perform { doesNotExist() }
                }
            }
        }
    }
}