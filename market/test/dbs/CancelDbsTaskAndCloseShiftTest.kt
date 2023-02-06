package ru.yandex.market.tpl.courier.test.dbs

import androidx.test.filters.LargeTest
import com.agoda.kakao.screen.Screen
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Issue
import io.qameta.allure.kotlin.Story
import io.qameta.allure.kotlin.TmsLink
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.After
import org.junit.Ignore
import org.junit.Test
import ru.yandex.market.tpl.courier.arch.ext.swipeLeft
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.presentation.feature.screen.CancellationReasonsScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.DrawerNavigationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("DBS смена")
@DisplayName("DBS смена")
class CancelDbsTaskAndCloseShiftTest : BaseTest() {
    private var externalOrderId: String? = null

    override fun prepareData() {
        externalOrderId = testDataRepository.generateExternalOrderId()
        val orderId = testDataRepository.createDbsOrder(externalOrderId!!)
        testDataRepository.reassignDbsOrder(orderId)
        testDataRepository.getCurrentShift()

        testDataRepository.checkIn()
    }

    @Ignore("https://st.yandex-team.ru/MARKETTPLAPP-1108")
    @Test
    @Issue("MARKETTPLAPP-948")
    @TmsLink("courier-app-184")
    @Story("DBS")
    fun cancelDbsTaskAndCloseShift() {
        checkSectionsSideBar()

        Screen.onScreen<MainScreen> {
            step("Дождаться появления основного экрана") {
                rootSwipeView { isVisible() }
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
                    childWith<CancellationReasonsScreen.Item> { withText("Клиент отказался от заказа") } perform {
                        step("Проверить наличие причины отмены \"Не могу дозвониться\"") {
                            isDisplayed()
                        }
                    }
                }
            }

            step("Кнопка \"Далее\" активна") {
                continueButton.isCompletelyDisplayed()
            }

            step("Нажать на кнопку \"Далее\"") {
                continueButton.click()
            }

            step("Кнопка \"Подтвердить\" отображается") {
                confirmButton.isCompletelyDisplayed()
            }

            step("Нажать на кнопку \"Подтвердить\"") {
                confirmButton.click()
            }

            step("Дождаться закрытия списка причин отмены") {
                wait until {
                    cancellationReasonList { doesNotExist() }
                }
            }
        }

        Screen.onScreen<MainScreen> {
            step("Дождаться появления финального текста \"Отлично!\nВсе задания выполнены!\"") {
                wait until {
                    firstFinishText { isCompletelyDisplayed() }
                    secondFinishText { isCompletelyDisplayed() }
                }
            }
        }
    }

    // courier-app-183
    private fun checkSectionsSideBar() {
        Screen.onScreen<MainScreen> {
            step("Дождаться появления кнопки \"navigationDrawer\"") {
                wait until {
                    navigationDrawerButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"navigationDrawer\"") {
                navigationDrawerButton { click() }
            }
        }

        Screen.onScreen<DrawerNavigationScreen> {
            step("Дождаться появления \"navigationDrawer\"") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            step("Проверить отображение кнопки \"Завершенные задания\"") {
                finishedTaskListButton { isCompletelyDisplayed() }
            }

            step("Проверить отображение кнопки \"Помощь\"") {
                helpButton { doesNotExist() }
            }

            step("Проверить отображение кнопки \"Чат с поддержкой\"") {
                supportChatButton { doesNotExist() }
            }

            step("Проверить отсутствие кнопки \"Оценить\"") {
                rateButton { doesNotExist() }
            }

            step("Проверить отображение кнопки \"Уведомления\"") {
                wait until {
                    notificationButton { isCompletelyDisplayed() }
                }
            }

            step("Проверить отображение кнопки \"Настройки\"") {
                swipeUp { rootSwipeView } until {
                    settingButton { isCompletelyDisplayed() }
                }
            }

            step("Проверить отображение кнопки \"Обновить данные\"") {
                swipeUp { rootSwipeView } until {
                    refreshDataButton { isCompletelyDisplayed() }
                }
            }

            step("Проверить отображение кнопки \"Показать QR-код\"") {
                swipeUp { rootSwipeView } until {
                    showQrCodeButton { isCompletelyDisplayed() }
                }
            }

            step("Проверить отображение кнопки \"Выйти\"") {
                swipeUp { rootSwipeView } until {
                    exitButton { isCompletelyDisplayed() }
                }
            }

            step("Свернуть \"navigationDrawer\"") {
                swipeLeft { rootSwipeView } until {
                    rootSwipeView { swipeLeft()}
                }
            }
        }
    }

    @After
    fun finishShift() {
        testDataRepository.finishShift()
    }

    companion object {
        private const val IS_DBS_ACCOUNT = true
    }
}