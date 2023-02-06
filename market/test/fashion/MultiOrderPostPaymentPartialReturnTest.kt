package ru.yandex.market.tpl.courier.test.fashion

import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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
import ru.yandex.market.tpl.courier.arch.common.seconds
import ru.yandex.market.tpl.courier.arch.ext.getQuantityString
import ru.yandex.market.tpl.courier.arch.ext.swipeUp
import ru.yandex.market.tpl.courier.arch.ext.testDataRepository
import ru.yandex.market.tpl.courier.arch.ext.wait
import ru.yandex.market.tpl.courier.arch.fp.NonEmptySet
import ru.yandex.market.tpl.courier.arch.fp.map
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.arch.fp.requireNotEmpty
import ru.yandex.market.tpl.courier.domain.feature.order.OrderId
import ru.yandex.market.tpl.courier.domain.feature.order.OrderItem
import ru.yandex.market.tpl.courier.domain.feature.order.minus
import ru.yandex.market.tpl.courier.domain.feature.order.sumOfMoney
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.fashion.Uit
import ru.yandex.market.tpl.courier.presentation.feature.money.MoneyFormatter
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.PaymentDrawerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionClientReturnScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionManualScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionPaymentScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionReturnRegistrationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionScanScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Fashion")
@DisplayName("Fashion")
class MultiOrderPostPaymentPartialReturnTest : BaseTest() {
    private val moneyFormatter = MoneyFormatter()
    private lateinit var firstOrderId: OrderId
    private lateinit var secondOrderId: OrderId
    private lateinit var multiOrderId: String
    private lateinit var uitsToReturn: List<Uit>
    private lateinit var itemsToReturn: List<OrderItem>
    private lateinit var notReturnableItem: OrderItem
    private lateinit var price: String
    private lateinit var boxBarcodeMask: String

