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
import ru.yandex.market.tpl.courier.presentation.feature.screen.DrawerNavigationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.FinishedTaskListScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.RescheduleScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.checkRadioButton
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Перенос заказов")
@DisplayName("Перенос заказов")
class ReschedulePrepaidSingleOrderWithCommentTest : BaseTest() {

    private lateinit var orderId: String
    private lateinit var orderPlace: String
    private val reasons = listOf(
        "Доставлю сегодня позже",
        "Нет паспорта/Нет 18 лет",
        "Другая проблема",
        "Не удалось доставить в интервал",
        "По просьбе клиента"
    )

    private val intervals = listOf(
        "09:00—22:00",
        "10:00—22:00",
        "10:00—18:00",
    )

    override fun prepareData() {
        testDataRepository.createShift(uid)
        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = true,
            paymentType = PaymentType.Prepaid
        )
        val routePointId = routePoint.id
        orderId = (routePoint.tasks.first() as OrderDeliveryTask).orderId.unwrap()
        orderPlace =
            (routePoint.tasks.first() as OrderDeliveryTask).order.places.first().barcode.unwrap()
        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePointId, uid)
        testDataRepository.arriveAtRoutePoint(routePointId)
    }

    @Test
    @Issue("MARKETTPLAPP-1383")
    @TmsLink("courier-app-246")
    @Story("Перенос заказов")
    @DisplayName("Перенос и восстановление одиночного предоплаченного заказа с комментарием в причине")
    @Description("Перенос простого предоплаченного заказа с вводом комментария")
    fun reschedulePrepaidSingleOrderWithCommentTest() {
        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(orderId)

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

            Allure.step("Выбираем 'Другая проблема' и идем дальше") {
                reasonList.checkRadioButton("Другая проблема") {
                    isCompletelyDisplayed()

                    val button = radioButton("Другая проблема")

                    button {
                        isCompletelyDisplayed()
                        click()
                    }
                }

                confirmButton {
                    containsText("Выбрать")
                    click()
                }
            }

            Allure.step("Вводим коментарий") {
                wait until {
                    input { isCompletelyDisplayed() }
                }

                confirmButton {
                    containsText("Сохранить")
                    isCompletelyDisplayed()
                    isDisabled()
                }

                input.typeText("Some_reason_for_reschedule")

                confirmButton {
                    containsText("Сохранить")
                    isCompletelyDisplayed()
                    isEnabled()
                    click()
                }
            }

            Allure.step("Отрисовались интервалы и кнопка назад") {
                wait until {
                    backButton { isCompletelyDisplayed() }
                }

                confirmButton {
                    isCompletelyDisplayed()
                    containsText("Выбрать")
                }

                reasonList.checkRadioButton("Завтра") {
                    isCompletelyDisplayed()

                    val button = radioButton("Завтра")

                    button {
                        isCompletelyDisplayed()
                        isChecked()
                    }
                }

                reasonList { hasSize(4) }

                reasonList.checkRadioButton("Послезавтра") {
                    isCompletelyDisplayed()
                }

                confirmButton { click() }
            }

            Allure.step("Выбираем завтра и смотрим интервалы") {
                wait until {
                    backButton { isCompletelyDisplayed() }
                }

                confirmButton {
                    isCompletelyDisplayed()
                    containsText("Выбрать")
                }

                intervals.forEachIndexed { index, interval ->
                    reasonList.checkRadioButton(interval) {
                        isCompletelyDisplayed()

                        if (index == 0) {
                            val button = radioButton(interval)

                            button {
                                isCompletelyDisplayed()
                                isChecked()
                            }
                        }
                    }
                }
            }

            Allure.step("Выбираем интервал и подтверждаем перенос") {
                confirmButton { click() }

                contentScene {
                    hasDescendant {
                        containsText("Завтра")
                    }

                    hasDescendant {
                        containsText(intervals[0])
                    }
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
                    viewContainsText(orderId) perform { doesNotExist() }
                }
            }

            runBlocking { delay(10000) }

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

            Allure.step("Проскролить экран до кнопки возобновление заказа") {
                swipeUp { rootSwipeView } until {
                    reopenOrderButton { isCompletelyDisplayed() }
                }
            }

            Allure.step("Возобновляем") {
                reopenOrderButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(orderId)

            Allure.step("Дождаться появление заказа в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
            }
        }
    }
}