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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.yandex.market.tpl.courier.R
import ru.yandex.market.tpl.courier.arch.android.camera.Barcode
import ru.yandex.market.tpl.courier.arch.ext.resources
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.domain.account.AccountCredentials
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePoint
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.pickup.OrderPickupTask
import ru.yandex.market.tpl.courier.domain.user.UserProperties
import ru.yandex.market.tpl.courier.presentation.feature.screen.BoxLoadingRoutePointScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.OrdersScannerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.ScanManualInputScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.StartShiftScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.pickup.FinishPickupScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.pickup.SignPickupScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Приемка посылок на СЦ")
@DisplayName("Приемка посылок на СЦ")
class SignPickupAllPlacesTest : BaseTest() {
    private var task: OrderPickupTask? = null
    private var latitude: Double = 55.80455
    private var longitude: Double = 37.599555

    private val barcodes = mutableSetOf<Barcode>()
    private val mainButtonText = resources.getString(R.string.sign_pickup)
    private val secondaryButtonText = resources.getString(R.string.navigate_to_pickup_scanning)

    override fun beforeActivityStart(credentials: AccountCredentials) {
        testDataRepository.switchUserFlags(credentials, FLAGS, true)
        testDataRepository.updateCache()
        runBlocking { delay(60000 ) }
    }

    override fun afterActivityStop(credentials: AccountCredentials) {
        testDataRepository.switchUserFlags(credentials, FLAGS, false)
        testDataRepository.updateCache()
    }

    override fun prepareData() {
        with(testDataRepository) {
            createShift(uid)
            createRoutePointWithDeliveryTask(latitude = latitude++, longitude = longitude++, uid = uid).catchPlaces()
            createRoutePointWithDeliveryTask(
                orderPlaceCount = 2,
                latitude = latitude++,
                longitude = longitude++,
                uid = uid
            ).catchPlaces()
            createRoutePointWithDeliveryTask(latitude = latitude, longitude = longitude, uid = uid).catchPlaces()
            createRoutePointWithDeliveryTask(latitude = latitude, longitude = longitude, uid = uid).catchPlaces()
        }
    }

    @Test
    @Story("Забор")
    @Issue("MARKETTPLAPP-1268")
    @TmsLink("courier-app-220")
    @DisplayName("Апп. Согласование получения всех посылок")
    @Description("Согласование получения всех посылок")
    fun successSignPickupAllPlaces() {
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

        barcodes.forEachIndexed { index, barcode ->
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
                    barcodeInput { typeText(barcode.unwrap()) }
                }

                step("Нажать на кнопку \"Подтвердить\"") {
                    confirmButton { click() }
                }
            }

            val placeScannedCount = index + 1
            val placeTotalCount = barcodes.size

            if (placeScannedCount != placeTotalCount) {
                Screen.onScreen<OrdersScannerScreen> {
                    step("Дождаться открытия сканера") {
                        wait until {
                            scannerView { isDisplayed() }
                        }
                    }

                    step("Проверяем счетчик отсканированных заказов") {
                        scannerView {
                            hasCounterTitle("$placeScannedCount из $placeTotalCount")
                        }
                    }
                }
            }
        }

        Screen.onScreen<FinishPickupScanScreen> {
            step("Ожидаем открытия экрана завершения сканирования") {
                wait until {
                    mainButton { isDisplayed() }
                }
            }

            step("Проверяем что текст mainButton \"$mainButtonText\"") {
                mainButton { hasText(mainButtonText) }
            }

            step("Проверяем что текст secondaryButton \"$secondaryButtonText\" ") {
                secondaryButton { hasText(secondaryButtonText) }
            }

            step("Кликаем по \"$secondaryButtonText\"") {
                secondaryButton { click() }
            }
        }

        Screen.onScreen<OrdersScannerScreen> {
            step("Дождаться открытия сканера") {
                wait until {
                    scannerView { isDisplayed() }
                }
            }

            step("Открываем дровер") {
                scannerView { openBottomSheet() }
            }

            step("Нажать на \"${resources.getString(R.string.delivery_scan_finish)}\"") {
                scannerView { completeButtonClick() }
            }
        }

        Screen.onScreen<FinishPickupScanScreen> {
            step("Дождаться открытия экрана завершения сканирования") {
                wait until {
                    mainButton { isDisplayed() }
                }
            }

            step("Нажать на \"$mainButtonText\"") {
                mainButton { click() }
            }
        }

        Screen.onScreen<SignPickupScreen> {
            step("Дождаться открытия экрана согласования получения") {
                step("Проверяем присутствие кнопки \"Крестик\" для отмены запроса") {
                    wait until {
                        closeButton { isDisplayed() }
                    }
                }
            }
        }

        step("Произвести подписание со стороны СЦ") {
            testDataRepository.transferActSign(uid)
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
            UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED,
            UserProperties.NEW_PICKUP_FLOW_ENABLED,
            UserProperties.USER_ORDER_BATCH_SHIFTING_ENABLED,
        )
    }
}