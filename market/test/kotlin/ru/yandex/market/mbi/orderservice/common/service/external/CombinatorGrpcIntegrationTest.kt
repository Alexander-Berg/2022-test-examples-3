package ru.yandex.market.mbi.orderservice.common.service.external

import com.google.protobuf.util.JsonFormat
import io.grpc.netty.NettyChannelBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.retry.support.RetryTemplate
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import ru.yandex.market.grpc.trace.TraceClientInterceptor
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryOptionAddress
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryOptionDestination
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryOptionItem
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.DeliveryOptionsRequest
import ru.yandex.market.mbi.orderservice.common.model.dto.combinator.OfferDimensions
import ru.yandex.market.mbi.orderservice.common.util.GrpcTvmInterceptor
import ru.yandex.market.mbi.orderservice.common.util.toProtoTimestamp
import ru.yandex.market.mbi.partner_tvm.tvm_client.FakeTvmClient
import ru.yandex.market.request.trace.Module
import yandex.market.combinator.v0.CombinatorGrpc
import yandex.market.combinator.v0.CombinatorOuterClass
import java.time.Instant

/**
 * Интеграционный тест для проверки Ваших запросов к комбинатору.
 */
@Disabled
class CombinatorGrpcIntegrationTest {

    private var stub: CombinatorGrpc.CombinatorBlockingStub? = null
    private var combinatorApiService: CombinatorApiService? = null

    @BeforeEach
    fun init() {
        val tvmClient = FakeTvmClient()
        val channel = NettyChannelBuilder.forTarget(
            "combinator.tst.vs.market.yandex.net:8080"
        )
            .usePlaintext()
            .intercept(GrpcTvmInterceptor(tvmClient, 2019599))
            .intercept(TraceClientInterceptor(Module.COMBINATOR))
            .build()

        stub = CombinatorGrpc.newBlockingStub(channel)
        combinatorApiService = CombinatorApiService(
            stub!!,
            RetryTemplate()
        )
    }

    @Test
    fun getExpressWarehousesTest() {
        val result = stub!!.getExpressWarehouses(
            CombinatorOuterClass.ExpressWarehousesRequest
                .newBuilder()
                .setRegionId(213)
                .build()
        )
        println(JsonFormat.printer().print(result))
    }

    @Test
    fun getDeliveryOptionsTest() {
        val result = stub!!.getDeliveryOptions(
            CombinatorOuterClass.DeliveryRequest
                .newBuilder()
                // Обычно дата создания заказа, время, с которого нужно начинать сборку
                .setStartTime(Instant.now().toProtoTimestamp())
                .setDestination(
                    CombinatorOuterClass.PointIds.newBuilder()
                        // Регион доставки
                        .setRegionId(213)
                        // GPS координаты назначения
                        .setGpsCoords(
                            CombinatorOuterClass.GpsCoords
                                .newBuilder()
                                // Широта
                                .setLat(55.70417927289191)
                                // Долгота
                                .setLon(37.90647953857668)
                                .build()
                        )
                        .build()
                )
                .addAllItems(
                    listOf(
                        CombinatorOuterClass.DeliveryRequestItem
                            .newBuilder()
                            // Необходимое количество
                            .setRequiredCount(1)
                            .addAllAvailableOffers(
                                listOf(
                                    CombinatorOuterClass.Offer
                                        .newBuilder()
                                        // SSKU
                                        .setShopSku("217176139.alisa3p")
                                        // Ид магазина
                                        .setShopId(10427354)
                                        // Ид склада
                                        .setPartnerId(172)
                                        // Доступное количество товара на складе
                                        .setAvailableCount(314)
                                        // Ид фида
                                        .setFeedId(475690)
                                        .build()
                                )
                            )
                            .build()
                    )
                )
                .build()
        )
        println(JsonFormat.printer().print(result))
    }

    @Test
    fun getCourierOptionsTest() {
        val options = stub!!.getCourierOptions(
            CombinatorOuterClass.DeliveryRequest
                .newBuilder()
                // Обычно дата создания заказа, время, с которого нужно начинать сборку
                .setStartTime(Instant.now().toProtoTimestamp())
                .setDestination(
                    CombinatorOuterClass.PointIds.newBuilder()
                        // Регион доставки
                        .setRegionId(213)
                        // GPS координаты назначения
                        .setGpsCoords(
                            CombinatorOuterClass.GpsCoords
                                .newBuilder()
                                // Широта
                                .setLat(55.70417927289191)
                                // Долгота
                                .setLon(37.90647953857668)
                                .build()
                        )
                        .build()
                )
                .addAllItems(
                    listOf(
                        CombinatorOuterClass.DeliveryRequestItem
                            .newBuilder()
                            // Необходимое количество
                            .setRequiredCount(1)
                            .addAllAvailableOffers(
                                listOf(
                                    CombinatorOuterClass.Offer
                                        .newBuilder()
                                        // SSKU
                                        .setShopSku("217176139.alisa3p")
                                        // Ид магазина
                                        .setShopId(10427354)
                                        // Ид склада
                                        .setPartnerId(172)
                                        // Доступное количество товара на складе
                                        .setAvailableCount(314)
                                        // Ид фида
                                        .setFeedId(475690)
                                        .build()
                                )
                            )
                            .build()
                    )
                )
                .build()
        )
        println(options)
    }

