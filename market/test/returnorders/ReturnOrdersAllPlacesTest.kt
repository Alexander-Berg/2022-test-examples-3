package ru.yandex.market.tpl.courier.test.returnorders

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
import ru.yandex.market.tpl.courier.arch.android.camera.Barcode
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.domain.account.AccountCredentials
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.task.CancelType
import ru.yandex.market.tpl.courier.domain.user.UserProperties
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.OrdersScannerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.RatingPageScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.ScanManualInputScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Возврат посылок на СЦ")
@DisplayName("Возврат посылок на СЦ")
class ReturnOrdersAllPlacesTest : BaseTest() {
    private val barcodes = mutableSetOf<Barcode>()

    override fun beforeActivityStart(credentials: AccountCredentials) {
        testDataRepository.switchUserFlags(credentials, FLAGS, true)
        testDataRepository.updateCache()
    }

    override fun afterActivityStop(credentials: AccountCredentials) {
        testDataRepository.switchUserFlags(credentials, FLAGS, false)
        testDataRepository.finishShift()
    }

    override fun prepareData() {
        with(testDataRepository) {
            createShift(uid)
            val routePoint = createRoutePointWithDeliveryTask(
                orderPlaceCount = 2,
                latitude = 55.80455,
                longitude = 37.599555,
                uid = uid
            )

            val task = routePoint.tasks.first() as OrderDeliveryTask
            barcodes.addAll(task.barcodes)

            testDataRepository.manualFinishPickup(uid)
            testDataRepository.cancelTask(taskId = task.id, type = CancelType.CLIENT_REFUSED, uid = uid)
        }
    }

    @Test
    @Story("Возврат")
    @TmsLink("courier-app-227")
    @Issue("MARKETTPLAPP-1268")
    @DisplayName("Возврат. Возврат всех посылок")
    @Description("Возврат всех посылок")
    fun returnOrdersAllPlaces() {
        Screen.onScreen<MainScreen> {
            step("Дождаться появления кнопки \"Я на месте\"") {
                wait until {
                    arriveButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Я на месте\"") {
                arriveButton { click() }
            }

            step("Дождаться появления кнопки \"Возврат посылок на склад\"") {
                wait until {
                    returnOrdersButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку \"Сканировать посылки\"") {
                returnOrdersButton { click() }
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

        Screen.onScreen<MainScreen> {
            step("Дождаться появления кнопки \"Завершить смену\"") {
                wait until {
                    finishShiftButton { isDisplayed() }
                }
            }
            step("Нажать кнопку \"Завершить смену\"") {
                finishShiftButton { click() }
            }
        }

        Screen.onScreen<RatingPageScreen> {
            step("Дождаться открытия экрана с отзывом") {
                wait until {
                    commentInput { isDisplayed() }
                }
            }
        }
    }


    private companion object {
        val FLAGS = setOf(
            UserProperties.NEW_PICKUP_FLOW_ENABLED,
            UserProperties.USER_ORDER_BATCH_SHIFTING_ENABLED,
        )
    }
}