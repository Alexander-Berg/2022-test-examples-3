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
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryDashboardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.StartShiftScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("DBS смена")
@DisplayName("DBS смена")
class StartDbsShiftAndDeliveryTest : BaseTest() {
    private var externalOrderId: String? = null

    override fun prepareData() {
        externalOrderId = testDataRepository.generateExternalOrderId()
        val orderId = testDataRepository.createDbsOrder(externalOrderId!!)
        testDataRepository.reassignDbsOrder(orderId)
        testDataRepository.getCurrentShift()
    }

    @Ignore("https://st.yandex-team.ru/MARKETTPLAPP-1108")
    @Test
    @Issue("MARKETTPLAPP-948")
    @TmsLink("courier-app-182")
    @Story("DBS")
    fun startDbsShiftAndDelivery() {
        Screen.onScreen<StartShiftScreen> {
            step("Дождаться загрузки главного экрана и появления кнопки \"Начать смену\"") {
                wait until {
                    startShiftButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Начать смену\"") {
                startShiftButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            step("Дождаться смены кнопки на \"Я на месте\"") {
                wait until {
                    arriveButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Я на месте\"") {
                arriveButton { click() }
            }

            step("Дождаться смены кнопки на \"Начать выдачу\"") {
                wait until {
                    giveOutOrderButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Начать выдачу\"") {
                giveOutOrderButton { click() }
            }
        }

        Screen.onScreen<DeliveryScanScreen> {
            manuallyScanOrderId(externalOrderId ?: "")
        }

        Screen.onScreen<DeliveryDashboardScreen> {
            step("Дождаться появления кнопки \"Подтвердить выдачу\"") {
                wait until {
                    confirmDeliveryButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Подтвердить выдачу\"") {
                confirmDeliveryButton { click() }
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

    @After
    fun finishShift() {
        testDataRepository.finishShift()
    }

    companion object {
        private const val IS_DBS_ACCOUNT = true
    }
}
