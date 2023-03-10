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
    @DisplayName("???????????? ??????????. ????????????????????. ?????????????????? ?????????? ?? ??????????????. ???????????? ???? ???????????? ????????????.")
    @Description("???????????? ??????????. ????????????????????. ?????????????????? ?????????? ?? ??????????????. ???????????? ???? ???????????? ????????????.")
    fun runTest() {
        val startFittingButtonText = getQuantityString(R.plurals.start_x_fitting_minutes, 0) // 3 ?????????????? ?????????? 0 ??????????
        val firstSavePackageBarcode = FashionReturnRegistrationScreen.getRandomBoxBarcode(boxBarcodeMask)
        runBlocking { delay(10) }
        val secondSavePackageBarcode = FashionReturnRegistrationScreen.getRandomBoxBarcode(boxBarcodeMask)

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(multiOrderId)

            step("?????????????????? ?????????????????? ???????????? ?? ??????????????") {
                wait until {
                    drawerOrderItem { isVisible() }
                }
                runBlocking {
                    delay(1000) // ???????? ???????? ???????????? ??????????????????
                }
            }

            step("?????????????????????? ???????????? ???? ????????????") {
                swipeUp { rootSwipeView } until {
                    drawerOrderItem { isCompletelyDisplayed() }
                }
            }

            step("???????????? ???? ?????????? ?? ??????????????") {
                drawerOrderItem { click() }
            }
        }

        Screen.onScreen<MultiOrderScreen> {
            step("?????????????????? ?????????????????? ???????????????? ????????????") {
                wait until {
                    rootSwipeView { isVisible() }
                }
            }

            step("?????????????????????? ?????????? ???? ???????????? ???????????? ????????????") {
                swipeUp { rootSwipeView } until {
                    giveOutOrderButton { isCompletelyDisplayed() }
                }
            }

            step("???????????? ???? ???????????? ???????????? ????????????") {
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
            step("?????????????????? ???????????????? ???????????????? ???????????????? - ???????????????????? ????????????????") {
                wait until {
                    partialReturnDashboard {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("??????????????????, ?????? ???????????? [$startFittingButtonText] ?? [???????????????????? ????????????????] ???????????????????????? ???? ????????????") {
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

            step("???????????? ???? ???????????? [$startFittingButtonText]") {
                startFittingButton {
                    click()
                }
            }

            step("?????????????????? ???????????????????? ??????????????") {
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

            step("???????????? ???? ???????????? [?????????????????? ????????????????]") {
                completeExpiredFittingButton {
                    click()
                }

            }

            step("??????????????????, ?????? ???????????? ?????????????????? ???? [?????????????????????? ????????????] ?? [????????????????????]") {
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

            step("???????????? ???? ???????????? [?????????????????????? ????????????]") {
                returnScannerButton {
                    click()
                }
            }
        }

        Screen.onScreen<DeliveryScanScreen> {
            waitUntilDrawerAndManualInputButtonIsDisplayed()

            step("??????????????????, ?????? ???????????? ?????????????? ????????????") {
                recyclerView {
                    hasSize(0)
                }
            }

            uitsToReturn.forEach {
                manuallyScanOrderId(it.unwrap())
            }

            openDrawerAndWaitUntilOpened("?????????????? ????????????")

            checkThatCompleteButtonIsEnabled()

            step("???????????? [?????????????????? ????????????????????????]") {
                scanner {
                    completeButton {
                        click()
                    }
                }
            }
        }

        Screen.onScreen<FashionClientReturnScreen> {
            step("?????????????????? ???????????????? ???????????????? ???????????????? - ???????????????????? ????????????????") {
                wait until {
                    partialReturnDashboard {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("??????????????????, ?????? ?? ???????????? ???????? ?????????????????? - [????????????]") {
                wait(10.seconds) until {
                    contentRecyclerView {
                        childWith<FashionClientReturnScreen.TitleItem> {
                            withDescendant {
                                containsText("????????????")
                            }
                        } perform {
                            isVisible()
                        }
                    }
                }
            }

            step("??????????????????, ?????? ?? ?????????????????????????????? ?????????????? ???????? ???????????? [?????????????? ?? ??????????]") {
                itemsToReturn.forEach {
                    orderItemWithName(it.name.unwrap()) {
                        itemAction {
                            isVisible()
                        }
                    }
                }
            }

            step("???????????? [?????????????? ?? ????????????]") {
                paymentButton { click() }
            }
        }

        Screen.onScreen<FashionPaymentScreen> {
            step("?????????????????? ???????????????? ???????????? ????????????") {
                wait until {
                    rootSwipeView { isVisible() }
                    contentRecyclerView {
                        isVisible()
                    }
                }
            }

            step("??????????????????, ?????? ???????????????????????? ") {
                step("???????????????? ?????????????? ???????????? ?? ???????????????????? ??????????") {
                    hasOrderCard(firstOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.not_paid).lowercase())
                        }
                        actualPriceView { isVisible() }
                        oldPriceView { isVisible() }
                    }
                }
                step("???????????????? ?????????????? ???????????? ?? ???????????????????? ??????????") {
                    hasOrderCard(secondOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.not_paid).lowercase())
                        }
                        actualPriceView { isVisible() }
                        oldPriceView { isVisible() }
                    }
                }
                step("???????????? [?????????????? ????????????]") {
                    hasPaymentButton()
                }
            }

            step("???????????? ???? ???????????? [?????????????? ????????????]") {
                clickPaymentButton()
            }
        }

        Screen.onScreen<PaymentDrawerScreen> {
            step("?????????????????? ???????????????? ?????????????? ???????????? ???????? ????????????") {
                wait until {
                    creditCardButton { isCompletelyDisplayed() }
                    cashButton { isCompletelyDisplayed() }
                }
            }

            step("??????????????????, ?????? ???????????????????????? ") {
                step("C???????? ???????? ?????????????? ?? ????????????") {
                    title {
                        isVisible()
                        containsText(price)
                    }
                }
                step("???????????????? ???????????? ???????????? ?????? ????????????") {
                    checkThatOrdersIsSelected(listOf(firstOrderId.unwrap(), secondOrderId.unwrap()))
                }
                step("???????????? [????????????]") {
                    creditCardButton {
                        isCompletelyDisplayed()
                    }
                }
                step("???????????? [??????????????????]") {
                    cashButton {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("?????????? ?????????????? ?? ?????????????? ????????????") {
                scanOrderLine(firstOrderId.unwrap()) {
                    checkbox {
                        click()
                    }
                }
            }

            step("??????????????????, ?????? ?????????????? ????????????") {
                scanOrderLine(firstOrderId.unwrap()) {
                    checkbox {
                        isNotChecked()
                    }
                }
            }

            step("???????????? ???????????? \"${getResourceString(R.string.with_credit_card)}\"") {
                creditCardButton { click() }
            }

            step(
                "?????????????????? ?????????????????? ???????????? \"${
                    getResourceString(
                        R.string.payment_confirmed
                    )
                }\" ?? ???????????? ????"
            ) {
                wait until { confirmButton { isCompletelyDisplayed() } }
                confirmButton { click() }
            }
        }

        Screen.onScreen<FashionPaymentScreen> {
            step("?????????????????? ???????????????? ???????????? ????????????") {
                wait until {
                    rootSwipeView { isVisible() }
                    contentRecyclerView {
                        isVisible()
                    }
                }
            }

            step("??????????????????, ??????  ") {
                step("???????????????? ?????????????? ???????????? ???? ????????????????") {
                    hasOrderCard(firstOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.not_paid).lowercase())
                        }
                    }
                }
                step("???????????????? ?????????????? ???????????? ????????????????") {
                    hasOrderCard(secondOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.paid).lowercase())
                        }
                    }
                }
                step("???????????? [?????????????? ????????????] ???????????????? ???? ????????????") {
                    hasPaymentButton()
                }
            }

            step("?????????????????? ???? ?????????? ????????????????") {
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
            }
        }

        Screen.onScreen<FashionClientReturnScreen> {
            step("?????????????????? ???????????????? ???????????????? ???????????????? - ???????????????????? ????????????????") {
                wait until {
                    partialReturnDashboard {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("??????????????????, ?????? ???????????? [?????????????? ?? ??????????] ?? ???????????????????? ?????????????? ??????????????????") {
                orderItemWithName(notReturnableItem.name.unwrap()) {
                    itemAction {
                        isVisible()
                        isDisabled()
                    }
                }
            }

            step("???????????? [?????????????? ?? ????????????]") {
                paymentButton { click() }
            }
        }

        Screen.onScreen<FashionPaymentScreen> {
            step("?????????????????? ???????????????? ???????????? ????????????") {
                wait until {
                    rootSwipeView { isVisible() }
                    contentRecyclerView {
                        isVisible()
                    }
                }
            }

            step("??????????????????, ?????? ???????????????????????? ") {
                step("???????????? [?????????????? ????????????]") {
                    hasPaymentButton()
                }
            }

            step("???????????? ???? ???????????? [?????????????? ????????????]") {
                clickPaymentButton()
            }
        }

        Screen.onScreen<PaymentDrawerScreen> {
            step(
                "?????????????????? ???????????????? ?????????????? ???????????? ???????? ???????????? ?? ???????????? ???????????? \"${
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
                "?????????????????? ?????????????????? ???????????? \"${
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
            step("?????????????????? ???????????????? ???????????? ????????????") {
                wait until {
                    rootSwipeView { isVisible() }
                    contentRecyclerView {
                        isVisible()
                    }
                }
            }

            step("??????????????????, ??????  ") {
                step("???????????????? ?????????????? ???????????? ????????????????") {
                    hasOrderCard(firstOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.paid).lowercase())
                        }
                    }
                }
                step("???????????????? ?????????????? ???????????? ????????????????") {
                    hasOrderCard(secondOrderId.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.paid).lowercase())
                        }
                    }
                }
                step("???????????? [?????????????? ????????????] ??????????????") {
                    hasNoPaymentButton()
                }
                step("?????????????????? ???????????? [?????????????????? ????????????]") {
                    toReturnPackingButton {
                        isCompletelyDisplayed()
                    }
                }
            }

            step("???????????? [?????????????????? ????????????]") {
                toReturnPackingButton {
                    click()
                }
            }

        }

        Screen.onScreen<FashionReturnRegistrationScreen> {
            step("?????????????????? ???????????????? ???????????? ???????????????????????? ???????? ??????????????") {
                contentRecyclerView { isVisible() }
            }

            step("??????????????????, ?????? ???? ???????????? ???????????????????????? ") {
                step("??????????????????") {
                    contentRecyclerView {
                        scrollToStart()
                        firstChild<FashionReturnRegistrationScreen.TitleItem> {
                            title { hasText(R.string.fashion_return_registration_dashboard_title) }
                            subtitle { hasText("0 ???? 2") }
                        }
                    }
                }
                step("???????????? ??????????????") {
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
                step("???????????????? ?????????????? ????????????") {
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
                step("???????????? [?????????????????????? ????????-??????????] ?????? ???????????? ??????????????") {
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

            step("???????????? ???????????? [?????????????????????? ????????-??????????] ?????? ???????????? ??????????????") {
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

        scanWithManualInput(firstSavePackageBarcode, "???????????????? ?????????????? ????????-????????????")

        Screen.onScreen<FashionReturnRegistrationScreen> {
            step("?????????????????? ???????????????? ???????????? ???????????????????????? ???????? ??????????????") {
                wait until {
                    contentRecyclerView { isCompletelyDisplayed() }
                }
            }

            step("??????????????????, ?????? ???? ???????????? ???????????? ?????????????????? ????????-??????????") {
                contentRecyclerView {
                    scrollToStart()
                    firstChild<FashionReturnRegistrationScreen.TitleItem> {
                        title { hasText(R.string.fashion_return_registration_dashboard_title) }
                        subtitle { hasText("1 ???? 2") }
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

            step("??????????????????, ?????? ?????????????????? ???????????? ?????? ???????????????? ???????????? [???????????????? ????????-??????????]") {
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

            step("???????????? ???????????? [?????????????????????? ????????-??????????] ?????? ???????????? ??????????????") {
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

        scanWithManualInput(secondSavePackageBarcode, "???????????????? ?????????????? ????????-????????????")

        Screen.onScreen<FashionReturnRegistrationScreen> {
            step("?????????????????? ???????????????? ???????????? ???????????????????????? ???????? ??????????????") {
                wait until {
                    contentRecyclerView { isCompletelyDisplayed() }
                }
            }

            step("??????????????????, ?????? ???? ???????????? ???????????? ?????????????????? ????????-??????????") {
                contentRecyclerView {
                    scrollToStart()
                    firstChild<FashionReturnRegistrationScreen.TitleItem> {
                        title { hasText(R.string.fashion_return_registration_dashboard_title) }
                        subtitle { hasText("2 ???? 2") }
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

            step("??????????????????, ?????? ?????????????????? ???????????? ?????? ???????????????? ???????????? [???????????????? ????????-??????????]") {
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

            step("??????????????????, ?????? ?????????? ???????????? ?????????????????? ???????????? [?? ???????????????????? ??????????????]") {
                confirmDeliveryButton {
                    isCompletelyDisplayed()
                    hasText(R.string.to_the_next_task)
                }
            }

            step("???????????? ???????????? [?? ???????????????????? ??????????????]") {
                confirmDeliveryButton { click() }
            }
        }

        Screen.onScreen<MainScreen> {
            step("?? ?????????????? ?????????????????? ?????????? ?????????????? ?? ???????????? [?? ???? ??????????]") {
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
            step("?????????????????? ?????????????????? ???????????? ?????????????? ?????????? ??????????????") {
                wait until {
                    manualInputButton { isCompletelyDisplayed() }
                }
            }

            step("???????????? ???? ???????????? ?????????????? ?????????? ??????????????") {
                manualInputButton { click() }
            }

        }

        Screen.onScreen<FashionManualScanScreen> {
            step("?????????????????? ???????????????? ???????????? ?????????????? ??????????") {
                wait until {
                    confirmButton { isVisible() }
                    barcodeInput { isVisible() }
                }
            }

            step("???????????? ?? ???????? ?????????? $whatToInput") {
                barcodeInput {
                    typeText(barcode)
                }
            }

            step("???????????? ???? ???????????? \"????????????\"") {
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