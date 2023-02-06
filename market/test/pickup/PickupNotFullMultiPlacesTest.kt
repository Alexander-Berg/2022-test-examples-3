package ru.yandex.market.tpl.courier.test.pickup

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
import ru.yandex.market.tpl.courier.R
import ru.yandex.market.tpl.courier.arch.android.camera.Barcode
import ru.yandex.market.tpl.courier.arch.ext.resources
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.domain.account.AccountCredentials
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePoint
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.user.UserProperties
import ru.yandex.market.tpl.courier.presentation.feature.screen.BoxLoadingRoutePointScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.OrdersScannerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.ScanManualInputScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.StartShiftScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.pickup.FinishPickupScanScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Приемка посылок на СЦ")
@DisplayName("Приемка посылок на СЦ")
class PickupNotFullMultiPlacesTest : BaseTest() {
    private val barcodes = mutableSetOf<Barcode>()

    private val completeScanButtonText = resources.getString(R.string.delivery_scan_finish)
    private val mainButtonText = resources.getString(R.string.continue_flow)

    override fun beforeActivityStart(credentials: AccountCredentials) {
        testDataRepository.switchUserFlags(credentials, FLAGS, true)
        testDataRepository.updateCache()
    }

    override fun afterActivityStop(credentials: AccountCredentials) {
        testDataRepository.switchUserFlags(credentials, FLAGS, false)
    }

    override fun prepareData() {
        with(testDataRepository) {
            createShift(uid)
            createRoutePointWithDeliveryTask(
                orderPlaceCount = 2,
                latitude = 55.80455,
                longitude = 37.599555,
                uid = uid
            ).catchPlaces()
        }
    }

    @Test
    @Story("Забор")
    @Issue("MARKETTPLAPP-1268")
    @TmsLink("courier-app-225")
    @DisplayName("Приемка. Неполный многомест")
    @Description("Неполный многомест")
    fun pickupNotFullMultiOrder() {
        Screen.onScreen<StartShiftScreen> {
            step("Дождаться появления кнопки \"Начать смену\"") {
                wait until {
                    startShiftButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Начать смену\"") {
                startShiftButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            step("Дождаться появления кнопки \"Я рядом со складом\"") {
                wait until {
                    iAmNearWarehouse { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Я рядом со складом\"") {
                iAmNearWarehouse { click() }
            }

            step("Дождаться появления кнопки \"Сканировать посылки\"") {
                wait until {
                    scanPackageButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Сканировать посылки\"") {
                scanPackageButton { click() }
            }
        }

        Screen.onScreen<OrdersScannerScreen> {
            step("Дождаться открытия сканера") {
                wait until {
                    scannerView { isDisplayed() }
                }
            }

            step("Нажать на кнопку \"Ввести код вручную\"") {
                scannerView { manualInputButtonClick() }
            }
        }

        Screen.onScreen<ScanManualInputScreen> {
            step("Дождаться появления поля для ввода") {
                wait until {
                    barcodeInput { isDisplayed() }
                }
            }

            step("Ввод externalId") {
                barcodeInput { typeText(barcodes.first().unwrap()) }
            }

            step("Нажать на кнопку \"Подтвердить\"") {
                confirmButton { click() }
            }
        }

        Screen.onScreen<OrdersScannerScreen> {
            step("Дождаться открытия сканера") {
                wait until {
                    scannerView { isDisplayed() }
                }
            }

            step("Проверяем счетчик отсканированных заказов") {
                scannerView {
                    hasCounterTitle("1 из 2")
                }
            }

            step("Открываем дровер") {
                scannerView { openBottomSheet() }
            }

            step("Нажать на \"$completeScanButtonText\"") {
                scannerView { completeButtonClick() }
            }
        }

        Screen.onScreen<OrdersScannerScreen> {
            step("Прием посылок не завершается") {
                scannerView { isDisplayed() }
            }
        }

        step("Отсканировать оставшуюся часть многоместа") {
            Screen.onScreen<OrdersScannerScreen> {
                step("Нажать на кнопку \"Ввести код вручную\"") {
                    scannerView { manualInputButtonClick() }
                }
            }

            Screen.onScreen<ScanManualInputScreen> {
                step("Дождаться появления поля для ввода") {
                    wait until {
                        barcodeInput { isDisplayed() }
                    }
                }

                step("Ввод externalId") {
                    barcodeInput { typeText(barcodes.last().unwrap()) }
                }

                step("Нажать на кнопку \"Подтвердить\"") {
                    confirmButton { click() }
                }
            }
        }

        Screen.onScreen<FinishPickupScanScreen> {
            step("Дождаться открытия экрана завершения сканирования") {
                wait until {
                    mainButton {
                        isDisplayed()
                        hasText(mainButtonText)
                    }
                }
            }

            step("Нажать на \"$mainButtonText\"") {
                mainButton { click() }
            }
        }

        Screen.onScreen<BoxLoadingRoutePointScreen> {
            step("Дождаться появления кнопки \"Готов к выезду со склада\"") {
                wait until {
                    finishPickupButton { isCompletelyDisplayed() }
                }
            }
        }
    }

    private fun RoutePoint.catchPlaces() {
        tasks.forEach { task ->
            barcodes.addAll((task as OrderDeliveryTask).barcodes)
        }
    }

    private companion object {
        val FLAGS = setOf(
            UserProperties.NEW_PICKUP_FLOW_ENABLED,
            UserProperties.USER_ORDER_BATCH_SHIFTING_ENABLED,
        )
    }
}