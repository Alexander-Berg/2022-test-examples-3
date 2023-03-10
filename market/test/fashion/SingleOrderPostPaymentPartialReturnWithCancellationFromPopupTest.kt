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
import ru.yandex.market.tpl.courier.arch.fp.requireNotEmpty
import ru.yandex.market.tpl.courier.domain.feature.order.OrderId
import ru.yandex.market.tpl.courier.domain.feature.order.OrderItem
import ru.yandex.market.tpl.courier.domain.feature.task.OrderDeliveryTask
import ru.yandex.market.tpl.courier.domain.feature.task.fashion.Uit
import ru.yandex.market.tpl.courier.presentation.feature.screen.DeliveryScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MainScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.MultiOrderScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.PaymentDrawerScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.CancelPartialReturnPopupScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionClientReturnScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionManualScanScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionPaymentScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionReturnRegistrationScreen
import ru.yandex.market.tpl.courier.presentation.feature.screen.fashion.FashionScanScreen
import ru.yandex.market.tpl.courier.test.BaseTest

@LargeTest
@Epic("Fashion")
@DisplayName("Fashion")
class SingleOrderPostPaymentPartialReturnWithCancellationFromPopupTest : BaseTest() {
    private lateinit var orderIdToDelivery: OrderId
    private lateinit var uitList: List<Uit>
    private var item: OrderItem? = null
    private lateinit var boxBarcodeMask: String
    private val fittingTime: Long = 3

    override fun prepareData() {
        testDataRepository.createShift(uid, 47819L)
        orderIdToDelivery = OrderId(testDataRepository.generateExternalOrderId().requireNotEmpty())
        val orderId =
            testDataRepository.createOrderFromXml(orderIdToDelivery.unwrap(), CREATE_ORDER_XML_FILE)
        runBlocking { delay(200) }
        testDataRepository.updateItemInstances(orderIdToDelivery.unwrap(), UPDATE_ORDER_ITEMS_XML_FILE)
        testDataRepository.reassignOrder(orderId, accountCredentials.get())

        val routePoint = testDataRepository.getFirstDeliveryRoutePoint(accountCredentials.get())
        val task = routePoint.tasks.first() as OrderDeliveryTask
        orderIdToDelivery = task.orderId
        item = task.order.items.find { it.count == 2L }
        uitList = task.order.items.find { it.count == 2L }?.uits ?: emptyList()
        testDataRepository.manualFinishPickup(uid)
        testDataRepository.manualSwitchRoutePoint(routePoint.id, uid)
        testDataRepository.arriveAtRoutePoint(routePoint.id)
        boxBarcodeMask = testDataRepository.getSavePackageBoxBarcode()
    }

