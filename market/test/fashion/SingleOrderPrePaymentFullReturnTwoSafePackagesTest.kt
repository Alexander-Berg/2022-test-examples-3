package ru.yandex.market.tpl.courier.test.fashion

import androidx.test.filters.LargeTest
import com.agoda.kakao.common.utilities.getResourceString
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
import ru.yandex.market.tpl.courier.arch.ext.getQuantityString
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.domain.feature.order.OrderId
import ru.yandex.market.tpl.courier.domain.feature.order.OrderItem
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.domain.feature.task.fashion.Uit
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionClientReturnScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionManualScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionPaymentScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionReturnRegistrationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionScanScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Fashion")
@DisplayName("Fashion")
class SingleOrderPrePaymentFullReturnTwoSafePackagesTest : BaseTest() {

    private lateinit var orderIdToDelivery: OrderId
    private lateinit var uitList: List<Uit>
    private lateinit var items: List<OrderItem>
    private lateinit var boxBarcodeMask: String

    override fun prepareData() {
        testDataRepository.createShift(uid, 47819L)
        val routePoint = testDataRepository.createRoutePointWithDeliveryTask(
            isFashion = true,
            uid = uid,
            isPaid = true,
            paymentType = PaymentType.Prepaid
        )
        val task = routePoint.tasks.first() as OrderDeliveryTask
        orderIdToDelivery = task.orderId
        uitList = task.uits
        items = task.order.items
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePoint.id, uid)
        testDataRepository.arriveAtRoutePoint(routePoint.id)
        boxBarcodeMask = testDataRepository.getSavePackageBoxBarcode()
    }

    @Test
    @Issue("MARKETTPLAPP-1355")
    @TmsLink("courier-app-197")
    @Story("Fashion")
    @DisplayName("Одиночный заказ. Предоплата. Отказ от всех позиций. Два сейф-пакета")
    @Description("Одиночный заказ. Предоплата. Отказ от всех позиций. Два сейф-пакета")
    fun runTest() {
        val startFittingButtonText = getQuantityString(R.plurals.start_x_fitting_minutes, 0) // 3 секунды дадут 0 минут
        val firstSavePackageBarcode = FashionReturnRegistrationScreen.getRandomBoxBarcode(boxBarcodeMask)
        runBlocking { delay(10) }
        val secondSavePackageBarcode = FashionReturnRegistrationScreen.getRandomBoxBarcode(boxBarcodeMask)

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(orderIdToDelivery.unwrap())

            step("Дождаться появление заказа в дровере") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
                runBlocking {
                    delay(1000) // ждем пока дровер опустится
                }
            }

            step("Проскролить дровер до заказа") {
                swipeUp { rootSwipeView } until {
                    drawerOrderItem { isCompletelyDisplayed() }
                }
            }

            step("Нажать на заказ в дровере") {
                drawerOrderItem { click() }
            }
        }

        Screen.onScreen<MultiOrderScreen> {
            step("Дождаться появления карточки заказа") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            step("Проскролить экран до кнопки выдачи заказа") {
                swipeUp { rootSwipeView } until {
                    giveOutOrderButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку выдачи заказа") {
                giveOutOrderButton { click() }
            }
        }

        Screen.onScreen<DeliveryScanScreen> {
            waitUntilDrawerAndManualInputButtonIsDisplayed()

            manuallyScanOrderId(orderIdToDelivery.unwrap())
        }
        Screen.onScreen<FashionClientReturnScreen> {
            step("Дождаться открытия дашборда примерки - оформления возврата") {
                wait until {
                    partialReturnDashboard {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("Проверить, что кнопки [$startFittingButtonText] и [Пропустить примерку] присутствуют на экране") {
                fittingNotStartedActions {
                    isVisible()
                }
                startFittingButton {
                    isCompletelyDisplayed()
                    hasText(startFittingButtonText)
                }
                skipFittingButton {
                    isCompletelyDisplayed()
                }
            }


            step("Нажать на кнопку [$startFittingButtonText]") {
                startFittingButton {
                    click()
                }
            }

            step("Дождаться завершения таймера") {
                wait until {
                    fittingInProgressActions {
                        isGone()
                    }
                    fittingExpiredActions {
                        isVisible()
                    }
                }

                completeExpiredFittingButton {
                    isVisible()
                }
            }

            step("Нажать на кнопку [Завершить примерку]") {
                completeExpiredFittingButton {
                    click()
                }

            }

            step("Проверить, что кнопки сменились на [Сканировать отказы] и [Продолжить]") {
                wait until {
                    fittingExpiredActions {
                        isGone()
                    }

                    returnActions {
                        isVisible()
                    }
                }

                returnScannerButton {
                    isCompletelyDisplayed()
                }
                paymentButton {
                    isCompletelyDisplayed()
                    hasText("Продолжить")
                }
            }

            step("Нажать на кнопку [Сканировать отказы]") {
                returnScannerButton {
                    click()
                }
            }
        }

        Screen.onScreen<DeliveryScanScreen> {
            waitUntilDrawerAndManualInputButtonIsDisplayed()

            step("Проверить, что список отказов пустой") {
                recyclerView {
                    hasSize(0)
                }
            }

            uitList.forEach {
                manuallyScanOrderId(it.unwrap())
            }
        }

        Screen.onScreen<FashionClientReturnScreen> {
            step("Дождаться открытия дашборда примерки - оформления возврата") {
                wait until {
                    partialReturnDashboard {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("Проверяем, что в списке первый заголовок - [Отказы]") {
                contentRecyclerView {
                    childAt<FashionClientReturnScreen.TitleItem>(0) {
                        title {
                            hasText("Отказы")
                        }
                    }
                }
            }

            step("Проверяем, что в списке у всех позиций есть кнопка вернуть в заказ") {
                items.forEach {
                    orderItemWithName(it.name.unwrap()) {
                        itemAction {
                            isVisible()
                        }
                    }
                }
            }

            step("Нажать на кнопку [Продолжить]") {
                paymentButton { click() }
            }
        }

        Screen.onScreen<FashionPaymentScreen> {
            step("Дождаться открытия экрана оплаты") {
                wait until {
                    rootSwipeView { isVisible() }
                    contentRecyclerView {
                        isVisible()
                    }
                }
            }

            step("Проверить, что присутствует карточка заказа в группе \"Клиент отказался от всех товаров\" и кнопка [Упаковать отказы]") {
                titleWithIndex(0) {
                    title {
                        hasText(R.string.all_parcels_not_purchased)
                    }
                }
                hasOrderCard(orderIdToDelivery.unwrap()) {
                    isVisible()
                    orderStatusView {
                        hasText(getResourceString(R.string.will_be_return_to_credit_card).lowercase())
                    }
                }
                toReturnPackingButton {
                    isCompletelyDisplayed()
                }
            }

            step("Нажать на кнопку [Упаковать отказы]") {
                toReturnPackingButton { click() }
            }

        }

        Screen.onScreen<FashionReturnRegistrationScreen> {
            step("Дождаться открытия экрана сканирования сейф пакетов") {
                contentRecyclerView { isVisible() }
            }

            step("Проверить, что на экране есть заказ и все его позиции и кнопка [Сканировать сейф-пакет]") {
                contentRecyclerView {
                    scrollToStart()
                    firstChild<FashionReturnRegistrationScreen.TitleItem> {
                        title { hasText(R.string.fashion_return_registration_dashboard_title) }
                        subtitle { hasText("0 из 1") }
                    }

                    val itemCountText = getQuantityString(R.plurals.refusals, items.sumOf { it.count }.toInt())

                    childWith<FashionReturnRegistrationScreen.OrderToReturnItem> {
                        withDescendant { containsText(orderIdToDelivery.unwrap()) }
                    } perform {
                        itemCount { hasText(itemCountText) }
                        safePackageBarcodeWithIndex(0) perform {
                            doesNotExist()
                        }
                    }

                    items.forEach {
                        childWith<FashionReturnRegistrationScreen.FashionReturnItem> {
                            withDescendant {
                                withText(it.name.unwrap())
                            }
                        } perform {
                            isVisible()
                        }
                    }

                    childWith<FashionReturnRegistrationScreen.ButtonItem> {
                        withIndex(0) {
                            withText(R.string.partial_return_scan_safe_package)
                        }
                    } perform {
                        isVisible()
                    }
                }
            }

            step("Нажать кнопку [Сканировать сейф-пакет]") {
                contentRecyclerView {
                    childWith<FashionReturnRegistrationScreen.ButtonItem> {
                        withIndex(0) {
                            withText(R.string.partial_return_scan_safe_package)
                        }
                    } perform {
                        click()
                    }
                }
            }
        }

        scanWithManualInput(firstSavePackageBarcode, "штрихкод первого сейф-пакета")

        Screen.onScreen<FashionReturnRegistrationScreen> {
            step("Дождаться открытия экрана сканирования сейф пакетов") {
                wait until {
                    contentRecyclerView { isCompletelyDisplayed() }
                }
            }

            step("Проверить, что на плитке заказа отображен сейф-пакет") {
                contentRecyclerView {
                    scrollToStart()
                    firstChild<FashionReturnRegistrationScreen.TitleItem> {
                        title { hasText(R.string.fashion_return_registration_dashboard_title) }
                        subtitle { hasText("1 из 1") }
                    }

                    childWith<FashionReturnRegistrationScreen.OrderToReturnItem> {
                        withDescendant { containsText(orderIdToDelivery.unwrap()) }
                    } perform {
                        safePackageBarcodeWithIndex(0) perform {
                            isCompletelyDisplayed()
                            containsText(firstSavePackageBarcode)
                        }
                    }
                }
            }

            step("Проверить, что появилась кнопка под составом заказа [Добавить сейф-пакет]") {
                contentRecyclerView {
                    childWith<FashionReturnRegistrationScreen.ButtonItem> {
                        withIndex(0) {
                            withText(R.string.partial_return_add_safe_package)
                        }
                    } perform {
                        isVisible()
                    }
                }
            }

            step("Проверить, что внизу экрана появилась кнопка [К следующему заданию]") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    hasText(R.string.to_the_next_task)
                }
            }

            step("Нажать кнопку [Добавить сейф-пакет]") {
                contentRecyclerView {
                    childWith<FashionReturnRegistrationScreen.ButtonItem> {
                        withIndex(0) {
                            withText(R.string.partial_return_add_safe_package)
                        }
                    } perform {
                        click()
                    }
                }
            }
        }

        scanWithManualInput(secondSavePackageBarcode, "штрихкод второго сейф-пакета")

        Screen.onScreen<FashionReturnRegistrationScreen> {
            step("Дождаться открытия экрана сканирования сейф пакетов") {
                wait until {
                    contentRecyclerView { isCompletelyDisplayed() }
                }
            }

            step("Проверить, что на плитке заказа отображены оба сейф-пакета") {
                contentRecyclerView {
                    scrollToStart()
                    childWith<FashionReturnRegistrationScreen.OrderToReturnItem> {
                        withDescendant { containsText(orderIdToDelivery.unwrap()) }
                    } perform {
                        listOf(firstSavePackageBarcode, secondSavePackageBarcode).forEachIndexed { index, barcode ->
                            safePackageBarcodeWithIndex(index) perform {
                                isCompletelyDisplayed()
                                containsText(barcode)
                            }
                        }
                    }
                }
            }

            step("Проверить, что появилась кнопка под составом заказа [Добавить сейф-пакет]") {
                contentRecyclerView {
                    childWith<FashionReturnRegistrationScreen.ButtonItem> {
                        withIndex(0) {
                            withText(R.string.partial_return_add_safe_package)
                        }
                    } perform {
                        isVisible()
                    }
                }
            }

            step("Проверить, что внизу экрана появилась кнопка [К следующему заданию]") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    hasText(R.string.to_the_next_task)
                }
            }

            step("Нажать кнопку [К следующему заданию]") {
                confirmDeliveryButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            step("В дровере появилось новое задание и кнопка [Я на месте]") {
                wait until {
                    arriveButton {
                        isCompletelyDisplayed()
                    }
                }
            }
        }

    }

    private fun scanWithManualInput(barcode: String, whatToInput: String) {
        Screen.onScreen<FashionScanScreen> {
            step("Дождаться появления кнопки ручного ввода баркода") {
                wait until {
                    manualInputButton { isCompletelyDisplayed() }
                }
            }

            step("Нажать на кнопку ручного ввода баркода") {
                manualInputButton { click() }
            }

        }

        Screen.onScreen<FashionManualScanScreen> {
            step("Дождаться открытия экрана ручного ввода") {
                wait until {
                    confirmButton { isVisible() }
                    barcodeInput { isVisible() }
                }
            }

            step("Ввести в поле ввода $whatToInput") {
                barcodeInput {
                    typeText(barcode)
                }
            }

            step("Нажать на кнопку \"Готово\"") {
                confirmButton { click() }
            }
        }
    }
}