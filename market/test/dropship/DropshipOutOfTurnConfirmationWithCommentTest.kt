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
import org.junit.Ignore
import org.junit.Test
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.arch.fp.NonEmptyString
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePointSwitchReason
import ru.yandex.market.tpl.courier.domain.feature.task.DropshipTask
import ru.yandex.market.tpl.courier.presentation.feature.screen.CollectDropshipDetailsScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.OutOfTurnCommentScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.OutOfTurnConfirmationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.OutOfTurnReasonScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.TaskListScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Заборная логистика")
@DisplayName("Заборная логистика")
class DropshipOutOfTurnConfirmationWithCommentTest : BaseTest() {
    private var secondRoutePointName: NonEmptyString? = null
    private var switchReasons: Set<RoutePointSwitchReason> = emptySet()
    private var firstContact: String = ""
    private var secondContact: String = ""

    override fun prepareData() {
        testDataRepository.setUserSoftMode()
        testDataRepository.createShift(uid)
        val firstRoutePoint = testDataRepository.createRoutePointWithDropshipTask(uid = uid)
        firstContact = (firstRoutePoint.tasks.first() as DropshipTask).contact.orEmpty()

        val secondRoutePoint = testDataRepository.createRoutePointWithDropshipTask(uid = uid)
        secondRoutePointName = secondRoutePoint.name
        secondContact = (secondRoutePoint.tasks.first() as DropshipTask).contact.orEmpty()

        testDataRepository.createRoutePointWithDropshipTask(uid = uid)

        switchReasons = testDataRepository.getDropshipSwitchReasons()

        testDataRepository.checkIn()
    }

    @Test
    @Issue("MARKETTPLAPP-966")
    @TmsLink("courier-app-185")
    @Story("Забор")
    @DisplayName("Внеочередной забор. Выполнение с выбором причины и вводом комментария")
    @Description("Переключение на второе задание на забор и его выполнение")
    fun dropshipOutOfTurnConfim() {
        onScreen<MainScreen> {
            step("Дождаться появления кнопки \"Коробочка\"") {
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

            step("Нажать на кнопку \"Да, хочу выполнить\"") {
                confirmButton { click() }
            }
        }

        onScreen<OutOfTurnReasonScreen> {
            val radioButtons = switchReasons.map {
                radioButtonWithText(it.description.toString())
            }
            val reasonWithCommentRequired = switchReasons.firstOrNull { it.commentRequired }

            step("Дождаться появления экрана с выбором причины выполнения вне очереди") {
                wait until {
                    title { isVisible() }
                    confirmButton { isCompletelyDisplayed() }
                    radioButtons.forEach {
                        it {
                            isVisible()
                        }
                    }
                }
            }

            step("Убедиться, что выбрана первая причина") {
                val firstRadioButton = radioButtons.first()

                firstRadioButton {
                    isChecked()
                }
            }

            step("Убедиться, что присутствует причина требующая ввода комментария") {
                assert(reasonWithCommentRequired != null)
            }

            step("Выбрать причину для которой требуется ввод комментария") {
                val radioButton = radioButtonWithText(reasonWithCommentRequired!!.description.toString())
                radioButton { click() }
            }

            step("Нажать на кнопку \"Далее\"") {
                confirmButton { click() }
            }
        }

        onScreen<OutOfTurnCommentScreen> {
            step("Дождаться появления экрана ввода комментария выполнения вне очереди") {
                wait until {
                    title { isVisible() }
                    confirmButton { isVisible() }
                    textArea { isVisible() }
                }
            }

            step("Кнопка \"Далее\" должна быть задизейблена, фокус на поле ввода комментария") {
                confirmButton { isDisabled() }
                textArea { isFocused() }
            }

            step("Ввести комментарий") {
                textArea {
                    typeText(TEST_COMMENT)
                }
            }

            step("Кнопка \"Далее\" должна быть раздизейблена") {
                wait until {
                    confirmButton { isEnabled() }
                }
            }

            step("Нажать на кнопку \"Далее\"") {
                confirmButton { click() }
            }
        }

        onScreen<MainScreen> {
            val secondRoutePoint = viewContainsText(secondContact)
            step("Дождаться появления второго забора на главном экране") {
                wait until {
                    secondRoutePoint { isVisible() }
                    getDropshipButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Получить посылки\"") {
                getDropshipButton { click() }
            }

            val firstRoutePoint = viewContainsText(firstContact)

            step("Дождаться появления первого забора на главном экране") {
                wait until {
                    firstRoutePoint { isVisible() }
                }
            }
        }
    }

    companion object {
        private const val TEST_COMMENT = "Test comment"
    }
}
