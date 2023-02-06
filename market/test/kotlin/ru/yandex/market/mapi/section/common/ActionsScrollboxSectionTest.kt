package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode
import ru.yandex.market.mapi.client.fapi.response.ResolveActualUserOrdersGroupedResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveGrowingCashbackStatusResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveReferralProgramStatusResponse
import ru.yandex.market.mapi.client.fapi.response.ResolveUserOrdersCombinedResponse
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.util.buildMockPlusInfo
import ru.yandex.market.mapi.core.util.mockApp
import ru.yandex.market.mapi.core.util.mockOauth
import ru.yandex.market.mapi.core.util.mockPlusInfo
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.action.ActionsScrollboxSection
import ru.yandex.market.mapi.section.common.action.GrowingCashbackAssembler
import ru.yandex.market.mapi.section.common.action.OrderSnippetAssembler
import ru.yandex.market.mapi.section.common.action.PlusBenefitAssembler
import ru.yandex.market.mapi.section.common.action.ReferralProgramAssembler
import ru.yandex.market.mapi.section.common.action.SoftUpdateSnippetAssembler

/**
 * @author Ilya Kislitsyn / ilyakis@ / 17.03.2022
 */
class ActionsScrollboxSectionTest : AbstractSectionTest() {
    private val assemblerOrder = OrderSnippetAssembler()
    private val assemblerPlus = PlusBenefitAssembler()
    private val assemblerSoftUpdate = SoftUpdateSnippetAssembler()
    private val assemblerGrowingCashback = GrowingCashbackAssembler()
    private val assemblerReferralProgram = ReferralProgramAssembler()

    private val resolverCombine = ResolveUserOrdersCombinedResponse.RESOLVER
    private val resolverGrouped = ResolveActualUserOrdersGroupedResponse.RESOLVER
    private val resolverGrowingCashback = ResolveGrowingCashbackStatusResponse.RESOLVER
    private val resolverReferralProgram = ResolveReferralProgramStatusResponse.RESOLVER

    @BeforeEach
    fun prepareContext() {
        mockOauth("test")
    }

    //TODO widget and snippet interactions

    //order kinds to cover with tests:
    // states:
    // - PENDING (any)
    // - UNPAID (WAITING_TINKOFF_DECISION, AWAIT_PAYMENT, WAITING_USER_DELIVERY_INPUT, other)
    // - PROCESSING (any)
    // - DELIVERY (regular, courier, on_demand, on_demand+delayed - at least one of them with chat)
    // - PICKUP (post_terminal, regular terminal)
    // - delivered (3 cases with feedback + at least 1 without)
    //
    //cover dsbs with tests
    // - not delivered
    // - wait cancellation
    // - any + dsbs + eda (should not be displayed)
    //
    //question if already delivered
    // - delivered
    // - delivering (2+ cases according to code)
    // - also should generate other action if any of delivered checks failed - at leas for one order

