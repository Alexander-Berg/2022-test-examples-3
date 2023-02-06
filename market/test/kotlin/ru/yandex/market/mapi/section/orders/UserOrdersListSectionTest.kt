package ru.yandex.market.mapi.section.orders

import com.yandex.div.dsl.context.CardContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.ResolveUserOrdersFullResponse
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.core.UserExpInfo
import ru.yandex.market.mapi.core.model.action.section.MergeSectionAction
import ru.yandex.market.mapi.core.model.divkit.DivkitSnippet
import ru.yandex.market.mapi.core.model.screen.AbstractSection
import ru.yandex.market.mapi.core.model.screen.ResourceResolver
import ru.yandex.market.mapi.core.model.screen.SectionResource
import ru.yandex.market.mapi.core.util.*
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.orders.OrderListAssembler
import ru.yandex.market.mapi.section.common.orders.OrderListEmptyRenderer
import ru.yandex.market.mapi.section.common.orders.OrderListTemplates
import ru.yandex.market.mapi.section.common.orders.UserOrdersListSection

/**
 * @author Madi Murzabek / madimur@ / 14.06.2022
 */
class UserOrdersListSectionTest : AbstractSectionTest() {

    private val assembler = OrderListAssembler()
    private val resolver = ResolveUserOrdersFullResponse.RESOLVER

    /* TODO add use cases for orders:
    * status UNPAID
    *   - digital
    *   + non-digital
    * status PROCESSING
    *   - DELIVERY
    *      - standard market courier
    *      - deferred courier (hour slot)
    *      + deferred courier (wide slot)
    *      + on demand
    *      - express
    *   - PICKUP
    *      + postomat (locker_code)
    *      + pvz (barcode)
    *   - POST
    * status DELIVERY (tha same as above)
    * status DELIVERED
    *   - digital/non-digital
    * status CANCELLED
    *   - digital
    *   + non-digital
    * */

    @Test
    @DisplayName("Test content result: order list page visible action with experiments")
    fun testContentResultsOrderListPageVisibleWithExps() {
        mockExpInfo(
            UserExpInfo(
                version = "test-version",
                uaasRearrs = linkedSetOf("test-rearr1=0")
            )
        )
        testContentResult(
            fapiResponsePath = "/section/common/orders/fapiResponseOneNonArchivedPage.json",
            expectedPath = "/section/common/orders/contentResultOrderListPageVisibleWithExps.json"
        )
    }

