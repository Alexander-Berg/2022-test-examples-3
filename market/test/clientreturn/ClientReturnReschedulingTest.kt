package ru.yandex.market.tpl.courier.test.clientreturn

import androidx.test.filters.LargeTest
import com.agoda.kakao.screen.Screen
import io.qameta.allure.kotlin.Allure
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
import ru.yandex.market.tpl.courier.domain.feature.clientreturn.ClientReturnId
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.RescheduleScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnCardScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Клиентский возврат")
@DisplayName("Клиентский возврат")
class ClientReturnReschedulingTest : BaseTest() {

    private lateinit var returnId: ClientReturnId

    override fun prepareData() {
        returnId = testDataRepository.createShiftWithClientReturnTask(uid, 47819L).externalReturnId
    }

    @Test
    @Issue("MARKETTPLAPP-1424")
    @TmsLink("courier-app-256")
    @Story("Клиентский возврат")
    @DisplayName("Перенос клиентского возврата")
    fun clientReturnReschedulingTest() {
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
                    rescheduleButton.isCompletelyDisplayed()
                }
            }

            Allure.step("Нажать на кнопку “Перенести задание”") {
                rescheduleButton.click()
            }
        }

        Screen.onScreen<RescheduleScreen> {
            wait until { reasonList.isCompletelyDisplayed() }

            Allure.step("Выбрать причину переноса") {
                reasonList.childAt<RescheduleScreen.RadioItem>(1) { click() }
            }

            Allure.step("Нажать на кнопку 'Выбрать'") {
                confirmButton.click()
            }

            wait until { reasonList.isCompletelyDisplayed() }

            Allure.step("Выбрать дату") {
                reasonList.childAt<RescheduleScreen.RadioItem>(0) { click() }
            }

            Allure.step("Нажать на кнопку 'Выбрать'") {
                confirmButton.click()
            }

            Allure.step("Выбрать время") {
                reasonList.childAt<RescheduleScreen.RadioItem>(0) { click() }
            }

            Allure.step("Нажать на кнопку 'Выбрать'") {
                confirmButton.click()
            }

            Allure.step("Нажать кнопку 'Подтвердить'") {
                confirmButton.click()

                Screen.onScreen<MainScreen> {
                    rootSwipeView.isCompletelyDisplayed()
                }
            }
        }
    }
}
