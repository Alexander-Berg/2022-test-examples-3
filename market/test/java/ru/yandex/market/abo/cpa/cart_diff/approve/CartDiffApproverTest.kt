package ru.yandex.market.abo.cpa.cart_diff.approve;

import java.util.Collections
import java.util.Date
import java.util.concurrent.ExecutorService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.common.util.currency.Currency
import ru.yandex.market.abo.core.CoreConfig
import ru.yandex.market.abo.core.hiding.util.filter.CartDiffOfferFilterService
import ru.yandex.market.abo.core.offer.report.Offer
import ru.yandex.market.abo.cpa.cart_diff.CartDiffHelper
import ru.yandex.market.abo.cpa.cart_diff.CartDiffPushApiResponseWrapper
import ru.yandex.market.abo.cpa.cart_diff.CartDiffService
import ru.yandex.market.abo.cpa.cart_diff.CartDiffStatus
import ru.yandex.market.abo.cpa.cart_diff.CartDiffType
import ru.yandex.market.abo.cpa.cart_diff.diff.CartDiff
import ru.yandex.market.abo.cpa.cart_diff.diff.logfields.LogDeliveryAddressInfo
import ru.yandex.market.abo.cpa.cart_diff.diff.logfields.LogOfferInfo
import ru.yandex.market.abo.cpa.cart_diff.diff.logfields.LogOrderInfo
import ru.yandex.market.abo.cpa.cart_diff.mass.CartDiffMassCheckTaskService
import ru.yandex.market.abo.util.db.toggle.DbToggleService
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType
import ru.yandex.market.common.report.model.Color
import ru.yandex.market.common.report.model.Color.BLUE

/**
 * @author artemmz
 *         created on 06.04.17.
 */
class CartDiffApproverTest {

    private var cartDiffService: CartDiffService = mock()
    private var offerFilterService: CartDiffOfferFilterService = mock()
    private var approver: CartDiffTypeApprover = mock()
    private var cartDiffMassCheckTaskService: CartDiffMassCheckTaskService = mock()
    private var cartDiffHelper: CartDiffHelper = mock()
    private var dbToggleService: DbToggleService = mock()
    private var poll: ExecutorService = mock()

    private var cartDiffApprover = CartDiffApprover(
        cartDiffService, offerFilterService, dbToggleService, cartDiffMassCheckTaskService, poll, cartDiffHelper
    )

    private var cartDiff: CartDiff = initCartDiff(BLUE)

    @BeforeEach
    fun setUp() {
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as Runnable).run()
            null
        }.whenever(poll).execute(any())
        whenever(dbToggleService.configDisabledInProduction(CoreConfig.CART_DIFF_MASS_CHECK)).thenReturn(false)
        whenever(approver.approvedType).thenReturn(CART_DIFF_TYPE)
        cartDiffApprover.setApprovers(listOf(approver))

        whenever(cartDiffHelper.cart(any(), any(), any())).thenReturn(CartDiffPushApiResponseWrapper());
    }

    @Test
    fun `unknown cart diff`() {
        cartDiffApprover.setApprovers(ArrayList())
        cartDiffApprover.processDiff(cartDiff)
        assertCheckCancelled();
        verify(cartDiffMassCheckTaskService, never()).createIfNotExists(any())
    }

    @Test
    fun `no cart response`() {
        whenever(cartDiffHelper.cart(any(), any(), any())).thenReturn(null)
        cartDiffApprover.processDiff(cartDiff)
        assertCheckCancelled()
        verify(cartDiffMassCheckTaskService, never()).createIfNotExists(any())
    }

    @Test
    fun `found diff`() {
        whenever(approver.approveDiffAndSaveDetails(any(), any(), any(), any())).thenReturn(true)
        cartDiffApprover.processDiff(cartDiff)
        assertOfferHidden()
        verify(cartDiffMassCheckTaskService).createIfNotExists(cartDiff);
    }

    @Test
    fun `found no diff`() {
        whenever(approver.approveDiffAndSaveDetails(any(), any(), any(), any())).thenReturn(false)
        cartDiffApprover.processDiff(cartDiff)
        assertCheckCancelled()
        verify(cartDiffMassCheckTaskService, never()).createIfNotExists(any())
    }

    private fun assertOfferHidden() {
        assertStatus(CartDiffStatus.APPROVED);
    }

    private fun assertCheckCancelled() {
        assertStatus(CartDiffStatus.CANCELLED)
    }

    private fun assertStatus(status: CartDiffStatus) {
        assertEquals(status, cartDiff.status)
    }

    companion object {
        private val CART_DIFF_TYPE: CartDiffType = CartDiffType.ITEM_DELIVERY

        @JvmStatic
        fun initCartDiff(rgb: Color): CartDiff {
            val cartDiff = CartDiff()
            cartDiff.diffDate = Date()
            cartDiff.shopId = 0
            cartDiff.regionId = 0L
            cartDiff.cartId = 0L
            cartDiff.cmId = "cmId"
            cartDiff.logOfferInfo = LogOfferInfo("offerId", 0L, "ware", 101010L, "shopSku", "offerName", rgb)
            val logOrderInfo = LogOrderInfo(0.0, Currency.RUR, 1, DeliveryType.DELIVERY, "service_name", 0.0, 0.0)
            cartDiff.logUserCartInfo = logOrderInfo
            cartDiff.logShopCartInfo = logOrderInfo
            cartDiff.logDeliveryAddressInfo = LogDeliveryAddressInfo(null, "", "", "", "", "", null)
            cartDiff.type = CART_DIFF_TYPE
            cartDiff.offerRemovedDate = Date(0)
            cartDiff.reportOffer = Offer()

            return cartDiff
        }
    }
}
