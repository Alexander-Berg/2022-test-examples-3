package ru.yandex.market.mbi.orderservice.api.controller.orders

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer
import ru.yandex.market.checkout.checkouter.client.ClientRole
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason
import ru.yandex.market.checkout.checkouter.order.OrderItem
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance
import ru.yandex.market.checkout.checkouter.order.OrderItemInstances
import ru.yandex.market.checkout.checkouter.order.OrderItems
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemsRemovalChangeRequestPayload
import ru.yandex.market.checkout.checkouter.util.OrderItemInstancesUtil
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.model.yt.Cis
import ru.yandex.market.mbi.orderservice.common.model.yt.ItemsRemovalRequestPayload
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEditRequestEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEditRequestKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.UpdatedItem
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterApiService
import ru.yandex.market.mbi.orderservice.model.ActorType
import ru.yandex.market.mbi.orderservice.model.ChangeOrderLinesCisRequest
import ru.yandex.market.mbi.orderservice.model.ChangeOrderLinesRequest
import ru.yandex.market.mbi.orderservice.model.OrderLineChange
import ru.yandex.market.mbi.orderservice.model.OrderLineCisDto
import ru.yandex.market.mbi.orderservice.model.UpdateOrderReasonType
import java.time.Instant

@CleanupTables(
    [
        OrderEntity::class, OrderLineEntity::class, OrderEditRequestEntity::class
    ]
)
class OrdersLinesControllerTest : FunctionalTest() {

    @Autowired
    lateinit var ytOrderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var changeRequestIdSequence: DataFieldMaxValueIncrementer

    @Autowired
    lateinit var checkouterApiService: CheckouterApiService

    @BeforeEach
    internal fun setUp() {
        this::class.loadTestEntities<OrderEntity>("items/orders.json").let {
            orderEntityRepository.insertRows(it)
        }
        this::class.loadTestEntities<OrderLineEntity>("items/orderLines.json").let {
            orderLineEntityRepository.insertRows(it)
        }
    }

