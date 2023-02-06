package ru.yandex.market.mbi.orderservice.api.controller.common

import com.google.protobuf.Timestamp
import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer
import ru.yandex.market.checkout.checkouter.client.ClientRole
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason
import ru.yandex.market.checkout.checkouter.order.Color
import ru.yandex.market.checkout.checkouter.order.Order
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType
import ru.yandex.market.checkout.checkouter.order.OrderStatus
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus
import ru.yandex.market.checkout.checkouter.pay.PaymentType
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.logistics.lom.model.enums.PartnerType
import ru.yandex.market.logistics.lom.model.enums.SegmentType
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderSubstatus
import ru.yandex.market.mbi.orderservice.common.enum.OrderSourcePlatform
import ru.yandex.market.mbi.orderservice.common.enum.OrderStockFreezeStatus
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryOption
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryOptionAddress
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryPaymentMethod
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryTimeInterval
import ru.yandex.market.mbi.orderservice.common.model.dto.stocks.StockItem
import ru.yandex.market.mbi.orderservice.common.model.yt.DeliveryDatesChangeRequestPayload
import ru.yandex.market.mbi.orderservice.common.model.yt.MerchantOrderIdIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEditRequestEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEditRequestKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventType
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLogisticsEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderStockFreezeStatusEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderStockFreezeStatusKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OutboundOrderEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.UpdatedDeliveryDates
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEditRequestRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLogisticsEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterApiService
import ru.yandex.market.mbi.orderservice.common.service.external.CombinatorApiService
import ru.yandex.market.mbi.orderservice.common.service.external.GeocoderApiService
import ru.yandex.market.mbi.orderservice.common.service.external.Logistics4ShopsApiService
import ru.yandex.market.mbi.orderservice.common.service.external.StockStorageApiService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEventService
import ru.yandex.market.mbi.orderservice.common.util.toInstantAtMoscowTime
import ru.yandex.market.mbi.orderservice.model.ActorType
import ru.yandex.market.mbi.orderservice.model.ChangeOrderStatus
import ru.yandex.market.mbi.orderservice.model.OrderSubStatus
import ru.yandex.market.mbi.orderservice.model.UpdateOrderReasonType
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderTrait
import ru.yandex.market.personal_market.PersonalMarketService
import ru.yandex.market.personal_market.PersonalRetrieveResponse
import ru.yandex.market.personal_market.client.model.CommonType
import ru.yandex.market.personal_market.client.model.CommonTypeEnum
import ru.yandex.market.personal_market.client.model.GpsCoord
import ru.yandex.market.personal_market.client.model.MultiTypeRetrieveResponseItem
import yandex.market.combinator.v0.CombinatorOuterClass
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date
import java.util.concurrent.CompletableFuture
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryDatesChangeRequestPayload as CheckouterDeliveryDatesChangeRequestPayload

/**
 * Тесты для [CommonOrdersController]
 */

@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        OrderLogisticsEntity::class,
        ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent::class,
        OutboundOrderEventEntity::class,
        OrderStockFreezeStatusEntity::class,
        OrderEditRequestEntity::class,
        MerchantOrderIdIndex::class
    ]
)
@DbUnitDataSet(before = ["regionRepositoryTest.before.csv"])
class CommonOrdersControllerTest : FunctionalTest() {

    @Autowired
    lateinit var combinatorApiService: CombinatorApiService

    @Autowired
    lateinit var geocoderApiService: GeocoderApiService

    @Autowired
    lateinit var stockStorageApiService: StockStorageApiService

    @Autowired
    lateinit var orderIdSequence: DataFieldMaxValueIncrementer

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEventService: OrderEventService

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLogisticsEntityRepository: OrderLogisticsEntityRepository

    @Autowired
    lateinit var checkouterApiService: CheckouterApiService

    @Autowired
    lateinit var orderEditRequestRepository: OrderEditRequestRepository

    @Autowired
    lateinit var logistics4ShopsApiService: Logistics4ShopsApiService

    @Autowired
    lateinit var personalMarketService: PersonalMarketService

