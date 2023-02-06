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
import ru.yandex.market.tpl.courier.arch.ext.firstInstanceOfOrNull
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.domain.account.AccountCredentials
import ru.yandex.market.tpl.courier.domain.feature.clientreturn.ClientReturnId
import ru.yandex.market.tpl.courier.domain.feature.order.OrderId
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePointId
import ru.yandex.market.tpl.courier.domain.feature.task.ClientReturnTask
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryDashboardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.OrdersScannerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.RatingPageScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.ScanManualInputScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.addPhotoButton
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnCardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnConfirmationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnItemCardScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.clientreturn.ClientReturnItemListScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.dashboardOrderCard
import ru.yandex.market.tpl.courier.presentation.feature.screen.scanner.BarcodeScannerScreen
import ru.yandex.market.tpl.courier.test.BaseTest
import ru.yandex.market.tpl.courier.test.returnorders.ReturnOrdersPartPlacesTest

@LargeTest
@Epic("Клиентский возврат")
@DisplayName("Клиентский возврат")
class ClientReturnOnScTest : BaseTest() {

    private lateinit var returnId: ClientReturnId
    private lateinit var returnRoutePointId: RoutePointId
    private lateinit var deliveryRoutePointId: RoutePointId
    private lateinit var orderIdToDelivery: OrderId
    private lateinit var barcode: String

    override fun prepareData() {
        val shift = testDataRepository.createShift(uid)
        val returnRoutePoint = testDataRepository.createRoutePointWithClientReturnTask(
            uid = uid,
            shiftId = shift.id,
        )

        returnRoutePointId = returnRoutePoint.id
        val task = checkNotNull(returnRoutePoint.tasks.firstInstanceOfOrNull<ClientReturnTask>())
        val deliveryRoutePoint = testDataRepository.createRoutePointWithDeliveryTask(
            uid = uid,
            phone = "+79000000001",
            isPaid = true,
            paymentType = PaymentType.Prepaid,
            itemsPrice = 500,
        )
        val deliveryTask = deliveryRoutePoint.tasks.first() as OrderDeliveryTask

        orderIdToDelivery = checkNotNull(deliveryTask.orderId)
        deliveryRoutePointId = deliveryRoutePoint.id
        returnId = task.externalReturnId

        testDataRepository.checkIn()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(deliveryRoutePointId, uid)
        testDataRepository.arriveAtRoutePoint(deliveryRoutePointId)
    }

    override fun afterActivityStop(credentials: AccountCredentials) {
        testDataRepository.finishShift()
    }