    @Test
    fun testOrderAssemblySimple() {
        // + PROCESSING
        // + PENDING (any)
        // + UNPAID (WAITING_TINKOFF_DECISION, AWAIT_PAYMENT, WAITING_USER_DELIVERY_INPUT, other)
        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverCombine to "/section/common/action/orderSimpleFapiResponse.json"
            ),
            expected = "/section/common/action/orderSimpleAssembled.json",
        )
    }

    @Test
    fun testOrderAssemblyDelivered() {
        // delivered to user (pickup, delivered, delivery+received)
        // too old too feedback ignored

        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverCombine to "/section/common/action/orderDeliveredFapiResponse.json"
            ),
            expected = "/section/common/action/orderDeliveredAssembled.json",
        )
    }

    @Test
    fun testOrderAssemblyDelivering() {
        // delivery state (simple)
        // with chat
        // on_demand + last_mile
        // courier tracking (lavka)
        // courier tracking (yandex)

        // exceptions:
        // on_demand + last_mile + delayed (impossible - delayed is not applicable to last_mile)
        // on_demand + delayed - no need to check, processed like simple action

        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverCombine to "/section/common/action/orderDeliveringFapiResponse.json"
            ),
            expected = "/section/common/action/orderDeliveringAssembled.json",
        )
    }

    @Test
    fun testOrderAssemblyPickup() {
        // - PICKUP (post_terminal, regular terminal)
        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverCombine to "/section/common/action/orderPickupFapiResponse.json"
            ),
            expected = "/section/common/action/orderPickupAssembled.json",
        )
    }

    @Test
    fun testOrderAssemblyWithLavka() {
        // lavka
        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverCombine to "/section/common/action/orderWithLavkaFapiResponse.json"
            ),
            expected = "/section/common/action/orderWithLavkaAssembled.json",
            compareMode = JSONCompareMode.STRICT_ORDER
        )
    }

    @Test
    fun testOrderAssemblyDsbsWithProblems() {
        // - not delivered
        // - wait cancellation
        // - any + dsbs + eda (should not be displayed)
        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverCombine to "/section/common/action/orderDsbsFapiResponse.json"
            ),
            expected = "/section/common/action/orderDsbsAssembled.json",
        )
    }

    @Test
    fun testOrderAssemblyAlreadyDeliveredIos() {
        // - delivered
        // - delivering (2+ cases according to code)
        // - ignore dsbs
        mockApp(MapiHeaders.PLATFORM_IOS, "4.5.9")
        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverCombine to "/section/common/action/orderAlreadyDeliveredQuestionFapiResponse.json"
            ),
            expected = "/section/common/action/orderQuestionAssembled.json"
        )
    }

    @Test
    fun testOrderAssemblyAlreadyDeliveredAndroid() {
        // - delivered
        // - delivering (2+ cases according to code)
        // - ignore dsbs
        mockApp(MapiHeaders.PLATFORM_ANDROID, "4.17")
        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverCombine to "/section/common/action/orderAlreadyDeliveredQuestionFapiResponse.json"
            ),
            expected = "/section/common/action/orderQuestionAssembled.json"
        )
    }

    @Test
    fun testSoftUpdate() {
        assemblerSoftUpdate.testAssembly(
            expected = "/section/common/action/softUpdateStaticSnippet.json",
        )
    }

    @Test
    fun testPlusAssembly() {
        assemblerPlus.testAssembly(
            fileMap = mapOf("anyResolver" to "/section/common/action/plusCheckFapiResponse.json"),
            "/section/common/action/plusCheckAssembledWrong.json",
        )
    }

    @Test
    fun testPlusAssemblyGenerated() {
        assemblerPlus.testAssembly(expected = "/section/common/action/plusCheckAssembled.json")
    }

    @Test
    fun testPlusAssemblyMocked() {
        mockPlusInfo(buildMockPlusInfo())
        assemblerPlus.testAssembly(expected = "/section/common/action/plusCheckAssembledAndMocked.json")
    }

    // Testing of new resolver with groups and buttons mapping
    @Test
    fun testOrderGroupedFapiResponse() {
        mockApp(MapiHeaders.PLATFORM_IOS, "4.6.1")
        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverGrouped to "/section/common/action/orderGroupedFapiResponse.json"
            ),
            expected = "/section/common/action/orderGroupedFapiAssembled.json",
        )
    }

    @Test
    fun testOrderGroupedFapiResponseSingleOrderButtons() {
        mockApp(MapiHeaders.PLATFORM_IOS, "4.6.1")
        assemblerOrder.testAssembly(
            fileMap = mapOf(
                resolverGrouped to "/section/common/action/orderGroupedFapiResponseSingleOrderButtons.json"
            ),
            expected = "/section/common/action/orderGroupedFapiSingleOrderButtonsAssembled.json",
        )
    }

    @Test
    fun testGrowingCashbackAvailable() {
        assemblerGrowingCashback.testAssembly(
            fileMap = mapOf(
                resolverGrowingCashback to "/section/common/action/growingCashbackStatusAvailable.json",
            ),
            config = GrowingCashbackAssembler.Config(iconUrl = "https://avatars.mdst.yandex.net/get-marketcms/69442/img-89548a10-8e46-4d73-999a-c7374e32f90d.png/optimize"),
            expected = "/section/common/action/growingCashbackAvailableResponse.json",
        )
    }

    @Test
    fun testGrowingCashbackCompleted() {
        assemblerGrowingCashback.testAssembly(
            fileMap = mapOf(
                resolverGrowingCashback to "/section/common/action/growingCashbackStatusCompleted.json",
            ),
            config = GrowingCashbackAssembler.Config(iconUrl = "https://avatars.mdst.yandex.net/get-marketcms/69442/img-89548a10-8e46-4d73-999a-c7374e32f90d.png/optimize"),
            expected = "/section/common/action/growingCashbackCompletedResponse.json",
        )
    }

    @Test
    fun testGrowingCashbackUnavailable() {
        assemblerGrowingCashback.testAssembly(
            fileMap = mapOf(
                resolverGrowingCashback to "/section/common/action/growingCashbackStatusUnavailable.json",
            ),
            config = GrowingCashbackAssembler.Config(iconUrl = "https://avatars.mdst.yandex.net/get-marketcms/69442/img-89548a10-8e46-4d73-999a-c7374e32f90d.png/optimize"),
            expected = "/section/common/action/emptyContent.json"
        )
    }

    @Test
    fun testReferralProgramActiveWithoutFullReward() {
        assemblerReferralProgram.testAssembly(
            fileMap = mapOf(
                resolverReferralProgram to "/section/common/action/referralProgramAvailable.json",
            ),
            config = GrowingCashbackAssembler.Config(iconUrl = "https://avatars.mdst.yandex.net/get-marketcms/69442/img-89548a10-8e46-4d73-999a-c7374e32f90d.png/optimize"),
            expected = "/section/common/action/referralProgramAvailableResponse.json",
        )
    }

    @Test
    fun testReferralProgramActiveWithFullReward() {
        assemblerReferralProgram.testAssembly(
            fileMap = mapOf(
                resolverReferralProgram to "/section/common/action/referralProgramAvailableGotFullReward.json",
            ),
            config = GrowingCashbackAssembler.Config(iconUrl = "https://avatars.mdst.yandex.net/get-marketcms/69442/img-89548a10-8e46-4d73-999a-c7374e32f90d.png/optimize"),
            expected = "/section/common/action/referralProgramUnavailableResponse.json",
        )
    }

    private fun buildWidget(): ActionsScrollboxSection {
        return ActionsScrollboxSection().apply {
            addDefParams()
        }
    }
}