    @Autowired
    lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        val orders = this::class.loadTestEntities<OrderEntity>("orders/orders.json")
        orderEntityRepository.insertRows(orders)
        val orderLogistics = this::class.loadTestEntities<OrderLogisticsEntity>("orders/orderLogistics.json")
        orderLogisticsEntityRepository.insertRows(orderLogistics)
    }

    @Test
    fun `single order creation simple test`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 3000
                    )
                )
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.1.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.1.response.json"
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        )
            .satisfies {
                assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.FROZEN)
            }

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).satisfies { assertThat(it!!.orderId).isEqualTo(prev + 1) }

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.status).isEqualTo(MerchantOrderStatus.PROCESSING)
                assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.STARTED)
                assertThat(it.merchantOrderId).isEqualTo("ozon_id")
                assertThat(it.sourcePlatform).isEqualTo(OrderSourcePlatform.OZON)
                assertThat(it.buyerCurrency).isEqualTo("RUB")
                assertThat(it.color).isEqualTo(Color.BLUE)
                assertThat(it.isFake).isEqualTo(false)
                assertThat(it.isFulfillment).isEqualTo(true)
                assertThat(it.paymentType).isEqualTo(PaymentType.PREPAID)
                assertThat(it.notes).isEqualTo("Позвонить за час до приезда")
                assertThat(it.traits).contains(
                    ru.yandex.market.mbi.orderservice.common.enum.OrderTrait.ORDER_EDIT_ALLOWED
                )

                assertThat(
                    orderRepository.orderLineEntityRepository.lookupRow(
                        OrderLineKey(
                            123,
                            prev + 1,
                            it.lineIds?.first()!!
                        )
                    )
                ).satisfies { line ->
                    assertThat(line!!.price).isEqualTo(800)
                    assertThat(line.msku).isEqualTo(453525252)
                    assertThat(line.categoryId).isEqualTo(9393)
                    assertThat(line.feedId).isEqualTo(35442)
                    assertThat(line.warehouseId).isEqualTo(172)
                    assertThat(line.ffWarehouseId).isEqualTo(172)
                    assertThat(line.shopSku).isEqualTo("test_tovar")
                    assertThat(line.initialCount).isEqualTo(10)
                    assertThat(line.cargoTypes).containsAll(listOf(969, 933, 922))
                    assertThat(line.offerId).isEqualTo("test_tovar")
                    assertThat(line.offerName).isEqualTo("tovar test")
                }
            }

        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.deliveryServiceId).isEqualTo(23242424)
                assertThat(it.deliveryRegionId).isEqualTo(213)

                assertThat(it.address).satisfies { address ->
                    assertThat(address!!.city).isEqualTo("Москва")
                    assertThat(address.street).isEqualTo("Измайловский проспект")
                    assertThat(address.house).isEqualTo("73/2")
                    assertThat(address.postcode).isEqualTo("12324")
                    assertThat(address.apartment).isEqualTo("12")
                }

                assertThat(it.buyer).satisfies { buyer ->
                    assertThat(buyer!!.email).isEqualTo("order-serv@mail.mbi")
                    assertThat(buyer.firstName).isEqualTo("Сервис")
                    assertThat(buyer.middleName).isEqualTo("Заказов")
                    assertThat(buyer.lastName).isEqualTo("Мбиайович")
                    assertThat(buyer.phone).isEqualTo("88005553535")
                    assertThat(buyer.personalFullnameId).isEqualTo("woehf928t6f23f298y")
                    assertThat(buyer.personalPhoneId).isEqualTo("1b3937f6f41153beb2f81a0e01b")
                    assertThat(buyer.personalEmailId).isEqualTo("610ca935e220e19d69891496206")
                }
            }

        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1)))
            .singleElement()
            .satisfies { event ->
                assertThat(event.type).isEqualTo(OrderEventType.EXTERNAL_ORDER_CREATED)
            }

        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).satisfies { events ->
            assertThat(events).hasSize(1)

            assertThat(events).first().satisfies {
                assertThat(it.orderId).isEqualTo(prev + 1)
                assertThat(it.processed).isFalse
                assertThat(it.eventPayloadType).isEqualTo(OrderEvent.PayloadCase.NEW_EXTERNAL_ORDER_CREATED_PAYLOAD.name)
                assertThat(OrderEvent.parseFrom(it.eventPayload)).satisfies { protoEvent ->
                    assertThat(protoEvent.id).isNotNull.isNotEqualTo(0L)
                    assertThat(protoEvent.createdAt).isNotNull.isNotEqualTo(Timestamp.getDefaultInstance())
                    assertThat(protoEvent.traceId).isNotEmpty
                    assertThat(protoEvent.newExternalOrderCreatedPayload).satisfies { payload ->
                        assertThat(payload.orderKey).extracting("shopId", "orderId").containsExactly(123L, prev + 1)
                        assertThat(payload.notes).isEqualTo("Позвонить за час до приезда")
                        assertThat(payload.traitsList).contains(OrderTrait.ORDER_EDIT_ALLOWED)
                        assertThat(payload.recipient).extracting(
                            "firstName",
                            "middleName",
                            "lastName",
                            "phone",
                            "email",
                            "personalFullnameId",
                            "personalPhoneId",
                            "personalEmailId"
                        )
                            .containsExactly(
                                "Сервис",
                                "Заказов",
                                "Мбиайович",
                                "88005553535",
                                "order-serv@mail.mbi",
                                "woehf928t6f23f298y",
                                "1b3937f6f41153beb2f81a0e01b",
                                "610ca935e220e19d69891496206"
                            )
                    }
                }
            }
        }
    }

    @Test
    fun `check delivery address verification`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(listOf())
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 3000
                    )
                )
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.2.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.2.response.json"
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        ).isNull()

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).isNull()

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1))).isEmpty()
        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).isEmpty()
    }

    @Test
    fun `verify that stock exists`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf()
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.3.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.3.response.json"
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        ).isNull()

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).isNull()

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1))).isEmpty()
        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).isEmpty()
    }

    @Test
    fun `check stock availability`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 1
                    )
                )
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.4.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.4.response.json"
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        ).isNull()

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1))).isEmpty()
        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).isEmpty()
    }

    @Test
    fun `verify request with multiple order lines`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar1",
                        warehouseId = 677,
                        count = 3000
                    ),
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar2",
                        warehouseId = 123,
                        count = 444
                    )
                )
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.5.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.5.response.json"
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        )
            .satisfies {
                assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.FROZEN)
            }

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).satisfies { assertThat(it!!.orderId).isEqualTo(prev + 1) }

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.status).isEqualTo(MerchantOrderStatus.PROCESSING)
                assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.STARTED)
                assertThat(it.merchantOrderId).isEqualTo("ozon_id")
                assertThat(it.sourcePlatform).isEqualTo(OrderSourcePlatform.OZON)
                assertThat(it.buyerCurrency).isEqualTo("RUB")
                assertThat(it.color).isEqualTo(Color.BLUE)
                assertThat(it.isFake).isEqualTo(false)
                assertThat(it.isFulfillment).isEqualTo(true)
                assertThat(it.paymentType).isEqualTo(PaymentType.PREPAID)
                assertThat(it.notes).isEqualTo("Позвонить за час до приезда")

                assertThat(
                    orderRepository.orderLineEntityRepository.lookupRow(
                        OrderLineKey(
                            123,
                            prev + 1,
                            it.lineIds?.first()!!
                        )
                    )
                ).satisfies { line ->
                    assertThat(line!!.price).isEqualTo(800)
                    assertThat(line.msku).isEqualTo(453525252)
                    assertThat(line.categoryId).isEqualTo(9393)
                    assertThat(line.feedId).isEqualTo(35442)
                    assertThat(line.warehouseId).isEqualTo(172)
                    assertThat(line.ffWarehouseId).isEqualTo(172)
                    assertThat(line.shopSku).isEqualTo("test_tovar1")
                    assertThat(line.initialCount).isEqualTo(10)
                    assertThat(line.cargoTypes).containsAll(listOf(969, 933, 922))
                    assertThat(line.offerId).isEqualTo("test_tovar1")
                    assertThat(line.offerName).isEqualTo("tovar test")
                }
            }

        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.deliveryServiceId).isEqualTo(23242424)
                assertThat(it.deliveryRegionId).isEqualTo(213)

                assertThat(it.address).satisfies { address ->
                    assertThat(address!!.city).isEqualTo("Москва")
                    assertThat(address.street).isEqualTo("Измайловский проспект")
                    assertThat(address.house).isEqualTo("73/2")
                    assertThat(address.postcode).isEqualTo("12324")
                }

                assertThat(it.buyer).satisfies { buyer ->
                    assertThat(buyer!!.email).isEqualTo("order-serv@mail.mbi")
                    assertThat(buyer.firstName).isEqualTo("Сервис")
                    assertThat(buyer.middleName).isEqualTo("Заказов")
                    assertThat(buyer.lastName).isEqualTo("Мбиайович")
                    assertThat(buyer.phone).isEqualTo("88005553535")
                }
            }

        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1)))
            .singleElement()
            .satisfies { event ->
                assertThat(event.type).isEqualTo(OrderEventType.EXTERNAL_ORDER_CREATED)
            }

        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).satisfies { events ->
            assertThat(events).hasSize(1)

            assertThat(events).first().satisfies {
                assertThat(it.orderId).isEqualTo(prev + 1)
                assertThat(it.processed).isFalse
                assertThat(it.eventPayloadType).isEqualTo(OrderEvent.PayloadCase.NEW_EXTERNAL_ORDER_CREATED_PAYLOAD.name)
                assertThat(OrderEvent.parseFrom(it.eventPayload)).satisfies { protoEvent ->
                    assertThat(protoEvent.id).isNotNull.isNotEqualTo(0L)
                    assertThat(protoEvent.createdAt).isNotNull.isNotEqualTo(Timestamp.getDefaultInstance())
                    assertThat(protoEvent.traceId).isNotEmpty
                    assertThat(protoEvent.newExternalOrderCreatedPayload).satisfies { payload ->
                        assertThat(payload.orderKey).extracting("shopId", "orderId").containsExactly(123L, prev + 1)
                        assertThat(payload.notes).isEqualTo("Позвонить за час до приезда")
                        assertThat(payload.recipient).extracting(
                            "firstName",
                            "middleName",
                            "lastName",
                            "phone",
                            "email"
                        )
                            .containsExactly("Сервис", "Заказов", "Мбиайович", "88005553535", "order-serv@mail.mbi")
                    }
                }
            }
        }
    }

    @Test
    fun `check phone verification`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 3000
                    )
                )
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.6.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.6.response.json",
            HttpStatus.SC_BAD_REQUEST
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 2)
            )
        ).isNull()

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).isNull()

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1))).isEmpty()
        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).isEmpty()
    }

    @Test
    fun `check email verification`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 3000
                    )
                )
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.7.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.7.response.json",
            HttpStatus.SC_BAD_REQUEST
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        ).isNull()

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).isNull()

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1))).isEmpty()
        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).isEmpty()
    }

    @Test
    fun `check source platfoem verification`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 3000
                    )
                )
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.8.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.8.response.json",
            HttpStatus.SC_BAD_REQUEST
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        ).isNull()

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).isNull()

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1))).isNull()
        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1))).isEmpty()
        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).isEmpty()
    }

    @Test
    fun `check duplicate merchant order verification`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 3000
                    )
                )
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.9.request.json"
            )
        )

        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.9.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.9.response.json",
            HttpStatus.SC_BAD_REQUEST
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        )
            .satisfies {
                assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.FROZEN)
            }

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).satisfies { assertThat(it!!.orderId).isEqualTo(prev + 1) }

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.status).isEqualTo(MerchantOrderStatus.PROCESSING)
                assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.STARTED)
                assertThat(it.merchantOrderId).isEqualTo("ozon_id")
                assertThat(it.sourcePlatform).isEqualTo(OrderSourcePlatform.OZON)
                assertThat(it.buyerCurrency).isEqualTo("RUB")
                assertThat(it.color).isEqualTo(Color.BLUE)
                assertThat(it.isFake).isEqualTo(false)
                assertThat(it.isFulfillment).isEqualTo(true)
                assertThat(it.paymentType).isEqualTo(PaymentType.PREPAID)
                assertThat(it.notes).isEqualTo("Позвонить за час до приезда")

                assertThat(
                    orderRepository.orderLineEntityRepository.lookupRow(
                        OrderLineKey(
                            123,
                            prev + 1,
                            it.lineIds?.first()!!
                        )
                    )
                ).satisfies { line ->
                    assertThat(line!!.price).isEqualTo(800)
                    assertThat(line.msku).isEqualTo(453525252)
                    assertThat(line.categoryId).isEqualTo(9393)
                    assertThat(line.feedId).isEqualTo(35442)
                    assertThat(line.warehouseId).isEqualTo(172)
                    assertThat(line.ffWarehouseId).isEqualTo(172)
                    assertThat(line.shopSku).isEqualTo("test_tovar")
                    assertThat(line.initialCount).isEqualTo(10)
                    assertThat(line.cargoTypes).containsAll(listOf(969, 933, 922))
                    assertThat(line.offerId).isEqualTo("test_tovar")
                    assertThat(line.offerName).isEqualTo("tovar test")
                }
            }

        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.deliveryServiceId).isEqualTo(23242424)
                assertThat(it.deliveryRegionId).isEqualTo(213)

                assertThat(it.address).satisfies { address ->
                    assertThat(address!!.city).isEqualTo("Москва")
                    assertThat(address.street).isEqualTo("Измайловский проспект")
                    assertThat(address.house).isEqualTo("73/2")
                    assertThat(address.postcode).isEqualTo("12324")
                }

                assertThat(it.buyer).satisfies { buyer ->
                    assertThat(buyer!!.email).isEqualTo("order-serv@mail.mbi")
                    assertThat(buyer.firstName).isEqualTo("Сервис")
                    assertThat(buyer.middleName).isEqualTo("Заказов")
                    assertThat(buyer.lastName).isEqualTo("Мбиайович")
                    assertThat(buyer.phone).isEqualTo("88005553535")
                }
            }

        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1)))
            .singleElement()
            .satisfies { event ->
                assertThat(event.type).isEqualTo(OrderEventType.EXTERNAL_ORDER_CREATED)
            }

        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).satisfies { events ->
            assertThat(events).hasSize(1)

            assertThat(events).first().satisfies {
                assertThat(it.orderId).isEqualTo(prev + 1)
                assertThat(it.processed).isFalse
                assertThat(it.eventPayloadType).isEqualTo(OrderEvent.PayloadCase.NEW_EXTERNAL_ORDER_CREATED_PAYLOAD.name)
                assertThat(OrderEvent.parseFrom(it.eventPayload)).satisfies { protoEvent ->
                    assertThat(protoEvent.id).isNotNull.isNotEqualTo(0L)
                    assertThat(protoEvent.createdAt).isNotNull.isNotEqualTo(Timestamp.getDefaultInstance())
                    assertThat(protoEvent.traceId).isNotEmpty
                    assertThat(protoEvent.newExternalOrderCreatedPayload).satisfies { payload ->
                        assertThat(payload.orderKey).extracting("shopId", "orderId").containsExactly(123L, prev + 1)
                        assertThat(payload.notes).isEqualTo("Позвонить за час до приезда")
                        assertThat(payload.recipient).extracting(
                            "firstName",
                            "middleName",
                            "lastName",
                            "phone",
                            "email"
                        )
                            .containsExactly("Сервис", "Заказов", "Мбиайович", "88005553535", "order-serv@mail.mbi")
                    }
                }
            }
        }
    }

    @Test
    fun `verify stock stoarage error handling`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 3000
                    )
                )
            )
        whenever(stockStorageApiService.freezeStocks(any(), any(), any()))
            .thenThrow(IllegalArgumentException())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.10.request.json"
            )
        )

        assertThat(result.statusLine.statusCode).isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        ).satisfies {
            assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.STARTED)
        }

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).isNull()

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.status).isEqualTo(MerchantOrderStatus.PLACING)
                assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.UNKNOWN)
                assertThat(it.merchantOrderId).isEqualTo("ozon_id")
                assertThat(it.sourcePlatform).isEqualTo(OrderSourcePlatform.OZON)
                assertThat(it.buyerCurrency).isEqualTo("RUB")
                assertThat(it.color).isEqualTo(Color.BLUE)
                assertThat(it.isFake).isEqualTo(false)
                assertThat(it.isFulfillment).isEqualTo(true)
                assertThat(it.paymentType).isEqualTo(PaymentType.PREPAID)
                assertThat(it.notes).isEqualTo("Позвонить за час до приезда")

                assertThat(
                    orderRepository.orderLineEntityRepository.lookupRow(
                        OrderLineKey(
                            123,
                            prev + 1,
                            it.lineIds?.first()!!
                        )
                    )
                ).satisfies { line ->
                    assertThat(line!!.price).isEqualTo(800)
                    assertThat(line.msku).isEqualTo(453525252)
                    assertThat(line.categoryId).isEqualTo(9393)
                    assertThat(line.feedId).isEqualTo(35442)
                    assertThat(line.warehouseId).isEqualTo(172)
                    assertThat(line.ffWarehouseId).isEqualTo(172)
                    assertThat(line.shopSku).isEqualTo("test_tovar")
                    assertThat(line.initialCount).isEqualTo(10)
                    assertThat(line.cargoTypes).containsAll(listOf(969, 933, 922))
                    assertThat(line.offerId).isEqualTo("test_tovar")
                    assertThat(line.offerName).isEqualTo("tovar test")
                }
            }

        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.deliveryServiceId).isEqualTo(23242424)
                assertThat(it.deliveryRegionId).isEqualTo(213)

                assertThat(it.address).satisfies { address ->
                    assertThat(address!!.city).isEqualTo("Москва")
                    assertThat(address.street).isEqualTo("Измайловский проспект")
                    assertThat(address.house).isEqualTo("73/2")
                    assertThat(address.postcode).isEqualTo("12324")
                }

                assertThat(it.buyer).satisfies { buyer ->
                    assertThat(buyer!!.email).isEqualTo("order-serv@mail.mbi")
                    assertThat(buyer.firstName).isEqualTo("Сервис")
                    assertThat(buyer.middleName).isEqualTo("Заказов")
                    assertThat(buyer.lastName).isEqualTo("Мбиайович")
                    assertThat(buyer.phone).isEqualTo("88005553535")
                }
            }

        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1)))
            .isEmpty()
        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).isEmpty()
    }

    @Test
    fun `verify delivery to different timezone`() {
        doReturn(false).whenever(logistics4ShopsApiService).validateOrderItemsRemoval(any())
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 64,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 3000
                    )
                )
            )
        whenever(stockStorageApiService.freezeStocks(any(), any(), any()))
            .thenThrow(IllegalArgumentException())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(
                        CombinatorOuterClass.Route.newBuilder()
                            .addAllPoints(
                                listOf(
                                    CombinatorOuterClass.Route.Point.newBuilder()
                                        .setIds(
                                            CombinatorOuterClass.PointIds.newBuilder()
                                                .setRegionId(64)
                                                .setPartnerId(172)
                                                .build()
                                        )
                                        .setPartnerType(PartnerType.DELIVERY.name)
                                        .setSegmentType(SegmentType.PICKUP.name)
                                        .build()
                                )
                            )
                            .build()
                    )
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.10.request.json"
            )
        )

        assertThat(result.statusLine.statusCode).isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)
        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        ).satisfies {
            assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.STARTED)
        }

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).isNull()

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.status).isEqualTo(MerchantOrderStatus.PLACING)
                assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.UNKNOWN)
                assertThat(it.merchantOrderId).isEqualTo("ozon_id")
                assertThat(it.sourcePlatform).isEqualTo(OrderSourcePlatform.OZON)
                assertThat(it.buyerCurrency).isEqualTo("RUB")
                assertThat(it.color).isEqualTo(Color.BLUE)
                assertThat(it.isFake).isEqualTo(false)
                assertThat(it.isFulfillment).isEqualTo(true)
                assertThat(it.paymentType).isEqualTo(PaymentType.PREPAID)
                assertThat(it.notes).isEqualTo("Позвонить за час до приезда")

                assertThat(
                    orderRepository.orderLineEntityRepository.lookupRow(
                        OrderLineKey(
                            123,
                            prev + 1,
                            it.lineIds?.first()!!
                        )
                    )
                ).satisfies { line ->
                    assertThat(line!!.price).isEqualTo(800)
                    assertThat(line.msku).isEqualTo(453525252)
                    assertThat(line.categoryId).isEqualTo(9393)
                    assertThat(line.feedId).isEqualTo(35442)
                    assertThat(line.warehouseId).isEqualTo(172)
                    assertThat(line.ffWarehouseId).isEqualTo(172)
                    assertThat(line.shopSku).isEqualTo("test_tovar")
                    assertThat(line.initialCount).isEqualTo(10)
                    assertThat(line.cargoTypes).containsAll(listOf(969, 933, 922))
                    assertThat(line.offerId).isEqualTo("test_tovar")
                    assertThat(line.offerName).isEqualTo("tovar test")
                    assertThat(line.pictureUrl).isEqualTo("picture url")
                }
            }

        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.deliveryServiceId).isEqualTo(23242424)
                assertThat(it.deliveryRegionId).isEqualTo(64)
                assertThat(it.deliveryFromTimestamp!!.toEpochMilli()).isEqualTo(1649988000000L)
                assertThat(it.deliveryToTimestamp!!.toEpochMilli()).isEqualTo(1650204000000L)

                assertThat(it.address).satisfies { address ->
                    assertThat(address!!.city).isEqualTo("Москва")
                    assertThat(address.street).isEqualTo("Измайловский проспект")
                    assertThat(address.house).isEqualTo("73/2")
                    assertThat(address.postcode).isEqualTo("12324")
                }

                assertThat(it.buyer).satisfies { buyer ->
                    assertThat(buyer!!.email).isEqualTo("order-serv@mail.mbi")
                    assertThat(buyer.firstName).isEqualTo("Сервис")
                    assertThat(buyer.middleName).isEqualTo("Заказов")
                    assertThat(buyer.lastName).isEqualTo("Мбиайович")
                    assertThat(buyer.phone).isEqualTo("88005553535")
                }
            }

        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1)))
            .isEmpty()
        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).isEmpty()
    }

    @Test
    fun `verify that real delivery date is saved`() {
        whenever(clock.instant()).thenReturn(Instant.now())
        val newRealDeliveryDate = Instant.parse("2021-12-15T16:30:00Z")

        val checkouterResponse = mock<Order> {
            on(it.status).thenReturn(OrderStatus.DELIVERED)
            on(it.substatus).thenReturn(OrderSubstatus.DELIVERED_USER_RECEIVED)
            on(it.getProperty(OrderPropertyType.REAL_DELIVERY_DATE)).thenReturn(Date.from(newRealDeliveryDate))
        }
        whenever(
            checkouterApiService.updateOrderStatusAndSaveRealDeliveryDate(
                partnerId = eq(543900),
                orderId = eq(81545127),
                status = eq(MerchantOrderStatus.DELIVERED),
                substatus = eq(MerchantOrderSubstatus.DELIVERED_USER_RECEIVED),
                realDeliveryDate = eq(newRealDeliveryDate),
                actor = any(),
                actorId = any()
            )
        ).thenReturn(checkouterResponse)

        saveRealDeliveryDate(
            81545127,
            ChangeOrderStatus.DELIVERED,
            OrderSubStatus.DELIVERED_USER_RECEIVED,
            newRealDeliveryDate,
            """{
                  "result":{
                    "partnerId":543900,
                    "orderId":81545127,
                    "status":"DELIVERED",
                    "substatus":"DELIVERED_USER_RECEIVED",
                    "realDeliveryDate":"2021-12-15T19:30:00+03:00"
                  }
                }
            """
        )

        assertThat(orderLogisticsEntityRepository.lookupRow(OrderKey(543900, 81545127)))
            .isNotNull
            .satisfies {
                assertThat(it!!.realDeliveryDate).isEqualTo(newRealDeliveryDate)
            }
        assertThat(orderEntityRepository.lookupRow(OrderKey(543900, 81545127)))
            .isNotNull
            .satisfies {
                assertThat(it!!.status).isEqualTo(MerchantOrderStatus.DELIVERED)
                assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.DELIVERED_USER_RECEIVED)
            }
        verify(checkouterApiService, never()).updateOrderStatus(
            partnerId = any(),
            orderId = any(),
            status = any(),
            substatus = any(),
            actor = any(),
            actorId = any()
        )
    }

    @Test
    fun `verify that delivery dates is saved - instant`() {
        val fromDate = LocalDate.parse("2021-12-16")
        val toDate = LocalDate.parse("2021-12-17")

        mockCheckouterRequest(
            fromDate, toDate, HistoryEventReason.USER_MOVED_DELIVERY_DATES, 12345,
            ChangeRequestStatus.APPLIED
        )

        saveDeliveryDates(
            81545127,
            fromDate,
            toDate,
            UpdateOrderReasonType.USER_MOVED_DELIVERY_DATES,
            """{
                  "result": {
                    "partnerId": 543900,
                    "orderId": 81545127,
                    "changeRequestStatus": "APPLIED"
                  }
                }
            """.trimIndent()
        )

        assertThat(orderEditRequestRepository.lookupRow(OrderEditRequestKey(543900, 81545127, 12345))).isNull()
        assertThat(orderLogisticsEntityRepository.lookupRow(OrderKey(543900, 81545127)))
            .isNotNull
            .satisfies {
                assertThat(it!!.deliveryFromTimestamp).isEqualTo(Instant.parse("2021-12-16T14:00:00Z"))
                assertThat(it.deliveryToTimestamp).isEqualTo(Instant.parse("2021-12-17T17:00:00Z"))
            }
    }

    @Test
    fun `verify that delivery dates is saved - async`() {
        val fromDate = LocalDate.parse("2021-12-16")
        val toDate = LocalDate.parse("2021-12-17")

        mockCheckouterRequest(
            fromDate, toDate, HistoryEventReason.USER_MOVED_DELIVERY_DATES, 12345,
            ChangeRequestStatus.PROCESSING
        )

        saveDeliveryDates(
            81545127,
            fromDate,
            toDate,
            UpdateOrderReasonType.USER_MOVED_DELIVERY_DATES,
            """{
                  "result": {
                    "partnerId": 543900,
                    "orderId": 81545127,
                    "changeRequestStatus": "PROCESSING"
                  }
                }
            """.trimIndent()
        )
        assertThat(orderEditRequestRepository.lookupRow(OrderEditRequestKey(543900, 81545127, 12345)))
            .isNotNull
            .satisfies {
                assertThat(it!!.status).isEqualTo(ChangeRequestStatus.PROCESSING)
                assertThat(it.deliveryDatesChangeRequestPayload).isEqualTo(
                    DeliveryDatesChangeRequestPayload(
                        UpdatedDeliveryDates(
                            fromDate.toInstantAtMoscowTime(),
                            toDate.toInstantAtMoscowTime()
                        ),
                        HistoryEventReason.USER_MOVED_DELIVERY_DATES
                    )
                )
            }
        assertThat(orderLogisticsEntityRepository.lookupRow(OrderKey(543900, 81545127)))
            .isNotNull
            .satisfies {
                assertThat(it!!.deliveryFromTimestamp).isEqualTo(Instant.parse("2021-12-14T14:00:00Z"))
                assertThat(it.deliveryToTimestamp).isEqualTo(Instant.parse("2021-12-15T17:00:00Z"))
            }
    }

    @Test
    fun `verify that delivery dates is saved - rejected`() {
        val fromDate = LocalDate.parse("2021-12-16")
        val toDate = LocalDate.parse("2021-12-17")

        mockCheckouterRequest(
            fromDate, toDate, HistoryEventReason.USER_MOVED_DELIVERY_DATES, 12345,
            ChangeRequestStatus.REJECTED
        )

        saveDeliveryDates(
            81545127,
            fromDate,
            toDate,
            UpdateOrderReasonType.USER_MOVED_DELIVERY_DATES,
            """{
                  "result": {
                    "partnerId": 543900,
                    "orderId": 81545127,
                    "changeRequestStatus": "REJECTED"
                  }
                }
            """.trimIndent()
        )
        assertThat(orderEditRequestRepository.lookupRow(OrderEditRequestKey(543900, 81545127, 12345))).isNull()
        assertThat(orderLogisticsEntityRepository.lookupRow(OrderKey(543900, 81545127)))
            .isNotNull
            .satisfies {
                assertThat(it!!.deliveryFromTimestamp).isEqualTo(Instant.parse("2021-12-14T14:00:00Z"))
                assertThat(it.deliveryToTimestamp).isEqualTo(Instant.parse("2021-12-15T17:00:00Z"))
            }
    }

    @Test
    fun `order creation with personal address`() {
        val addressItem = MultiTypeRetrieveResponseItem()

        addressItem.let {
            addressItem.id = "personalAddressId"
            addressItem.type = CommonTypeEnum.ADDRESS
            val value = CommonType()
            value.address = mapOf(
                "city" to "Москваааааа",
                "street" to "Измайловский проспектттттттт",
                "house" to "74/2"
            )
            addressItem.value = value
        }

        val gpsItem = MultiTypeRetrieveResponseItem()

        gpsItem.let {
            gpsItem.id = "personalGpsId"
            gpsItem.type = CommonTypeEnum.GPS_COORD
            val value = CommonType()
            val coord = GpsCoord()
            coord.longitude = BigDecimal.valueOf(1L)
            coord.latitude = BigDecimal.valueOf(-1L)
            value.gpsCoord = coord
            gpsItem.value = value
        }

        val response = PersonalRetrieveResponse(listOf(addressItem, gpsItem))

        whenever(personalMarketService.retrieve(any()))
            .thenReturn(CompletableFuture.completedFuture(response))
        whenever(clock.instant()).thenReturn(Instant.now())
        whenever(geocoderApiService.getDeliveryAddresses(any()))
            .thenReturn(
                listOf(
                    DeliveryOptionAddress(
                        city = "Москва",
                        street = "Измайловский проспект",
                        house = "73/2",
                        geoId = 213,
                        longitude = 37.791720,
                        latitude = 55.788788,
                        postcode = "12324"
                    )
                )
            )
        whenever(stockStorageApiService.getAvailableAmounts(any()))
            .thenReturn(
                listOf(
                    StockItem(
                        partnerId = 123,
                        shopSku = "test_tovar",
                        warehouseId = 677,
                        count = 3000
                    )
                )
            )
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(CombinatorOuterClass.DeliveryRoute.getDefaultInstance())
        whenever(combinatorApiService.getDeliveryRoute(any(), any(), any(), any()))
            .thenReturn(
                CombinatorOuterClass.DeliveryRoute
                    .newBuilder()
                    .setRoute(CombinatorOuterClass.Route.getDefaultInstance())
                    .build()
            )
        whenever(combinatorApiService.getCourierOptions(any(), any(), any()))
            .thenReturn(
                listOf(
                    DeliveryOption(
                        deliveryDateFrom = LocalDate.of(2022, 4, 15),
                        deliveryDateTo = LocalDate.of(2022, 4, 19),
                        deliveryTimeInterval = DeliveryTimeInterval(
                            from = LocalTime.of(9, 0),
                            to = LocalTime.of(21, 0)
                        ),
                        deliveryServiceId = 54242,
                        paymentMethods = listOf(DeliveryPaymentMethod.PREPAYMENT),
                        leaveAtTheDoor = false,
                        doNotCall = true,
                        customizers = listOf()
                    )
                )
            )

        val prev = orderIdSequence.nextLongValue()
        val result = post(
            "/partners/123/common/orders/external",
            listOf(),
            this::class.loadResourceAsString(
                "orders/external/request/CommonOrdersControllerTest.create.external.order.11.request.json"
            )
        )
        assertExpectedResponse(
            result,
            prev + 1,
            "orders/external/response/CommonOrdersControllerTest.create.external.order.11.response.json"
        )

        assertThat(
            orderRepository.orderStockFreezeStatusRepository.lookupRow(
                OrderStockFreezeStatusKey(123, prev + 1)
            )
        )
            .satisfies {
                assertThat(it!!.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.FROZEN)
            }

        assertThat(
            orderRepository.merchantOrderIdIndexRepository.lookupRow(
                MerchantOrderIdIndex.Key(
                    partnerId = 123,
                    merchantOrderId = "ozon_id",
                    sourcePlatform = OrderSourcePlatform.OZON
                )
            )
        ).satisfies { assertThat(it!!.orderId).isEqualTo(prev + 1) }

        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.status).isEqualTo(MerchantOrderStatus.PROCESSING)
                assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.STARTED)
                assertThat(it.merchantOrderId).isEqualTo("ozon_id")
                assertThat(it.sourcePlatform).isEqualTo(OrderSourcePlatform.OZON)
                assertThat(it.buyerCurrency).isEqualTo("RUB")
                assertThat(it.color).isEqualTo(Color.BLUE)
                assertThat(it.isFake).isEqualTo(false)
                assertThat(it.isFulfillment).isEqualTo(true)
                assertThat(it.paymentType).isEqualTo(PaymentType.PREPAID)
                assertThat(it.notes).isEqualTo("Позвонить за час до приезда")
                assertThat(it.traits).contains(
                    ru.yandex.market.mbi.orderservice.common.enum.OrderTrait.ORDER_EDIT_ALLOWED
                )

                assertThat(
                    orderRepository.orderLineEntityRepository.lookupRow(
                        OrderLineKey(
                            123,
                            prev + 1,
                            it.lineIds?.first()!!
                        )
                    )
                ).satisfies { line ->
                    assertThat(line!!.price).isEqualTo(800)
                    assertThat(line.msku).isEqualTo(453525252)
                    assertThat(line.categoryId).isEqualTo(9393)
                    assertThat(line.feedId).isEqualTo(35442)
                    assertThat(line.warehouseId).isEqualTo(172)
                    assertThat(line.ffWarehouseId).isEqualTo(172)
                    assertThat(line.shopSku).isEqualTo("test_tovar")
                    assertThat(line.initialCount).isEqualTo(10)
                    assertThat(line.cargoTypes).containsAll(listOf(969, 933, 922))
                    assertThat(line.offerId).isEqualTo("test_tovar")
                    assertThat(line.offerName).isEqualTo("tovar test")
                }
            }

        assertThat(orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(123, prev + 1)))
            .satisfies {
                assertThat(it!!.key.partnerId).isEqualTo(123)
                assertThat(it.key.orderId).isEqualTo(prev + 1)
                assertThat(it.deliveryServiceId).isEqualTo(23242424)
                assertThat(it.deliveryRegionId).isEqualTo(213)

                assertThat(it.address).satisfies { address ->
                    assertThat(address!!.city).isEqualTo("Москваааааа")
                    assertThat(address.street).isEqualTo("Измайловский проспектттттттт")
                    assertThat(address.house).isEqualTo("74/2")
                }

                assertThat(it.buyer).satisfies { buyer ->
                    assertThat(buyer!!.email).isEqualTo("order-serv@mail.mbi")
                    assertThat(buyer.firstName).isEqualTo("Сервис")
                    assertThat(buyer.middleName).isEqualTo("Заказов")
                    assertThat(buyer.lastName).isEqualTo("Мбиайович")
                    assertThat(buyer.phone).isEqualTo("88005553535")
                    assertThat(buyer.personalFullnameId).isEqualTo("woehf928t6f23f298y")
                    assertThat(buyer.personalPhoneId).isEqualTo("1b3937f6f41153beb2f81a0e01b")
                    assertThat(buyer.personalEmailId).isEqualTo("610ca935e220e19d69891496206")
                }
            }

        assertThat(orderEventService.findEventsByOrderKey(OrderKey(123, prev + 1)))
            .singleElement()
            .satisfies { event ->
                assertThat(event.type).isEqualTo(OrderEventType.EXTERNAL_ORDER_CREATED)
            }

        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).satisfies { events ->
            assertThat(events).hasSize(1)

            assertThat(events).first().satisfies {
                assertThat(it.orderId).isEqualTo(prev + 1)
                assertThat(it.processed).isFalse
                assertThat(it.eventPayloadType).isEqualTo(OrderEvent.PayloadCase.NEW_EXTERNAL_ORDER_CREATED_PAYLOAD.name)
                assertThat(OrderEvent.parseFrom(it.eventPayload)).satisfies { protoEvent ->
                    assertThat(protoEvent.id).isNotNull.isNotEqualTo(0L)
                    assertThat(protoEvent.createdAt).isNotNull.isNotEqualTo(Timestamp.getDefaultInstance())
                    assertThat(protoEvent.traceId).isNotEmpty
                    assertThat(protoEvent.newExternalOrderCreatedPayload).satisfies { payload ->
                        assertThat(payload.orderKey).extracting("shopId", "orderId").containsExactly(123L, prev + 1)
                        assertThat(payload.notes).isEqualTo("Позвонить за час до приезда")
                        assertThat(payload.traitsList).contains(OrderTrait.ORDER_EDIT_ALLOWED)
                        assertThat(payload.recipient).extracting(
                            "firstName",
                            "middleName",
                            "lastName",
                            "phone",
                            "email",
                            "personalFullnameId",
                            "personalPhoneId",
                            "personalEmailId"
                        )
                            .containsExactly(
                                "Сервис",
                                "Заказов",
                                "Мбиайович",
                                "88005553535",
                                "order-serv@mail.mbi",
                                "woehf928t6f23f298y",
                                "1b3937f6f41153beb2f81a0e01b",
                                "610ca935e220e19d69891496206"
                            )
                    }
                }
            }
        }
    }

    private fun mockCheckouterRequest(
        fromDate: LocalDate,
        toDate: LocalDate,
        reason: HistoryEventReason,
        expectedRequestId: Long,
        expectedStatus: ChangeRequestStatus,
        partnerId: Long = 543900,
        orderId: Long = 81545127
    ) {
        val changeRequest = ChangeRequest(
            expectedRequestId,
            orderId,
            CheckouterDeliveryDatesChangeRequestPayload().apply {
                this.fromDate = fromDate
                this.toDate = toDate
                this.reason = reason
            },
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

    private fun saveRealDeliveryDate(
        orderId: Long,
        status: ChangeOrderStatus,
        substatus: OrderSubStatus?,
        realDeliveryDate: Instant,
        expected: String,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK
    ) {
        val params = listOfNotNull(
            "status" to status.name,
            substatus?.let { "substatus" to it.name },
            "realDeliveryDate" to realDeliveryDate.toString(),
            "actor" to ActorType.MERCHANT_PI.name,
            "actorId" to "1234"
        ).toMap()
        val request = HttpPost(getUri("/partners/$partnerId/common/orders/$orderId/real-delivery-date", params))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")

        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)

        JSONAssert.assertEquals(
            expected,
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    private fun saveDeliveryDates(
        orderId: Long,
        fromDate: LocalDate,
        toDate: LocalDate,
        reason: UpdateOrderReasonType,
        expected: String,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK
    ) {
        val params = listOfNotNull(
            "fromDate" to fromDate.toString(),
            "toDate" to toDate.toString(),
            "reason" to reason.name,
            "actor" to ActorType.MERCHANT_PI.name,
            "actorId" to "1234"
        ).toMap()
        val request = HttpPost(getUri("/partners/$partnerId/common/orders/$orderId/delivery-dates", params))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")

        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)

        JSONAssert.assertEquals(
            expected,
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    private fun post(
        path: String,
        params: List<Pair<String, String>>,
        body: String
    ): CloseableHttpResponse {
        val request = HttpPost(getUri(path, params))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        request.entity = StringEntity(body, ContentType.APPLICATION_JSON)
        return HttpClientBuilder.create().build().execute(request)
    }

    private fun assertExpectedResponse(
        response: CloseableHttpResponse,
        orderId: Long,
        path: String,
        status: Int = HttpStatus.SC_OK
    ) {
        val content = IOUtils.toString(response.entity.content)
        assertThat(response.statusLine.statusCode).isEqualTo(status)
        val expected = this::class.loadResourceAsString(path).replace("\"&orderId\"", orderId.toString())
        JSONAssert.assertEquals(
            expected,
            content,
            JSONCompareMode.STRICT_ORDER
        )
    }
}
