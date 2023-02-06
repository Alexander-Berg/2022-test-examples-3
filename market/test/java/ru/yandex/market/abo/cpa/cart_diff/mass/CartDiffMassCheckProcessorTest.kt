package ru.yandex.market.abo.cpa.cart_diff.mass

import java.util.concurrent.ExecutorService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.EmptyTestWithTransactionTemplate
import ru.yandex.market.abo.core.cutoff.CutoffManager
import ru.yandex.market.abo.core.cutoff.feature.FeatureStatusManager
import ru.yandex.market.abo.core.datacamp.client.DataCampClient
import ru.yandex.market.abo.core.exception.ExceptionalShopReason.CART_DIFF
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.core.offer.report.IndexType.MAIN
import ru.yandex.market.abo.core.offer.report.OfferService
import ru.yandex.market.abo.core.partner.info.PartnerInfoService
import ru.yandex.market.abo.core.pilot.PilotPartnerService
import ru.yandex.market.abo.cpa.MbiApiService
import ru.yandex.market.abo.cpa.cart_diff.CartDiffHelper
import ru.yandex.market.abo.cpa.cart_diff.CartDiffService
import ru.yandex.market.abo.cpa.cart_diff.approve.AcceptFailedApprover
import ru.yandex.market.abo.cpa.cart_diff.approve.CartDiffTypeApprover
import ru.yandex.market.abo.cpa.cart_diff.approve.ItemCountApprover
import ru.yandex.market.abo.cpa.cart_diff.diff.CartDiff
import ru.yandex.market.abo.cpa.cart_diff.mass.handler.ApproveCartDiffMassCheckStateHandler
import ru.yandex.market.abo.cpa.cart_diff.mass.handler.CartDiffMassCheckStateHandler
import ru.yandex.market.abo.cpa.cart_diff.mass.handler.RecheckCartDiffMassStateHandler
import ru.yandex.market.abo.cpa.cart_diff.mass.model.CartDiffMassCheckState.APPROVE
import ru.yandex.market.abo.cpa.cart_diff.mass.model.CartDiffMassCheckState.RECHECK
import ru.yandex.market.abo.cpa.cart_diff.mass.model.CartDiffMassCheckTask
import ru.yandex.market.abo.util.db.toggle.ToggleService
import ru.yandex.market.common.report.indexer.IdxAPI
import ru.yandex.market.util.db.ConfigurationService

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.05.2022
 */
class CartDiffMassCheckProcessorTest : EmptyTestWithTransactionTemplate() {

    private val cartDiffMassCheckTaskService: CartDiffMassCheckTaskService = mock()
    private val cartDiffService: CartDiffService = mock()
    private val cachedToggleService: ToggleService = mock()
    private val exceptionalShopsService: ExceptionalShopsService = mock()
    private val offerService: OfferService = mock()
    private val mbiApiService: MbiApiService = mock()
    private val dataCampClient: DataCampClient = mock()
    private val idxApiService: IdxAPI = mock()
    private val cartDiffHelper: CartDiffHelper = mock()
    private val acceptFailedApprover: AcceptFailedApprover = mock()
    private val pilotPartnerService: PilotPartnerService = mock()
    private val itemCountApprover: ItemCountApprover = mock()
    private val cutoffManager: CutoffManager = mock()
    private val coreConfigService: ConfigurationService = mock()
    private val partnerInfoService: PartnerInfoService = mock()
    private val featureStatusManager: FeatureStatusManager = mock()
    private val allApprovers: List<CartDiffTypeApprover> = listOf(itemCountApprover)
    private val approveHandler: ApproveCartDiffMassCheckStateHandler = ApproveCartDiffMassCheckStateHandler(
        cachedToggleService, cartDiffMassCheckTaskService, cutoffManager, "test"
    )
    private val recheckHandler: RecheckCartDiffMassStateHandler = RecheckCartDiffMassStateHandler(
        cachedToggleService, cartDiffMassCheckTaskService, cutoffManager,
        coreConfigService, partnerInfoService, featureStatusManager, transactionTemplate
    )
    private val stateHandlers: List<CartDiffMassCheckStateHandler> = listOf(approveHandler, recheckHandler)
    private var poll: ExecutorService = mock()

    private val cartDiffMassCheckProcessor = CartDiffMassCheckProcessor(
        cartDiffMassCheckTaskService, cartDiffService, cachedToggleService, exceptionalShopsService,
        offerService, mbiApiService, dataCampClient,
        idxApiService, cartDiffHelper, acceptFailedApprover, pilotPartnerService, allApprovers, stateHandlers,
        poll
    )

    private val taskForApprove: CartDiffMassCheckTask = mock {
        on { shopId } doReturn PARTNER_ID_FOR_APPROVE
        on { state } doReturn APPROVE
        on { cartDiffId } doReturn ORIGINAL_DIFF_ID
    }
    private val taskForRecheck: CartDiffMassCheckTask = mock {
        on { shopId } doReturn PARTNER_ID_FOR_RECHECK
        on { state } doReturn RECHECK
        on { cartDiffId } doReturn ORIGINAL_DIFF_ID
    }
    private val originalCartDiff: CartDiff = mock {
        on { id } doReturn ORIGINAL_DIFF_ID
    }

    @BeforeEach
    fun init() {
        doAnswer { invocation ->
            (invocation.getArgument<Any>(0) as Runnable).run()
            null
        }.whenever(poll).execute(any())
        whenever(cartDiffMassCheckTaskService.loadNextTasks(any())).thenReturn(listOf(taskForApprove, taskForRecheck))
        whenever(cartDiffService.load(ORIGINAL_DIFF_ID)).thenReturn(originalCartDiff)
    }

    @Test
    fun `test skip exceptional task`() {
        whenever(exceptionalShopsService.shopHasException(any(), eq(CART_DIFF))).thenReturn(true)
        cartDiffMassCheckProcessor.process()

        verify(cartDiffMassCheckTaskService).delete(taskForApprove)
        verify(cartDiffMassCheckTaskService).delete(taskForRecheck)
    }

    @Test
    fun `test original diff deleted in approve task`() {
        whenever(cartDiffService.load(ORIGINAL_DIFF_ID)).thenReturn(null)
        whenever(cartDiffMassCheckTaskService.loadNextTasks(any())).thenReturn(listOf(taskForApprove))
        cartDiffMassCheckProcessor.process()

        verify(cartDiffMassCheckTaskService).delete(taskForApprove)
    }

    @Test
    fun `test original diff deleted in recheck task`() {
        whenever(cartDiffService.load(ORIGINAL_DIFF_ID)).thenReturn(null)
        whenever(cartDiffMassCheckTaskService.loadNextTasks(any())).thenReturn(listOf(taskForRecheck))
        assertThrows<IllegalStateException> { cartDiffMassCheckProcessor.process() }
    }

    @Test
    fun `test no offers`() {
        whenever(offerService.findWithParams(eq(MAIN), anyVararg())).thenReturn(emptyList())
        cartDiffMassCheckProcessor.process()

        verify(cartDiffMassCheckTaskService).markFinished(taskForApprove)
        verify(cartDiffMassCheckTaskService).updateTriggerTime(taskForRecheck)
    }

    companion object {
        private const val PARTNER_ID_FOR_APPROVE = 123L
        private const val PARTNER_ID_FOR_RECHECK = 124L

        private const val ORIGINAL_DIFF_ID = 11111L
    }
}