    @Test
    @DisplayName("Test content for DELIVERY/USER_RECEIVED order")
    fun testContentResultsForDeliveryUserReceivedOrder() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/delivery/user_received/fapiResponse.json",
            expectedPath = "/section/common/orders/delivery/user_received/contentResult.json")
    }

    @Test
    @DisplayName("Test content for order with alerts")
    fun testContentResultAlerts() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/delivery/alerts/fapiResponse.json",
            expectedPath = "/section/common/orders/delivery/alerts/contentResult.json")
    }

    @Test
    @DisplayName("Test content for UNPAID order")
    fun testContentResultUnpaid() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/unpaid/fapiResponse.json",
            expectedPath = "/section/common/orders/unpaid/contentResult.json")
    }

    @Test
    @DisplayName("Test content for CANCELLED order")
    fun testContentResultCancelled() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/cancelled/fapiResponse.json",
            expectedPath = "/section/common/orders/cancelled/contentResult.json")
    }

    @Test
    @DisplayName("Test content for PICKUP in market pickup point with barcode")
    fun testContentResultPickupBarcode() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/pickup/barcode/fapiResponse.json",
            expectedPath = "/section/common/orders/pickup/barcode/contentResult.json")
    }

    @Test
    @DisplayName("Test content for DEFERRED_COURIER READY_FOR_LAST_MILE order")
    fun testContentResultDeferredCallCourier() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/delivery/deferred_call_courier/fapiResponse.json",
            expectedPath = "/section/common/orders/delivery/deferred_call_courier/contentResult.json")
    }

    @Test
    @DisplayName("Test content for PICKUP in market postomat with locker code")
    fun testContentResultOnDemandCallCourier() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/pickup/locker_code/fapiResponse.json",
            expectedPath = "/section/common/orders/pickup/locker_code/contentResult.json")
    }

    @Test
    @DisplayName("Test content for ON_DEMAND READY_FOR_LAST_MILE order")
    fun testContentResultPickupLockerCode() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/delivery/on_demand_call_courier/fapiResponse.json",
            expectedPath = "/section/common/orders/delivery/on_demand_call_courier/contentResult.json")
    }

    @Test
    @DisplayName("Test content for ON_DEMAND LAST_MILE_STARTED order")
    fun testContentResultOnDemandLastMileStarted() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/delivery/on_demand_last_mile_started/fapiResponse.json",
            expectedPath = "/section/common/orders/delivery/on_demand_last_mile_started/contentResult.json"
        )

    }

    @Test
    @DisplayName("Test content for DELIVERED with feedback stars")
    fun testContentResultDeliveredFeedbackStartsSection() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/delivered/feedback_stars/fapiResponse.json",
            expectedPath = "/section/common/orders/delivered/feedback_stars/contentResult.json")
    }

    @Test
    @DisplayName("Test content for DELIVERED with feedback question")
    fun testContentResultDeliveredFeedbackQuestionSection() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/delivered/feedback_question/fapiResponse.json",
            expectedPath = "/section/common/orders/delivered/feedback_question/contentResult.json")
    }

    @Test
    @DisplayName("Test content for DBS order with already delivered questionnaire")
    fun testContentResultAlreadyDeliveredQuestionnaire() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/delivery/already_delivered_question/fapiResponse.json",
            expectedPath = "/section/common/orders/delivery/already_delivered_question/contentResult.json")
    }

    @Test
    @DisplayName("Test loadMoreAction on order snippet 1")
    fun testContentResultLoadMoreActionOrderSnippet1() {
        val section = buildWidget().also { section ->
            val loadingSnippet = DivkitSnippet("test-loading-snippet", CardContext())
            val action = MergeSectionAction(
                sectionId = section.id,
                params = daoJsonTyped(
                    "nextTokenPage" to mapOf(
                        "orders.page" to 2,
                        "orders.archived" to false
                    )
                )
            )
            loadingSnippet.actions = daoJsonTyped(
                "LoadMoreOrders_orders.page_2_orders.archived_false_1647341610000" to action
            )
            section.loadingSnippet = loadingSnippet
        }
        testContentResult(
            section = section,
            fapiResponsePath = "/section/common/orders/fapiResponseOneNonArchivedPageOfTwo.json",
            expectedPath = "/section/common/orders/contentResultLoadMoreActionOnSnippet1.json",
            config = daoJsonTree(
                "loadMoreSnippetNum" to 1
            )
        )
    }

    @Test
    @DisplayName("Test no loadMoreAction on order snippets when loadMoreSnippetNum is invalid")
    fun testContentResultLoadMoreSnippetNumInvalid() {
        testContentResult(
            fapiResponsePath = "/section/common/orders/fapiResponseOneNonArchivedPageOfTwo.json",
            expectedPath = "/section/common/orders/contentResultLoadMoreSnippetNumInvalid.json",
            config = daoJsonTree(
                "loadMoreSnippetNum" to 4
            )
        )
    }

    @Test
    @DisplayName("Test content for empty fapi response on non archived and archived orders")
    fun testContentEmptyFapiResponse() {
        testContentResult(
            section = buildWidget().also { section ->
                section.resources = listOf(getSectionResource(1, true))
                section.refreshParams = daoJsonTyped(
                    OrderListAssembler.NON_ARCHIVED_ORDERS_COUNT to 0
                )
            },
            fapiResponsePath = "/section/common/orders/empty/fapiResponse.json",
            expectedPath = "/section/common/orders/empty/contentResult.json",

        )
    }

    @Test
    @DisplayName("Test assembly for empty fapi response on non archived and archived orders")
    fun testAssemblyEmptyFapiResponse() {
        assembler.testAssembly(
            mapOf(resolver to "/section/common/orders/empty/fapiResponse.json"),
            expected = "/section/common/orders/empty/assemblyResult.json",
            section = buildWidget().also { section ->
                section.resources = listOf(getSectionResource(1, true))
                section.refreshParams = daoJsonTyped(
                    OrderListAssembler.NON_ARCHIVED_ORDERS_COUNT to 0
                )
            },
        )
    }

    @Test
    @DisplayName("Test assembly non archived orders is empty (next page token with archived)")
    fun testAssemblyNonArchivedEmptyOrders() {
        assembler.testAssembly(
            mapOf(resolver to "/section/common/orders/empty/fapiResponse.json"),
            expected = "/section/common/orders/assembledNonArchivedEmptyPage.json",
            section = buildWidget().also { section ->
                section.resources = listOf(getSectionResource(1, false))
            }
        )
    }

    @Test
    @DisplayName("Test assembly non archived orders is empty on ios version < 4.6.4 with fake snippet")
    fun testAssemblyNonArchivedEmptyOrdersIosBelow464() {
        mockApp(MapiHeaders.PLATFORM_IOS, "4.6.2")
        assembler.testAssembly(
            mapOf(resolver to "/section/common/orders/empty/fapiResponse.json"),
            expected = "/section/common/orders/assembledNonArchivedEmptyPageIosBelow464.json",
            section = buildWidget().also { section ->
                section.resources = listOf(getSectionResource(1, false))
            }
        )
    }

    private fun getSectionResource(page: Int? = 1, archived: Boolean = false): SectionResource{
        return SectionResource().also { res ->
            res.resolvers = listOf(
                ResourceResolver.simple(
                    ResolveUserOrdersFullResponse.RESOLVER_NAME,
                    // запрашиваем 1 страницу архивных заказов
                    daoJsonTree(
                        "page" to page,
                        "archived" to archived
                    )
                ).also { resolver -> resolver.version = ResolveUserOrdersFullResponse.RESOLVER_VERSION }
            )
        }
    }

    private fun testContentResult(
        section: AbstractSection? = null,
        fapiResponsePath: String,
        expectedPath: String,
        config: Any? = null)
    {
        testContentResult(
            section = section ?: buildWidget(),
            assembler = assembler,
            resolver = buildAnyResolver(),
            resolverResponseMap = mapOf(resolver to fapiResponsePath),
            expected = expectedPath,
            config = config
        )
    }

    @Test
    @DisplayName("Test assembly one non-archive page")
    fun testAssemblyOneNonArchivedPage() {
        assembler.testAssembly(
            mapOf(resolver to "/section/common/orders/fapiResponseOneNonArchivedPage.json"),
            expected = "/section/common/orders/assembledOneNonArchivedPage.json"
        )
    }

    @Test
    @DisplayName("Test assembly one non-archive page of two")
    fun testAssemblyOneNonArchivedPageOfTwo() {
        assembler.testAssembly(
            mapOf(resolver to "/section/common/orders/fapiResponseOneNonArchivedPageOfTwo.json"),
            expected = "/section/common/orders/assembledOneNonArchivedPageOfTwo.json"
        )
    }

    @Test
    @DisplayName("Test assembly second archive page of three")
    fun testAssemblySecondArchivedPageOfThree() {
        assembler.testAssembly(
            mapOf(resolver to "/section/common/orders/fapiResponseSecondArchivedPageOfThree.json"),
            expected = "/section/common/orders/assembledSecondArchivedPageOfThree.json"
        )
    }

    @Test
    @DisplayName("Test assembly last archived page (no page token)")
    fun testAssemblyLastArchivedPage() {
        assembler.testAssembly(
            mapOf(resolver to "/section/common/orders/fapiResponseLastArchivedPage.json"),
            expected = "/section/common/orders/assembledLastArchivedPage.json"
        )
    }

    @Test
    fun testSectionWithPager() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            mapOf(resolver to "/section/common/orders/fapiResponseOneNonArchivedPage.json"),
            expected = "/section/common/orders/sectionResultWithPager.json",
            withLoadMoreAction = true
        )
    }

    @Test
    fun testSectionNoPager() {
        testSectionResult(
            buildWidget(),
            assembler,
            buildAnyResolver(),
            mapOf(resolver to "/section/common/orders/fapiResponseOneNonArchivedPage.json"),
            expected = "/section/common/orders/sectionResultNoPager.json",
            withLoadMoreAction = false
        )
    }

    @Test
    fun testDivkitTemplates() {
        assertJson(OrderListTemplates.templates, "/section/common/orders/templates.json")
    }

    @Test
    @DisplayName("Test templates for empty fapi response")
    fun testTemplatesEmptyFapiResponse() {
        val templates = OrderListEmptyRenderer.render().templates!!
        assertJson(templates, "/section/common/orders/empty/templates.json")
    }

    private fun buildWidget() = UserOrdersListSection().apply {
        id = "my-orders-test-section"
    }
}