    @Ignore("https://st.yandex-team.ru/MARKETTPL-9462")
    @Test
    @Issue("MARKETTPLAPP-1426")
    @TmsLink("courier-app-257")
    @Story("Клиентский возврат")
    @DisplayName("Возврат на СЦ клиентского возврата")
    fun clientReturnOnScTest() {
        Allure.step("Выдать заказ до клиентского возврата") {
            val orderIdToDelivery = checkNotNull(orderIdToDelivery.unwrap())

            Screen.onScreen<MainScreen> {
                val drawerOrderItem = viewContainsText(orderIdToDelivery)

                Allure.step("Дождаться появление заказа в дровере") {
                    wait until {
                        drawerOrderItem { isVisible() }
                    }
                }

                Allure.step("Проскролить дровер до заказа") {
                    swipeUp { rootSwipeView } until {
                        drawerOrderItem { isCompletelyDisplayed() }
                    }
                }

                Allure.step("Нажать на заказ в дровере") {
                    drawerOrderItem { click() }
                }
            }

            Screen.onScreen<MultiOrderScreen> {
                Allure.step("Дождаться появления карточки заказа") {
                    wait until {
                        rootSwipeView { isVisible() }
                    }
                }

                Allure.step("Проскролить экран до кнопки выдачи заказа") {
                    swipeUp { rootSwipeView } until {
                        giveOutOrderButton { isCompletelyDisplayed() }
                    }
                }

                Allure.step("Нажать на кнопку выдачи заказа") {
                    giveOutOrderButton { click() }
                }
            }

            Screen.onScreen<DeliveryScanScreen> {
                waitUntilDrawerAndManualInputButtonIsDisplayed()

                manuallyScanOrderId(orderIdToDelivery)
            }

            Screen.onScreen<DeliveryDashboardScreen> {
                Allure.step("Дождаться открытия дашборда выдачи") {
                    wait until {
                        contentRecyclerView {
                            isDisplayed()
                        }
                    }
                }

                Allure.step("Кнопки \"Добавить фото\", \"Добавить фото\", \"Закрыть\" (крестик) и карточки заказов присутствуют на экране ") {
                    confirmDeliveryButton {
                        isCompletelyDisplayed()
                        isEnabled()
                    }

                    heresyIcon {
                        isCompletelyDisplayed()
                        isEnabled()
                    }

                    contentRecyclerView {
                        addPhotoButton {
                            isCompletelyDisplayed()
                            isEnabled()
                        }

                        dashboardOrderCard(orderIdToDelivery) {
                            isDisplayed()
                        }

                    }
                }

                Allure.step("Нажать на кнопку \"Подтвердить выдачу\"") {
                    confirmDeliveryButton { click() }
                }
            }
        }

        Allure.step("Забрать клиентского возврат") {
            Screen.onScreen<MainScreen> {
                Allure.step("Дожидаемся появления кнопки 'Я на месте'") {
                    wait until { arriveButton.isCompletelyDisplayed() }

                    arriveButton { click() }

                    wait until { arriveButton.doesNotExist() }
                }

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

            barcode = testDataRepository.getClientReturnBarcode()

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

                Allure.step("Дожидаемся появления кнопки 'Я на месте'") {
                    wait until { arriveButton.isCompletelyDisplayed() }

                    arriveButton { click() }

                    wait until { arriveButton.doesNotExist() }
                }
            }
        }

        Allure.step("Находимся на точке возврата посылок на СЦ") {

            Screen.onScreen<MainScreen> {
                Allure.step("Дождаться появления кнопки \"Возврат посылок на склад\"") {
                    wait until {
                        returnOrdersButton { isCompletelyDisplayed() }
                    }
                }

                Allure.step("Нажать на кнопку \"Возврат посылок на склад\"") {
                    returnOrdersButton { click() }
                }
            }

            Screen.onScreen<OrdersScannerScreen> {
                Allure.step("Дождаться открытия сканера") {
                    wait until {
                        scannerView { isDisplayed() }
                    }
                }

                Allure.step("Нажать на кнопку \"Ввести код вручную\"") {
                    scannerView { manualInputButtonClick() }
                }
            }

            Screen.onScreen<ScanManualInputScreen> {
                Allure.step("Дождаться появления поля для ввода") {
                    wait until {
                        barcodeInput { isDisplayed() }
                    }
                }

                Allure.step("Ввод неверного шк") {
                    barcodeInput { typeText("1000000") }
                    confirmButton { click() }
                    inputLayoutHelper.hasText("Штрих-код не совпадает ни с одной из посылок!")
                }

                Allure.step("Вводим верный шк") {
                    barcodeInput {
                        clearText()
                        typeText(barcode)
                    }
                    confirmButton { click() }
                }
            }

            Screen.onScreen<MainScreen> {
                Allure.step("Дождаться появления кнопки \"Завершить смену\"") {
                    wait until {
                        finishShiftButton { isDisplayed() }
                    }
                }
                Allure.step("Нажать кнопку \"Завершить смену\"") {

                    finishShiftButton { click() }
                }
            }

            Screen.onScreen<RatingPageScreen> {
                Allure.step("Дождаться открытия экрана с отзывом") {
                    wait until {
                        commentInput { isDisplayed() }
                    }
                }
            }
        }
    }

    companion object {
        private const val TEXT_BARCODE_DETACHED = "Штрихкод не привязан"
        private const val TEXT_SCAN_BARCODE = "Сканировать штрихкод"
        private const val TEXT_TO_NEXT_TASK = "К следующему заданию"
    }
}