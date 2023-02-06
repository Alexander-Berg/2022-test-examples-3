package ru.yandex.market.tpl.courier.test.dropship

import androidx.test.filters.LargeTest
import com.agoda.kakao.screen.Screen.Companion.onScreen
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
import ru.yandex.market.tpl.courier.arch.fp.NonEmptyString
import ru.yandex.market.tpl.courier.presentation.feature.screen.CollectDropshipDetailsScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.OutOfTurnConfirmationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.TaskListScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Заборная логистика")
@DisplayName("Заборная логистика")
class DropshipOutOfTurnGoBackTest : BaseTest() {
    private var firstRoutePointName: NonEmptyString? = null
    private var secondRoutePointName: NonEmptyString? = null
    private var thirdRoutePointName: NonEmptyString? = null

    override fun prepareData() {
        testDataRepository.createShift(uid)
        firstRoutePointName = testDataRepository.createRoutePointWithDropshipTask(uid = uid).name
        secondRoutePointName = testDataRepository.createRoutePointWithDropshipTask(uid = uid).name
        thirdRoutePointName = testDataRepository.createRoutePointWithDropshipTask(uid = uid).name

        testDataRepository.checkIn()
    }

    @Test
    @Issue("MARKETTPLAPP-922")
    @TmsLink("courier-app-179")
    @Story("Забор")
    @DisplayName("Внеочередной забор. Отказ от переключения")
    @Description("Переключение на второе задание на забор и отказ от переключения на него")
    fun dropshipOutOfTurnGoBack() {
        onScreen<MainScreen> {
            step("Дождаться загрузки главного экрана и появления кнопки \"Коробочка\"") {
                wait until {
                    taskListButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Коробочка\"") {
                taskListButton { click() }
            }

        }

        onScreen<TaskListScreen> {
            val secondDropshipTaskButton = buttonContainsText(secondRoutePointName!!.toString())

            step("Дождаться загрузки списка заданий и появления превью второго задания на забор") {
                wait until {
                    secondDropshipTaskButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на превью второго задания на забор") {
                secondDropshipTaskButton { click() }
            }
        }

        onScreen<CollectDropshipDetailsScreen> {
            step("Дождаться загрузки экрана забора и появления кнопок \"Я на месте\" и \"Отменить задание\"") {
                wait until {
                    rootSwipeView { isVisible() }
                    arriveButton { isVisible() }
                    cancelDropshipButton { isVisible() }
                }
            }

            step("Пролистать до кнопки \"Я на месте\"") {
                swipeUp { rootSwipeView } until {
                    arriveButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Я на месте\"") {
                arriveButton { click() }
            }
        }

        onScreen<OutOfTurnConfirmationScreen> {
            step("Дождаться появления дровера и кнопок \"Да, хочу выполнить\" и \"Нет, назад\"") {
                wait until {
                    confirmButton { isCompletelyDisplayed() }
                    goBackButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Нет, назад\"") {
                goBackButton { click() }
            }
        }

        onScreen<CollectDropshipDetailsScreen> {
            step("Дождаться закрытия дровера кнопка \"Я на месте\" осталась на жкране") {
                wait until {
                    arriveButton { isCompletelyDisplayed() }
                }
            }

        }

    }
}