    @Test
    fun getDeliveryRouteTest() {
        val options = stub!!.getCourierOptions(
            CombinatorOuterClass.DeliveryRequest
                .newBuilder()
                // Обычно дата создания заказа, время, с которого нужно начинать сборку
                .setStartTime(Instant.now().toProtoTimestamp())
                .setDestination(
                    CombinatorOuterClass.PointIds.newBuilder()
                        // Регион доставки
                        .setRegionId(213)
                        // GPS координаты назначения
                        .setGpsCoords(
                            CombinatorOuterClass.GpsCoords
                                .newBuilder()
                                // Широта
                                .setLat(55.70417927289191)
                                // Долгота
                                .setLon(37.90647953857668)
                                .build()
                        )
                        .build()
                )
                .addAllItems(
                    listOf(
                        CombinatorOuterClass.DeliveryRequestItem
                            .newBuilder()
                            // Необходимое количество
                            .setRequiredCount(1)
                            .addAllAvailableOffers(
                                listOf(
                                    CombinatorOuterClass.Offer
                                        .newBuilder()
                                        // SSKU
                                        .setShopSku("217176139.alisa3p")
                                        // Ид магазина
                                        .setShopId(10427354)
                                        // Ид склада
                                        .setPartnerId(172)
                                        // Доступное количество товара на складе
                                        .setAvailableCount(314)
                                        // Ид фида
                                        .setFeedId(475690)
                                        .build()
                                )
                            )
                            .build()
                    )
                )
                .build()
        )

        val routeOption = options.getOptions(0)

        val route = stub!!.getDeliveryRoute(
            CombinatorOuterClass.DeliveryRequest
                .newBuilder()
                // Обычно дата создания заказа, время, с которого нужно начинать сборку
                .setStartTime(Instant.now().toProtoTimestamp())
                .setDestination(
                    CombinatorOuterClass.PointIds.newBuilder()
                        // Регион доставки
                        .setRegionId(213)
                        // GPS координаты назначения
                        .setGpsCoords(
                            CombinatorOuterClass.GpsCoords
                                .newBuilder()
                                // Широта
                                .setLat(55.70417927289191)
                                // Долгота
                                .setLon(37.90647953857668)
                                .build()
                        )
                        .build()
                )
                .addAllItems(
                    listOf(
                        CombinatorOuterClass.DeliveryRequestItem
                            .newBuilder()
                            // Необходимое количество
                            .setRequiredCount(1)
                            .addAllAvailableOffers(
                                listOf(
                                    CombinatorOuterClass.Offer
                                        .newBuilder()
                                        // SSKU
                                        .setShopSku("217176139.alisa3p")
                                        // Ид магазина
                                        .setShopId(10427354)
                                        // Ид склада
                                        .setPartnerId(172)
                                        // Доступное количество товара на складе
                                        .setAvailableCount(314)
                                        // Ид фида
                                        .setFeedId(475690)
                                        .build()
                                )
                            )
                            .build()
                    )
                )
                .setOption(routeOption)
                .build()
        )
        println(JsonFormat.printer().print(route))
    }

    @Test
    fun getCourierOptionsFromCombinatorTest() {
        val result = combinatorApiService!!.getCourierOptions(
            DeliveryOptionsRequest(
                214124,
                Instant.now(),
                DeliveryOptionAddress(
                    city = "Москва",
                    street = "Измайловский проспект",
                    house = "73/2"
                ),
                listOf(
                    DeliveryOptionItem(
                        ssku = "217176139.alisa3p",
                        partnerId = 10427354,
                        warehouseId = 172,
                        feedId = 475690,
                        offerName = "dd",
                        msku = 2L,
                        categoryId = 1,
                        requiredCount = 314,
                        price = 1,
                        dimensions = OfferDimensions(
                            weight = 1010,
                            depth = 20,
                            width = 10,
                            height = 10
                        ),
                        cargoTypes = listOf(1)
                    )
                )
            ),
            DeliveryOptionDestination(
                213, null, null
            ),
            mapOf(Pair("217176139.alisa3p", 10L))
        )

        println(ObjectMapper().writeValueAsString(result))
    }

    @Test
    fun getDeliveryRouteFromCombinatorTest() {
        val deliveryOptionsRequest = DeliveryOptionsRequest(
            2323,
            Instant.now(),
            DeliveryOptionAddress(
                city = "Москва",
                street = "Измайловский проспект",
                house = "73/2"
            ),
            listOf(
                DeliveryOptionItem(
                    ssku = "217176139.alisa3p",
                    partnerId = 10427354,
                    warehouseId = 172,
                    feedId = 475690,
                    offerName = "dd",
                    msku = 2L,
                    categoryId = 1,
                    requiredCount = 314,
                    price = 1,
                    dimensions = OfferDimensions(
                        weight = 1010,
                        depth = 20,
                        width = 10,
                        height = 10
                    ),
                    cargoTypes = listOf(1)
                )
            )
        )

        val options = combinatorApiService!!.getCourierOptions(
            deliveryOptionsRequest,
            DeliveryOptionDestination(
                213, null, null
            ),
            mapOf(Pair("217176139.alisa3p", 10L))
        )
        val option = options[0]
        val route = combinatorApiService!!.getDeliveryRoute(
            deliveryOptionsRequest,
            DeliveryOptionDestination(
                213, null, null
            ),
            option,
            mapOf(Pair("217176139.alisa3p", 10L))
        )

        println(route)
    }
}