    @Test
    @Issue("MARKETTPLAPP-1384")
    @TmsLink("courier-app-199")
    @Story("Fashion")
    @DisplayName("?????????????????? ??????????. ????????????????????. ?????????????????? ??????????????. ???????????? ?????????? ?????????????? ???? ????????????")
    @Description("?????????????????? ??????????. ????????????????????. ?????????????????? ??????????????. ???????????? ?????????? ?????????????? ???? ????????????")
    fun runTest() {
        val startFittingButtonText = getQuantityString(R.plurals.start_x_fitting_minutes, 0) // 3 ?????????????? ?????????? 0 ??????????
        val firstSavePackageBarcode = FashionReturnRegistrationScreen.getRandomBoxBarcode(boxBarcodeMask)

        Screen.onScreen<MainScreen> {
            val drawerOrderItem = viewContainsText(orderIdToDelivery.unwrap())

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

            manuallyScanOrderId(orderIdToDelivery.unwrap())
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

            uitList.forEach {
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

            step("??????????????????, ?????? ?? ?????????????????????????????? ?????????????? ???????? ???????????? [?????????????? ?? ??????????]") {
                orderItemWithName(item?.name?.unwrap() ?: "oops") {
                    itemAction {
                        isVisible()
                    }
                }
            }

            step("???????????????? ???? ???????????? [?????????????? ?? ??????????]") {
                orderItemWithName(item?.name?.unwrap() ?: "oops") {
                    itemAction {
                        click()
                    }
                }
            }
        }

        Screen.onScreen<CancelPartialReturnPopupScreen> {
            step("?????????????????? ???????????????? ????????????") {
                wait until {
                    closeButton {
                        isVisible()
                    }
                }
            }

            step("??????????????????, ??????") {

                step("???????????????????????? ???????????? ???????????????? ????????????") {
                    closeButton { isVisible() }
                }
                step("?? ???????????? ?????????????? ???????? ???????????? ?????????????????????? uit") {
                    uitList.forEach {
                        uit(it.unwrap()) {
                            barcodeView {
                                isCompletelyDisplayed()
                            }
                        }
                    }
                }
                step("?? ???????????? ?????????????? ???????? ???????????? [?????????????? ????????]") {
                    uitList.forEach {
                        uit(it.unwrap()) {
                            cancelReturnButton {
                                isCompletelyDisplayed()
                            }
                        }
                    }
                }
            }

            step("???????????? [?????????????? ????????] ?? ?????????? ??????????????") {
                uit(uitList.first().unwrap()) {
                    cancelReturnButton {
                        click()
                    }
                }
            }

            step("?????????????????? ???????? ?????????? ???????????????? ?? ??????????") {
                wait until {
                    uit(uitList.first().unwrap()) {
                        cancelReturnButton {
                            isGone()
                        }
                        infoView {
                            isVisible()
                        }
                    }
                }
            }
            step("?????????????? ?????????? ?????????? [??????????????]") {
                closeButton.click()
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

            step("??????????????????, ?????? ?????????? ?????????????? ?? ???????? [???????????? ????????????]") {
                contentRecyclerView {
                    hasSize(6)
                }

                title(getResourceString(R.string.purchase_items_title)) {
                    isVisible()
                }
                title(getResourceString(R.string.return_items_title)) {
                    isVisible()
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
                step("???????????????? ???????????? ?? ???????????????????? ??????????") {
                    hasOrderCard(orderIdToDelivery.unwrap()) {
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
            step(
                "?????????????????? ???????????????? ?????????????? ???????????? ???????? ???????????? ?? ???????????? ???????????? \"${
                    getResourceString(
                        R.string.with_credit_card
                    )
                }\""
            ) {
                wait until {
                    creditCardButton { isCompletelyDisplayed() }
                    cashButton { isCompletelyDisplayed() }
                }

                creditCardButton { click() }
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
                step("???????????????? ?????????? ???????????? [????????????????]") {
                    hasOrderCard(orderIdToDelivery.unwrap()) {
                        isVisible()
                        orderStatusView {
                            hasText(getResourceString(R.string.paid).lowercase())
                        }
                        actualPriceView { isVisible() }
                        oldPriceView { isVisible() }
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

            step("??????????????????, ?????? ???? ???????????? ") {
                step("???????? ??????????????????") {
                    contentRecyclerView {
                        scrollToStart()
                        firstChild<FashionReturnRegistrationScreen.TitleItem> {
                            title { hasText(R.string.fashion_return_registration_dashboard_title) }
                            subtitle { hasText("0 ???? 1") }
                        }
                    }
                }
                step("???????? ??????????") {
                    contentRecyclerView {
                        val itemCountText = getQuantityString(R.plurals.refusals, 1)

                        childWith<FashionReturnRegistrationScreen.OrderToReturnItem> {
                            withDescendant { containsText(orderIdToDelivery.unwrap()) }
                        } perform {
                            itemCount { hasText(itemCountText) }
                            safePackageBarcodeWithIndex(0) perform {
                                doesNotExist()
                            }
                        }
                    }
                }
                step("???????? ??????????") {
                    contentRecyclerView {
                        childWith<FashionReturnRegistrationScreen.FashionReturnItem> {
                            withDescendant {
                                withText(item?.name?.unwrap() ?: "oops")
                            }
                        } perform {
                            isVisible()
                        }
                    }
                }
                step("???????? ???????????? [?????????????????????? ????????-??????????]") {
                    contentRecyclerView {
                        childWith<FashionReturnRegistrationScreen.ButtonItem> {
                            withIndex(0) {
                                withText(R.string.partial_return_scan_safe_package)
                            }
                        } perform {
                            isVisible()
                        }
                    }
                }
            }

            step("???????????? ???????????? [?????????????????????? ????????-??????????]") {
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
                        subtitle { hasText("1 ???? 1") }
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
        const val CREATE_ORDER_XML_FILE = "requestData/fashion_two_instances/fashion_order.xml"
        const val UPDATE_ORDER_ITEMS_XML_FILE = "requestData/fashion_two_instances/update_item_instances.xml"
    }
}