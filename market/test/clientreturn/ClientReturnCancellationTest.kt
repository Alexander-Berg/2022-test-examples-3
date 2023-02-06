package ru.yandex.market.tpl.courier.test.clientreturn

import androidx.test.filters.LargeTest
import com.agoda.kakao.screen.Screen
import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.Epic
import io.qameta.allure.kotlin.Issue
import io.qameta.allure.kotlin.Story
import io.qameta.allure.kotlin.TmsLink
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.domain.feature.clientreturn.ClientReturnId
import ru.yandex.market.tpl.courier.presentation.feature.screen.CancellationReasonsScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnCardScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Клиентский возврат")
@DisplayName("Клиентский возврат")
class ClientReturnCancellationTest : BaseTest() {

    private lateinit var returnId: ClientReturnId

    override fun prepareData() {
        returnId = testDataRepository.createShiftWithClientReturnTask(uid).externalReturnId
    }

    @Test
    @Issue("MARKETTPLAPP-1423")
    @TmsLink("courier-app-255")
    @Story("Клиентский возврат")
    @DisplayName("Отмена клиентского возврата")
    fun clientReturnCancellationTest() {
        Screen.onScreen<MainScreen> {
            val orderView = orderCard(returnId.value)

            Allure.step("Дождаться появление возврата в дровере") {
                wait until { orderView.isVisible() }
            }

            Allure.step("Проскролить дровер до возврата") {
                swipeUp { rootSwipeView } until { orderView.isCompletelyDisplayed() }
            }

            Allure.step("Нажать на возврат в дровере") {
                orderView.click()
            }
        }

        Screen.onScreen<ClientReturnCardScreen> {
            Allure.step("Дождаться появления карточки возврата") {
                wait until {
                    rootContainer.isVisible()
                    cancelButton.isCompletelyDisplayed()
                }
            }

            Allure.step("Нажать на кнопку 'Отменить задание'") {
                cancelButton.click()
            }
        }

        Screen.onScreen<CancellationReasonsScreen> {
            wait until { cancellationReasonList.isCompletelyDisplayed() }

            Allure.step("Выбрать причину отмены задания") {
                cancellationReasonList.childAt<CancellationReasonsScreen.Item>(0) {
                    click()
                }
            }

            Allure.step("Нажать на кнопку 'Далее'") {
                continueButton.click()
            }

            Allure.step("Нажать на кнопку 'Подтвердить'") {
                wait until { confirmButton.isCompletelyDisplayed() }

                confirmButton.click()

                Screen.onScreen<MainScreen> {
                    rootSwipeView.isCompletelyDisplayed()
                }
            }
        }
    }
}