    override fun prepareData() {
        testDataRepository.createShift(uid, 47819L)
        firstOrderId = OrderId(testDataRepository.generateExternalOrderId().requireNotEmpty())
        val first =
            testDataRepository.createOrderFromXml(firstOrderId.unwrap(), CREATE_ORDER_XML_FILE_1)
        runBlocking { delay(200) }
        testDataRepository.updateItemInstances(firstOrderId.unwrap(), UPDATE_ORDER_ITEMS_XML_FILE_1)

        runBlocking { delay(1000) }

        secondOrderId = OrderId(testDataRepository.generateExternalOrderId().requireNotEmpty())
        val second =
            testDataRepository.createOrderFromXml(secondOrderId.unwrap(), CREATE_ORDER_XML_FILE_2)
        runBlocking { delay(200) }
        testDataRepository.updateItemInstances(secondOrderId.unwrap(), UPDATE_ORDER_ITEMS_XML_FILE_2)

        testDataRepository.reassignOrders(listOf(first, second), accountCredentials.get())

        val routePoint = testDataRepository.getFirstDeliveryRoutePoint(accountCredentials.get())

        val tasks = routePoint.tasks as NonEmptySet<OrderDeliveryTask>

        multiOrderId = tasks.first().multiOrderId.unwrap()
        notReturnableItem = tasks.find { it.order.externalOrderId == secondOrderId }!!.order.items.first()
        itemsToReturn = tasks.map { it.order.items.first() }
        uitsToReturn = itemsToReturn.map { it.uits.first() }

        val returnPrice = itemsToReturn.sumOfMoney { it.price }.orThrow()
        val totalPrice = tasks.sumOfMoney { it.totalPrice }.orThrow()
        price = totalPrice.minus(returnPrice).map { moneyFormatter.format(it) }.orThrow()

        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePoint.id, uid)
        testDataRepository.arriveAtRoutePoint(routePoint.id)
        boxBarcodeMask = testDataRepository.getSavePackageBoxBarcode()
    }

    @Test
    @Issue("MARKETTPLAPP-1394")
    @TmsLink("courier-app-202")
    @Story("Fashion")
    @DisplayName("Мульти заказ. Постоплата. Частичный отказ в заказах. Оплата по одному заказу.")
    @Description("Мульти заказ. Постоплата. Частичный отказ в заказах. Оплата по одному заказу.")
    fun runTest() {
        val startFittingButtonText = getQuantityString(R.plurals.start_x_fitting_minutes, 0) // 3 секунды дадут 0 минут
        val firstSavePackageBarcode = FashionReturnRegistrationScreen.getRandomBoxBarcode(boxBarcodeMask)
        runBlocking { delay(10) }
        val secondSavePackageBarcode = FashionReturnRegistrationScreen.getRandomBoxBarcode(boxBarcodeMask)

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(multiOrderId)

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

            manuallyScanOrderId(firstOrderId.unwrap())

            waitUntilDrawerAndManualInputButtonIsDisplayed()

            manuallyScanOrderId(secondOrderId.unwrap())
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

            uitsToReturn.forEach {
                manuallyScanOrderId(it.unwrap())
            }

            openDrawerAndWaitUntilOpened("Поднять дровер")

            checkThatCompleteButtonIsEnabled()

            step("Нажать [Завершить сканирование]") {
                scanner {
                    completeButton {
                        click()
                    }
                }
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

            step("Проверяем, что в списке есть заголовок - [Отказы]") {
                wait(10.seconds) until {
                    contentRecyclerView {
                        childWith<FashionClientReturnScreen.TitleItem> {
                            withDescendant {
                                containsText("Отказы")
                            }
                        } perform {
                            isVisible()
                        }
                    }
                }
            }

            step("Проверяем, что у отсканированных позиций есть кнопка [Вернуть в заказ]") {
                itemsToReturn.forEach {
                    orderItemWithName(it.name.unwrap()) {
                        itemAction {
                            isVisible()
                        }
                    }
                }
            }

            step("Нажать [Перейти к оплате]") {
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

            step("Проверить, что присутствует ") {
                step("карточка первого заказа с измененной ценой") {
                    hasOrderCard(firstOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.not_paid).lowercase())
                        }
                        actualPriceView { isVisible() }
                        oldPriceView { isVisible() }
                    }
                }
                step("карточка второго заказа с измененной ценой") {
                    hasOrderCard(secondOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.not_paid).lowercase())
                        }
                        actualPriceView { isVisible() }
                        oldPriceView { isVisible() }
                    }
                }
                step("кнопка [Принять оплату]") {
                    hasPaymentButton()
                }
            }

            step("Нажать на кнопку [Принять оплату]") {
                clickPaymentButton()
            }
        }

        Screen.onScreen<PaymentDrawerScreen> {
            step("Дождаться открытия дровера выбора типа оплаты") {
                wait until {
                    creditCardButton { isCompletelyDisplayed() }
                    cashButton { isCompletelyDisplayed() }
                }
            }

            step("Проверить, что присутствует ") {
                step("Cумма всех заказов в мульте") {
                    title {
                        isVisible()
                        containsText(price)
                    }
                }
                step("Чекбоксы выбора заказа для оплаты") {
                    checkThatOrdersIsSelected(listOf(firstOrderId.unwrap(), secondOrderId.unwrap()))
                }
                step("Кнопка [Картой]") {
                    creditCardButton {
                        isCompletelyDisplayed()
                    }
                }
                step("Кнопка [Наличными]") {
                    cashButton {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("Снять чекбокс у первого заказа") {
                scanOrderLine(firstOrderId.unwrap()) {
                    checkbox {
                        click()
                    }
                }
            }

            step("Проверить, что чекбокс снялся") {
                scanOrderLine(firstOrderId.unwrap()) {
                    checkbox {
                        isNotChecked()
                    }
                }
            }

            step("Нажать кнопку \"${getResourceString(R.string.with_credit_card)}\"") {
                creditCardButton { click() }
            }

            step(
                "Дождаться появления кнопки \"${
                    getResourceString(
                        R.string.payment_confirmed
                    )
                }\" и нажать ее"
            ) {
                wait until { confirmButton { isCompletelyDisplayed() } }
                confirmButton { click() }
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

            step("Проверить, что  ") {
                step("карточка первого заказа не оплачена") {
                    hasOrderCard(firstOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.not_paid).lowercase())
                        }
                    }
                }
                step("карточка второго заказа оплачена") {
                    hasOrderCard(secondOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.paid).lowercase())
                        }
                    }
                }
                step("кнопка [Принять оплату] осталась на экране") {
                    hasPaymentButton()
                }
            }

            step("Вернуться на экран примерки") {
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
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

            step("Проверяем, что кнопка [Вернуть в заказ] у оплаченной позиции неактивна") {
                orderItemWithName(notReturnableItem.name.unwrap()) {
                    itemAction {
                        isVisible()
                        isDisabled()
                    }
                }
            }

            step("Нажать [Перейти к оплате]") {
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

            step("Проверить, что присутствует ") {
                step("кнопка [Принять оплату]") {
                    hasPaymentButton()
                }
            }

            step("Нажать на кнопку [Принять оплату]") {
                clickPaymentButton()
            }
        }

        Screen.onScreen<PaymentDrawerScreen> {
            step(
                "Дождаться открытия дровера выбора типа оплаты и нажать кнопку \"${
                    getResourceString(
                        R.string.with_cash
                    )
                }\""
            ) {
                wait until {
                    creditCardButton { isCompletelyDisplayed() }
                    cashButton { isCompletelyDisplayed() }
                }

                cashButton { click() }
            }

            step(
                "Дождаться появления кнопки \"${
                    getResourceString(
                        R.string.payment_confirmed
                    )
                }\""
            ) {
                wait until { confirmButton { isCompletelyDisplayed() } }
                confirmButton { click() }
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

            step("Проверить, что  ") {
                step("карточка первого заказа оплачена") {
                    hasOrderCard(firstOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.paid).lowercase())
                        }
                    }
                }
                step("карточка второго заказа оплачена") {
                    hasOrderCard(secondOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.paid).lowercase())
                        }
                    }
                }
                step("кнопка [Принять оплату] пропала") {
                    hasNoPaymentButton()
                }
                step("появилась кнопка [Упаковать отказы]") {
                    toReturnPackingButton {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("Нажать [Упаковать отказы]") {
                toReturnPackingButton {
                    click()
                }
            }

        }

        Screen.onScreen<FashionReturnRegistrationScreen> {
            step("Дождаться открытия экрана сканирования сейф пакетов") {
                contentRecyclerView { isVisible() }
            }

            step("Проверить, что на экране присутствуют ") {
                step("Заголовок") {
                    contentRecyclerView {
                        scrollToStart()
                        firstChild<FashionReturnRegistrationScreen.TitleItem> {
                            title { hasText(R.string.fashion_return_registration_dashboard_title) }
                            subtitle { hasText("0 из 2") }
                        }
                    }
                }
                step("Номера заказов") {
                    contentRecyclerView {
                        val itemCountText = getQuantityString(R.plurals.refusals, 1)

                        childWith<FashionReturnRegistrationScreen.OrderToReturnItem> {
                            withDescendant { containsText(firstOrderId.unwrap()) }
                        } perform {
                            itemCount { hasText(itemCountText) }
                            safePackageBarcodeWithIndex(0) perform {
                                doesNotExist()
                            }
                        }

                        childWith<FashionReturnRegistrationScreen.OrderToReturnItem> {
                            withDescendant { containsText(secondOrderId.unwrap()) }
                        } perform {
                            itemCount { hasText(itemCountText) }
                            safePackageBarcodeWithIndex(0) perform {
                                doesNotExist()
                            }
                        }
                    }
                }
                step("Карточки состава заказа") {
                    itemsToReturn.forEach {
                        contentRecyclerView {
                            childWith<FashionReturnRegistrationScreen.FashionReturnItem> {
                                withDescendant {
                                    withText(it.name.unwrap())
                                }
                            } perform {
                                isVisible()
                            }
                        }
                    }
                }
                step("Кнопка [Сканировать сейф-пакет] под каждым заказом") {
                    for (i in 0..1) {
                        contentRecyclerView {
                            childWith<FashionReturnRegistrationScreen.ButtonItem> {
                                withIndex(i) {
                                    withText(R.string.partial_return_scan_safe_package)
                                }
                            } perform {
                                isVisible()
                            }
                        }
                    }
                }
            }

            step("Нажать кнопку [Сканировать сейф-пакет] под первым заказом") {
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
                        subtitle { hasText("1 из 2") }
                    }

                    childWith<FashionReturnRegistrationScreen.OrderToReturnItem> {
                        withDescendant { containsText(firstSavePackageBarcode) }
                    } perform {
                        safePackageBarcodeWithIndex(0) perform {
                            isCompletelyDisplayed()
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

            step("Нажать кнопку [Сканировать сейф-пакет] под вторым заказом") {
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

        scanWithManualInput(secondSavePackageBarcode, "штрихкод второго сейф-пакета")

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
                        subtitle { hasText("2 из 2") }
                    }

                    childWith<FashionReturnRegistrationScreen.OrderToReturnItem> {
                        withDescendant { containsText(secondSavePackageBarcode) }
                    } perform {
                        safePackageBarcodeWithIndex(0) perform {
                            isCompletelyDisplayed()
                        }
                    }
                }
            }

            step("Проверить, что появилась кнопка под составом заказа [Добавить сейф-пакет]") {
                contentRecyclerView {
                    scrollToEnd()
                    childWith<FashionReturnRegistrationScreen.ButtonItem> {
                        withIndex(1) {
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

    companion object {
        const val CREATE_ORDER_XML_FILE_1 = "requestData/fashion_multiorder/fashion_order.xml"
        const val CREATE_ORDER_XML_FILE_2 = "requestData/fashion_multiorder/fashion_order_second.xml"
        const val UPDATE_ORDER_ITEMS_XML_FILE_1 = "requestData/fashion_multiorder/update_item_instances.xml"
        const val UPDATE_ORDER_ITEMS_XML_FILE_2 = "requestData/fashion_multiorder/update_item_instances_second.xml"
    }
}