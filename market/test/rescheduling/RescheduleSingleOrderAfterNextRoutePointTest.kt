package ru.yandex.market.tpl.courier.test.rescheduling

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
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.presentation.feature.screen.FinishedTaskListScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.OrderListScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.RescheduleScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.checkRadioButton
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Перенос заказов")
@DisplayName("Перенос заказов")
class RescheduleSingleOrderAfterNextRoutePointTest: BaseTest() {

    private lateinit var firstOrderId: String
    private lateinit var secondOrderId: String
    private lateinit var multiOrderId: String

    private val reasons = listOf(
        "Доставлю сегодня позже",
        "Нет паспорта/Нет 18 лет",
        "Другая проблема",
        "Не удалось доставить в интервал",
        "По просьбе клиента"
    )

    private val intervals = listOf(
        "Через 15 минут",
        "Через 30 минут",
        "Через 1 час",
        "Через 2 час",
        "Через 3 час",
    )

    override fun prepareData() {
        testDataRepository.createShift(uid)
        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = true,
            paymentType = PaymentType.Prepaid,
            addMinutesToCurrentTime = 30,
        )
        val routePointId = routePoint.id
        firstOrderId = (routePoint.tasks.first() as OrderDeliveryTask).orderId.unwrap()
        multiOrderId = (routePoint.tasks.first() as OrderDeliveryTask).multiOrderId.unwrap()

        val nextRoutePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000002",
            latitude = 55.80555,
            longitude = 37.598555,
            isPaid = true,
            paymentType = PaymentType.Prepaid,
            addMinutesToCurrentTime = 50,
        )
        secondOrderId = (nextRoutePoint.tasks.first() as OrderDeliveryTask).orderId.unwrap()

        testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000003",
            latitude = 55.80355,
            longitude = 37.593555,
            isPaid = true,
            paymentType = PaymentType.Prepaid,
            addMinutesToCurrentTime = 150,
        )

        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePointId, uid)
        testDataRepository.arriveAtRoutePoint(routePointId)
    }

    @Test
    @Issue("MARKETTPLAPP-1385")
    @TmsLink("courier-app-247")
    @Story("Перенос заказов")
    @DisplayName("Перенос и восстановление одиночного заказа в течение дня")
    @Description("Перенос заказа в рамках дня с последующим восстановлением")
    fun rescheduleSingleOrderAfterNextRoutePointTest() {
        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(firstOrderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
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
                    rescheduleOrderButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Нажать на кнопку отмены заказа") {
                rescheduleOrderButton { click() }
            }
        }

        Screen.onScreen<RescheduleScreen> {
            Allure.step("Отрисовались причины переноса") {
                wait until {
                    reasonList { isVisible() }
                }

                wait until {
                    confirmButton { isCompletelyDisplayed() }
                }

                reasons.forEach {
                    reasonList.checkRadioButton(it) {
                        isCompletelyDisplayed()
                    }
                }

                reasonList.checkRadioButton("Доставлю сегодня позже") {
                    isCompletelyDisplayed()

                    val button = radioButton("Доставлю сегодня позже")

                    button {
                        isCompletelyDisplayed()
                        isChecked()
                    }
                }
            }

            Allure.step("Выбираем перенос в рамках дня и проверяем, что отрисовались варианты времени") {
                confirmButton {
                    containsText("Выбрать")
                    click()
                }

                wait until {
                    backButton {
                        isVisible()
                        containsText("Назад")
                    }
                }

                intervals.forEach {
                    reasonList.checkRadioButton(it) {
                        isCompletelyDisplayed()
                    }
                }
            }

            Allure.step("Выбираем время 'Через 2 часа'") {
                reasonList.checkRadioButton("Через 2 час") { click() }
            }

            Allure.step("Проскролить экран до кнопки подтверждения") {
                swipeUp { contentScene } until {
                    confirmButton { isCompletelyDisplayed() }
                }

                confirmButton {
                    containsText("Подтвердить")
                }
            }

            Allure.step("Выбираем интервал и подтверждаем перенос") {
                confirmButton { click() }

                contentScene {
                    hasDescendant {
                        containsText("Сегодня")
                    }

                    hasDescendant {
                        containsText("Доставлю сегодня позже")
                    }
                }

                backButton {
                    isCompletelyDisplayed()
                    containsText("Назад")
                }

                confirmButton {
                    containsText("Подтвердить")
                    click()
                }

                runBlocking { delay(10000) }
            }
        }

        Screen.onScreen<MainScreen> {
            Allure.step("Дождаться, когда задание пропадет из bottom sheet'а") {
                wait until {
                    viewContainsText(firstOrderId) perform { doesNotExist() }
                }
            }

            Allure.step("Дождаться появление нового заказа в дровере") {
                val drawerOrderItem = viewContainsText(secondOrderId)

                wait until {
                    drawerOrderItem { isVisible() }
                }
            }

            runBlocking { delay(5000) }

            Allure.step("Идем в задания на сегодня") {
                wait(30.seconds) until {
                    taskListButton  { isCompletelyDisplayed() }
                }
                taskListButton { click() }
            }
        }

        Screen.onScreen<OrderListScreen> {
            Allure.step("Дожидаемся отрисовки экрана и проваливаемся в заказ") {
                wait until {
                    tasksList { isCompletelyDisplayed() }
                }

                val taskView = taskView(multiOrderId)

                wait until {
                    taskView { isCompletelyDisplayed() }
                }

                taskView {
                    hasDescendant { containsText("Перенос на 2 час") }
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

            Allure.step("Проскролить экран до кнопки возобновление заказа") {
                swipeUp { rootSwipeView } until {
                    revertOrderButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Возобновляем") {
                revertOrderButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(firstOrderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
            }
        }
    }
}