    @Test
    fun `change order lines cis`() {
        val request = listOf(
            Pair(140430845L, listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")),
            Pair(140430846L, listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA=="))
        )

        val checkouterResponse = request.map { (lineId, cisList) ->
            mock<OrderItem> { item ->
                on(item.id).thenReturn(lineId)
                on(item.instances).thenReturn(cisList.map {
                    val cis = Cis(it)
                    OrderItemInstance(cis.getIdentity()).apply { cisFull = cis.value }
                }.let { OrderItemInstancesUtil.convertToNode(it) })
            }
        }.let { OrderItems(it) }
        whenever(
            checkouterApiService.putOrderItemInstances(
                partnerId = eq(543900),
                orderId = eq(81545128),
                items = eq(request.map { OrderItemInstances(it.first, it.second.map { OrderItemInstance(it) }) }),
                actor = any(),
                actorId = any()
            )
        ).thenReturn(checkouterResponse)

        changeLinesCis(
            orderId = 81545128,
            items = request,
            expected = """{
              "result":{
                "partnerId":543900,
                "orderId":81545128,
                "lines":[
                  {"lineId":140430845,"cis":["010460361900008721109679014718691EE0692c29tZV9jcnlwdG9fdGFpbA=="]},
                  {"lineId":140430846,"cis":["010460361900008721109679010011291EE0692c29tZV9jcnlwdG9fdGFpbA=="]}
                ]
              }
            }
            """
        )

        verifyOrderLinesCis(request)
    }

    @Test
    fun `change order lines cis - order not found`() {
        whenever(
            checkouterApiService.putOrderItemInstances(
                partnerId = eq(543900),
                orderId = eq(81545128),
                items = any(),
                actor = any(),
                actorId = any()
            )
        ).thenThrow(OrderNotFoundException(81545128))

        changeLinesCis(
            orderId = 81545128,
            items = listOf(
                Pair(140430845L, listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")),
                Pair(140430846L, listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA=="))
            ),
            expected = """{
              "errors":[
                {
                  "code":"ORDER_NOT_FOUND",
                  "message":"Order not found: 81545128",
                  "details":{"checkouterErrorCode":"ORDER_NOT_FOUND"}
                }
              ]
            }
            """,
            expectedHttpStatus = HttpStatus.SC_NOT_FOUND
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = ChangeRequestStatus::class,
        names = ["NEW", "PROCESSING", "APPLIED"]
    )
    fun `change order line items - async change request processing`(expectedChangeRequestStatus: ChangeRequestStatus) {
        val items = listOf(
            Triple(
                140430846L,
                1,
                listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
            Triple(
                140430845L,
                1,
                listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
        )
        mockCheckouterEditRequest(
            12345,
            HistoryEventReason.ITEMS_NOT_FOUND,
            expectedChangeRequestStatus,
            items
        )

        changeLineItems(
            81545128,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(
                        1,
                        140430846, null,
                        listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                    OrderLineChange(
                        1,
                        140430845, null,
                        listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "result":{
                   "partnerId":543900,
                   "orderId":81545128,
                   "changeRequestStatus":"PROCESSING"
                 }
               }
            """
        )

        verifyOrderEditRequest(
            12345,
            items,
            ChangeRequestStatus.NEW,
            HistoryEventReason.ITEMS_NOT_FOUND
        )
    }

    @Test
    fun `change order line items without lineIds and sskus`() {
        changeLineItems(
            81545128,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(
                        1,
                        140430846, null,
                        listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                    OrderLineChange(
                        1,
                        null, null,
                        listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "errors":[
                   {
                     "code":"BUSINESS_VALIDATION_ERROR",
                     "message":"Either ssku or lineId must be provided",
                     "details":{}
                   }
                 ]
               }
            """,
            expectedHttpStatus = HttpStatus.SC_BAD_REQUEST
        )
    }

    @Test
    fun `change order line items - some lines are missing`() {
        mockCheckouterEditRequest(
            12345,
            HistoryEventReason.ITEMS_NOT_FOUND,
            ChangeRequestStatus.APPLIED,
            listOf(
                Triple(
                    140430845L,
                    1,
                    listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                ),
                Triple(
                    140430846L,
                    0,
                    null
                )
            )
        )

        changeLineItems(
            81545128,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(
                        1,
                        140430845L, null,
                        listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "result":{
                   "partnerId":543900,
                   "orderId":81545128,
                   "changeRequestStatus":"PROCESSING"
                 }
               }
            """
        )

        verifyOrderEditRequest(
            12345,
            listOf(
                Triple(
                    140430845L,
                    1,
                    listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                ),
                Triple(
                    140430846L,
                    0,
                    listOf()
                )
            ),
            ChangeRequestStatus.NEW,
            HistoryEventReason.ITEMS_NOT_FOUND
        )
    }

    @Test
    fun `change sku items - some skus are missing`() {
        val prev = changeRequestIdSequence.nextLongValue()
        changeLineItems(
            1000000000000,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(
                        1,
                        null, "1474180",
                        listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "result":{
                   "partnerId":543900,
                   "orderId":1000000000000,
                   "changeRequestStatus":"PROCESSING",
                   "changeRequestId":${prev + 1}
                 }
               }
            """
        )

        verifyOrderEditRequest(
            prev + 1,
            listOf(
                Triple(
                    140430848L,
                    1,
                    listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                ),
                Triple(
                    140430847L,
                    0,
                    listOf()
                ),
            ),
            ChangeRequestStatus.PROCESSING,
            HistoryEventReason.ITEMS_NOT_FOUND,
            partnerId = 543900,
            orderId = 1000000000000
        )
    }

    @Test
    fun `change sku items - async change request processing`() {
        val items = listOf(
            Triple(
                140430848L,
                1,
                listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
            Triple(
                140430847L,
                1,
                listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
        )

        val prev = changeRequestIdSequence.nextLongValue()

        changeLineItems(
            1000000000000,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(
                        1,
                        null, "1474180",
                        listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                    OrderLineChange(
                        1,
                        null, "390677",
                        listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "result":{
                   "partnerId":543900,
                   "orderId":1000000000000,
                   "changeRequestStatus":"PROCESSING",
                   "changeRequestId":${prev + 1}
                 }
               }
            """
        )

        verifyOrderEditRequest(
            prev + 1,
            items,
            ChangeRequestStatus.PROCESSING,
            HistoryEventReason.ITEMS_NOT_FOUND,
            partnerId = 543900,
            orderId = 1000000000000
        )
    }

    @Test
    fun `change sku items - async change request processing - with retry`() {
        val prev = changeRequestIdSequence.nextLongValue()

        changeLineItems(
            1000000000000,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(
                        1,
                        null, "1474180",
                        listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                    OrderLineChange(
                        1,
                        null, "390677",
                        listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "result":{
                   "partnerId":543900,
                   "orderId":1000000000000,
                   "changeRequestStatus":"PROCESSING",
                   "changeRequestId":${prev + 1},
                   "items":[
                   {"itemId":140430847,"partnerId":543900,"offerName":
                   "Оперативная память Patriot Memory VIPER 3 8 ГБ (4 ГБ x 2) DDR3 1600 МГц DIMM CL9 PV38G160C9K",
                   "shopSku":"390677",
                   "cis":["0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA=="],
                   "cisFull":[],"price":350000,"count":1,"initialCount":3},

                   {"itemId":140430848,"partnerId":543900,
                   "offerName":"Корпус microATX Aerocool Wave-G-BK-v2 Без БП чёрный","shopSku":"1474180",
                   "cis":["0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA=="],"cisFull":[],
                   "price":389000,"count":1,"initialCount":1}]
                 }
               }
            """
        )

        var items = listOf(
            Triple(
                140430848L,
                1,
                listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
            Triple(
                140430847L,
                1,
                listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
        )

        verifyOrderEditRequest(
            prev + 1,
            items,
            ChangeRequestStatus.PROCESSING,
            HistoryEventReason.ITEMS_NOT_FOUND,
            partnerId = 543900,
            orderId = 1000000000000
        )

        changeLineItems(
            1000000000000,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(
                        0,
                        null, "1474180",
                        listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                    OrderLineChange(
                        1,
                        null, "390677",
                        listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA=+")
                    ),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "result":{
                   "partnerId":543900,
                   "orderId":1000000000000,
                   "changeRequestStatus":"PROCESSING",
                   "changeRequestId":${prev + 1},
                   "items":[
                   {"itemId":140430847,"partnerId":543900,"offerName":
                   "Оперативная память Patriot Memory VIPER 3 8 ГБ (4 ГБ x 2) DDR3 1600 МГц DIMM CL9 PV38G160C9K",
                   "shopSku":"390677",
                   "cis":["0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA=="],
                   "cisFull":[],"price":350000,"count":1,"initialCount":3},

                   {"itemId":140430848,"partnerId":543900,
                   "offerName":"Корпус microATX Aerocool Wave-G-BK-v2 Без БП чёрный","shopSku":"1474180",
                   "cis":["0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA=="],"cisFull":[],
                   "price":389000,"count":1,"initialCount":1}]
                 }
               }
            """
        )

        items = listOf(
            Triple(
                140430848L,
                1,
                listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
            Triple(
                140430847L,
                1,
                listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
        )

        verifyOrderEditRequest(
            prev + 1,
            items,
            ChangeRequestStatus.PROCESSING,
            HistoryEventReason.ITEMS_NOT_FOUND,
            partnerId = 543900,
            orderId = 1000000000000
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = ChangeRequestStatus::class,
        names = ["REJECTED", "INVALID"]
    )
    fun `change order line items - invalid change request`(
        expectedChangeRequestStatus: ChangeRequestStatus
    ) {
        val items = listOf(
            Triple(
                140430846L,
                1,
                listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
            Triple(
                140430845L,
                1,
                listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
            ),
        )
        mockCheckouterEditRequest(
            12345,
            HistoryEventReason.ITEMS_NOT_FOUND,
            expectedChangeRequestStatus,
            items
        )

        changeLineItems(
            81545128,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(
                        1, 140430846L, null,
                        listOf("0104603619000087211096790147186\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                    OrderLineChange(
                        1, 140430845L, null,
                        listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "result":{
                   "partnerId":543900,
                   "orderId":81545128,
                   "changeRequestStatus":"REJECTED",
                   "items":[
                    {
                        "itemId":140430845,"partnerId":543900,
                        "offerName":"Оперативная память Patriot Memory VIPER 3 8 ГБ (4 ГБ x 2) DDR3 1600 МГц DIMM CL9 PV38G160C9K",
                        "shopSku":"390677","cis":[],"cisFull":[],"price":350000,"count":3,"initialCount":3
                    },
                    {
                        "itemId":140430846,"partnerId":543900,
                        "offerName":"Корпус microATX Aerocool Wave-G-BK-v2 Без БП чёрный","shopSku":"1474180",
                        "cis":["some_cis"],"cisFull":[],"price":389000,"count":1,"initialCount":1
                    }
                   ]
                 }
               }
            """
        )

        verifyNoOrderEditRequestExists(12345)
    }

    @Test
    fun `change order line items - invalid cis`() {
        changeLineItems(
            81545128,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(
                        1, 140430846L, null,
                        listOf("invalid_cis")
                    ),
                    OrderLineChange(
                        1, 140430845L, null,
                        listOf("0104603619000087211096790100112\u001D91EE06\u001D92c29tZV9jcnlwdG9fdGFpbA==")
                    ),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "errors":[
                   {
                     "code":"CIS_VALIDATION_ERROR",
                     "message":"String invalid_cis is not a valid CIS"
                   }
                 ]
               }
            """,
            expectedHttpStatus = HttpStatus.SC_BAD_REQUEST
        )
    }

    @Test
    fun `change order line items - change request was not created`() {
        whenever(
            checkouterApiService.editOrder(
                partnerId = eq(543900),
                orderId = eq(81545128),
                orderEditRequest = any(),
                actor = any(),
                actorId = any(),
                rgbs = any()
            )
        ).thenReturn(listOf())

        changeLineItems(
            81545128,
            ChangeOrderLinesRequest(
                listOf(
                    OrderLineChange(1, 140430846L, null, null),
                    OrderLineChange(3, 140430845L, null, null),
                ),
                UpdateOrderReasonType.ITEMS_NOT_FOUND
            ),
            """{
                 "result":{
                   "partnerId":543900,
                   "orderId":81545128,
                   "changeRequestStatus":"APPLIED"
                 }
               }
            """
        )
    }

    private fun mockCheckouterEditRequest(
        changeRequestId: Long,
        reason: HistoryEventReason?,
        expectedStatus: ChangeRequestStatus,
        items: List<Triple<Long, Int, List<String>?>>,
        orderId: Long = 81545128
    ) {
        val changeRequest = ChangeRequest(
            changeRequestId,
            orderId,
            ItemsRemovalChangeRequestPayload(
                items.map { (lineId, count, _) ->
                    mock {
                        on(it.id).thenReturn(lineId)
                        on(it.count).thenReturn(count)
                    }
                },
                listOf(mock()),
                reason,
                items.map { (lineId, _, cis) ->
                    OrderItemInstances(
                        lineId,
                        cis?.map { OrderItemInstance(it) }
                    )
                }.toSet()
            ),
            expectedStatus,
            Instant.now(),
            null,
            ClientRole.SHOP_USER
        )
        whenever(
            checkouterApiService.editOrder(
                partnerId = any(),
                orderId = eq(orderId),
                orderEditRequest = any(),
                actor = any(),
                actorId = any(),
                rgbs = any()
            )
        ).thenReturn(listOf(changeRequest))
    }

    private fun verifyOrderLinesCis(expected: List<Pair<Long, List<String>>>) {
        val lines = orderLineEntityRepository.selectRows(
            """
                * from [${orderLineEntityRepository.tablePath}]
                where orderLineId in (${expected.map { it.first }.toSet().joinToString(",")})
            """.trimIndent(),
            allowFullScan = true
        )
            .associateBy { it.key.orderLineId }
        expected.forEach { (lineId, cis) ->
            assertThat(lines[lineId]).isNotNull
                .satisfies {
                    assertThat(it!!.cis).isEqualTo(cis.map { Cis(it).getIdentity() })
                    assertThat(it.identifiers!!.cisFull).isEqualTo(cis.map { Cis(it) }.toSet())
                }
        }
    }

    private fun verifyNoOrderEditRequestExists(
        orderEditRequestId: Long,
        partnerId: Long = 543900,
        orderId: Long = 81545128
    ) {
        assertThat(
            ytOrderRepository.orderEditRequestRepository.lookupRow(
                OrderEditRequestKey(partnerId, orderId, orderEditRequestId)
            )
        ).isNull()
    }

    private fun verifyOrderEditRequest(
        orderEditRequestId: Long,
        expectedItems: List<Triple<Long, Int, List<String>?>>,
        expectedStatus: ChangeRequestStatus,
        expectedReason: HistoryEventReason,
        partnerId: Long = 543900,
        orderId: Long = 81545128
    ) {
        val key = OrderEditRequestKey(partnerId, orderId, orderEditRequestId)
        val actualEditRequest = ytOrderRepository.orderEditRequestRepository.lookupRow(
            key
        )
        assertThat(actualEditRequest).isNotNull
            .usingRecursiveComparison().ignoringFields("createdAt")
            .isEqualTo(
                OrderEditRequestEntity(
                    key = key,
                    type = ChangeRequestType.ITEMS_REMOVAL,
                    status = expectedStatus,
                    itemsRemovalRequestPayload = ItemsRemovalRequestPayload(
                        updatedItems = expectedItems.map { (lineId, count, cis) ->
                            UpdatedItem(lineId, count, cis)
                        },
                        reason = expectedReason
                    )
                )
            )
    }

    private fun changeLinesCis(
        orderId: Long,
        items: List<Pair<Long, List<String>>>,
        expected: String,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK
    ) {
        val params = mapOf(
            "actor" to ActorType.MERCHANT_PI.name,
            "actorId" to "1234"
        )
        val request = HttpPut(getUri("/partners/$partnerId/common/orders/$orderId/cis", params)).apply {
            setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            entity = StringEntity(
                ObjectMapper().writeValueAsString(ChangeOrderLinesCisRequest(lines = items.map {
                    OrderLineCisDto(it.first, it.second)
                })),
                ContentType.APPLICATION_JSON
            )
        }

        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)

        JSONAssert.assertEquals(
            expected,
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    private fun changeLineItems(
        orderId: Long,
        changeOrderLinesRequest: ChangeOrderLinesRequest,
        expected: String,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK
    ) {
        val params = mapOf(
            "actor" to ActorType.MERCHANT_PI.name,
            "actorId" to "1234"
        )
        val request = HttpPost(getUri("/partners/$partnerId/common/orders/$orderId/lines/edit", params)).apply {
            setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            entity = StringEntity(
                ObjectMapper().writeValueAsString(changeOrderLinesRequest),
                ContentType.APPLICATION_JSON
            )
        }

        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)

        JSONAssert.assertEquals(
            expected,
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }
}
