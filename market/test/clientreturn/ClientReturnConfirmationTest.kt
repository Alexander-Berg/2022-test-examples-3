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
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.ScanManualInputScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnCardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnConfirmationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnItemCardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnItemListScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.scanner.BarcodeScannerScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Клиентский возврат")
@DisplayName("Клиентский возврат")
class ClientReturnConfirmationTest : BaseTest() {

    private lateinit var returnId: ClientReturnId

    override fun prepareData() {
        returnId = testDataRepository.createShiftWithClientReturnTask(uid).externalReturnId
    }

    @Test
    @Issue("MARKETTPLAPP-1311")
    @TmsLink("courier-app-254")
    @Story("Клиентский возврат")
    @DisplayName("Успешный забор клиентского возврата")
    fun clientReturnConfirmationTest() {
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
                    pickupButton.isCompletelyDisplayed()
                }
            }

            Allure.step("Нажать на кнопку 'Забрать возвраты'") {
                pickupButton.click()
            }
        }

        Screen.onScreen<ClientReturnItemListScreen> {
            Allure.step("Дождаться появления списка товаров") {
                wait until { rootContainer.isVisible() }
            }

            Allure.step("Нажать на любой из товаров") {
                itemsList.childAt<ClientReturnItemListScreen.ClientReturnItem>(0) {
                    mainInfoContainer.click()
                }
            }
        }

        Screen.onScreen<ClientReturnItemCardScreen> {
            Allure.step("Дождаться появления информации о товаре") {
                wait until { rootContainer.isVisible() }
            }

            pressBack()
        }

        Screen.onScreen<ClientReturnItemListScreen> {
            Allure.step("Нажать на кнопку 'К упаковке возвратов'") {
                confirmButton.click()
            }
        }

        Screen.onScreen<ClientReturnConfirmationScreen> {
            Allure.step("Дождаться появления экрана подтверждения") {
                wait until { rootContainer.isVisible() }
            }

            barcodeStatus.containsText(TEXT_BARCODE_DETACHED)
            confirmButton.containsText(TEXT_SCAN_BARCODE)

            Allure.step("Нажать на кнопку 'Сканировать штрихкод'") {
                confirmButton.click()
            }
        }

        Screen.onScreen<BarcodeScannerScreen> {
            wait until { rootContainer.isVisible() }

            Allure.step("Нажать на “Ввести код вручную”") {
                manualInputButton.click()
            }
        }

        val barcode = testDataRepository.getClientReturnBarcode()
        Screen.onScreen<ScanManualInputScreen> {
            wait until { rootContainer.isVisible() }

            Allure.step("Ввести не верный штрихкод (пример 100000000)") {
                barcodeInput.typeText("100000000")
                confirmButton.click()
                inputLayoutHelper.hasText("Отсканируйте штрихкод от возврата")
            }

            Allure.step("Ввести верный штрихкод (Формат: VOZ_MK_)") {
                barcodeInput {
                    clearText()
                    typeText(barcode)
                }
                confirmButton.click()
            }
        }

        Screen.onScreen<ClientReturnConfirmationScreen> {
            barcodeStatus.containsText("Штрихкод $barcode")
            confirmButton.containsText(TEXT_TO_NEXT_TASK)

            Allure.step("Нажать на кнопку 'Удалить штрихкод'") {
                removeButton.click()
            }

            barcodeStatus.containsText(TEXT_BARCODE_DETACHED)
            confirmButton.containsText(TEXT_SCAN_BARCODE)

            Allure.step("Нажать на кнопку 'Сканировать штрихкод'") {
                confirmButton.click()
            }
        }

        Screen.onScreen<BarcodeScannerScreen> {
            wait until { rootContainer.isVisible() }

            Allure.step("Нажать на “Ввести код вручную”") {
                manualInputButton.click()
            }
        }

        Screen.onScreen<ScanManualInputScreen> {
            wait until { rootContainer.isVisible() }

            Allure.step("Ввести верный штрихкод (Формат: VOZ_MK_)") {
                barcodeInput.typeText(barcode)
                confirmButton.click()
            }
        }

        Screen.onScreen<ClientReturnConfirmationScreen> {
            barcodeStatus.containsText("Штрихкод $barcode")
            confirmButton.containsText(TEXT_TO_NEXT_TASK)

            Allure.step("Нажать на кнопку 'К следующему заданию'") {
                confirmButton.click()
            }
        }

        Screen.onScreen<MainScreen> {
            rootSwipeView.isCompletelyDisplayed()
        }
    }

    companion object {
        private const val TEXT_BARCODE_DETACHED = "Штрихкод не привязан"
        private const val TEXT_SCAN_BARCODE = "Сканировать штрихкод"
        private const val TEXT_TO_NEXT_TASK = "К следующему заданию"
    }
}