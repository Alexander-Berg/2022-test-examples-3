package ru.yandex.market.sc.test.network.api

import retrofit2.http.*
import ru.yandex.market.sc.core.data.lot.LotDto
import ru.yandex.market.sc.core.data.order.OrderDto
import ru.yandex.market.sc.test.data.manual.courier.ManualCourier
import ru.yandex.market.sc.test.data.manual.inbound.ManualInboundDto
import ru.yandex.market.sc.test.data.partner.lot.PartnerLotDto
import ru.yandex.market.sc.test.data.partner.lot.PartnerLotRequest
import ru.yandex.market.sc.test.network.data.manual.inbound.ManualInboundRequest
import ru.yandex.market.sc.test.network.data.manual.outbound.ManualOutboundRequest
import ru.yandex.market.sc.test.network.data.manual.route.ShipRouteRequest
import ru.yandex.market.sc.test.network.data.partner.lot.PartnerOrphanLotRequest

/**
 * [Swagger](https://sc-int.tst.vs.market.yandex.net/swagger-ui.html#/)
 */
interface SortingCenterManualService {
    @GET("inbounds")
    suspend fun getInbound(@Query("externalId") externalId: String): ManualInboundDto

    @POST("inbounds/createDemo")
    suspend fun createDemoInbound(
        @Query("scId") scId: Long,
        @Query("warehouseFromYandexId") warehouseFromYandexId: String,
        @Query("externalId") externalId: String? = null,
        @Query("registryId") registryId: String? = null,
        @Query("nextLogisticPointId") nextLogisticPointId: String? = null,
        @Query("transportationId") transportationId: String? = null,
        @Body request: ManualInboundRequest = ManualInboundRequest(),
    ): String

    @PUT("inbounds/fixInbound")
    suspend fun fixInbound(@Query("externalId") externalId: String)

    @POST("outbounds/createDemo")
    suspend fun createDemoOutbound(
        @Query("scId") scId: Long,
        @Query("externalId") externalId: String? = null,
        @Query("partnerToExternalId") partnerToExternalId: String? = null,
        @Query("logisticPointToExternalId") logisticPointToExternalId: String,
        @Body request: ManualOutboundRequest = ManualOutboundRequest(),
    ): String

    @POST("outbounds/{externalId}/ship")
    suspend fun shipOutbound(@Path("externalId") externalId: String)

    @POST("outbounds/{externalId}/plannedRegistry")
    suspend fun putOutboundRegistry(
        @Path("externalId") externalId: String,
        @Query("scId") scId: Long,
        @Query("registryExternalId") registryExternalId: String,
        @Query("boxExternalIds") boxExternalIds: List<String>? = null,
        @Query("palletExternalIds") palletExternalIds: List<String>? = null,
    )

    @PUT("outbounds/{externalId}/cancel")
    suspend fun closeOutbound(@Path("externalId") externalId: String)

    @PUT("lots/{lotId}/prepare")
    suspend fun prepareToShipLot(
        @Path("lotId") lotId: Long,
    )

    @PUT("lots/ship")
    suspend fun shipLot(
        @Query("externalLotId") externalLotId: String,
        @Query("scId") scId: Long,
    )

    @GET("lots/external")
    suspend fun getLotByExternalId(
        @Query("externalId") externalId: String,
        @Query("scId") scId: Long,
    ): PartnerLotDto

    @GET("lots/{lotId}")
    suspend fun getLotById(
        @Path("lotId") lotId: Long,
    ): LotDto

    @POST("lots/create")
    suspend fun createLots(
        @Query("scId") scId: Long,
        @Body partnerLotRequest: PartnerLotRequest,
    ): List<Long>

    @POST("lots/createOrphans")
    suspend fun createOrphanLots(
        @Query("scId") scId: Long,
        @Body partnerLotRequest: PartnerOrphanLotRequest,
    ): List<String>

    @GET("orders")
    suspend fun getOrder(
        @Query("externalId") externalId: String,
        @Query("scId") scId: Long? = null,
    ): OrderDto

    @POST("orders/accept")
    suspend fun acceptOrder(
        @Query("externalOrderId") externalOrderId: String,
        @Query("externalPlaceId") externalPlaceId: String? = null,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/acceptAndSort")
    suspend fun acceptAndSortOrder(
        @Query("cellId") cellId: Long? = null,
        @Query("courierId") courierId: Int? = null,
        @Query("externalOrderId") externalOrderId: String,
        @Query("externalPlaceId") externalPlaceId: String? = null,
        @Query("ignoreTodayRouteOnKeep") ignoreTodayRouteOnKeep: Boolean? = null,
        @Query("scId") scId: Long? = null,
        @Query("warehouseId") warehouseId: Long? = null,
    )

    @POST("orders/cancel")
    suspend fun cancelOrder(
        @Query("externalOrderId") externalOrderId: String,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/createDemo")
    suspend fun createDemoOrder(
        @Query("deliveryServiceYandexId") deliveryServiceYandexId: String? = null,
        @Query("placesCnt") placesCnt: Int? = null,
        @Query("scId") scId: Long,
        @Query("shipmentDate") shipmentDate: String? = null,
        @Query("textExternalId") textExternalId: Boolean? = null,
        @Query("warehousePartnerId") warehousePartnerId: String? = null,
        @Query("warehouseYandexId") warehouseYandexId: String? = null,
        @Query("courierId") courierId: Long? = null,
    ): String

    @POST("orders/createDemo")
    suspend fun createDemoOrder(
        @Query("deliveryServiceYandexId") deliveryServiceYandexId: String? = null,
        @Query("placesCnt") placesCnt: Int? = null,
        @Query("scId") scId: Long,
        @Query("shipmentDate") shipmentDate: String? = null,
        @Query("textExternalId") textExternalId: Boolean? = null,
        @Query("warehousePartnerId") warehousePartnerId: String? = null,
        @Query("warehouseYandexId") warehouseYandexId: String? = null,
        @Body courierRequest: ManualCourier,
    ): String

    @POST("orders/keep")
    suspend fun keepOrder(
        @Query("cellId") cellId: Int? = null,
        @Query("externalOrderId") externalOrderId: String,
        @Query("externalPlaceId") externalPlaceId: String? = null,
        @Query("ignoreTodayRoute") ignoreTodayRoute: Boolean? = null,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/markOrderAsDamaged")
    suspend fun markOrderAsDamaged(
        @Query("externalOrderId") externalOrderId: String,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/preship")
    suspend fun prepareToShipOrder(
        @Query("cellId") cellId: Int,
        @Query("externalOrderId") externalOrderId: String,
        @Query("externalPlaceId") externalPlaceId: String? = null,
        @Query("routeId") routeId: Long,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/return")
    suspend fun returnOrder(
        @Query("externalOrderId") externalOrderId: String,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/revertMarkOrderAsDamaged")
    suspend fun revertMarkOrderAsDamaged(
        @Query("externalOrderId") externalOrderId: String,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/revertReturnOrder")
    suspend fun revertReturnOrder(
        @Query("externalOrderId") externalOrderId: String,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/ship")
    suspend fun shipOrder(
        @Query("courierId") courierId: Long? = null,
        @Query("externalOrderId") externalOrderId: String,
        @Query("externalPlaceId") externalPlaceId: String? = null,
        @Query("ignoreTodayRoute") ignoreTodayRoute: Boolean? = null,
        @Query("scId") scId: Long? = null,
        @Query("warehouseId") warehouseId: Long? = null,
    )

    @POST("orders/sort")
    suspend fun sortOrder(
        @Query("cellId") cellId: Int? = null,
        @Query("externalOrderId") externalOrderId: String,
        @Query("externalPlaceId") externalPlaceId: String? = null,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/updateCourier")
    suspend fun updateCourier(
        @Query("courierId") courierId: Long? = null,
        @Query("externalOrderId") externalOrderId: String,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/updateDeliveryDate")
    suspend fun updateDeliveryDate(
        @Query("deliveryDate") deliveryDate: String,
        @Query("externalOrderId") externalOrderId: String,
        @Query("scId") scId: Long? = null,
    )

    @POST("orders/updateShipmentDate")
    suspend fun updateShipmentDate(
        @Query("shipmentDate") shipmentDate: String,
        @Query("externalOrderId") externalOrderId: String,
        @Query("force") force: Boolean? = null,
        @Query("scId") scId: Long? = null,
    )

    @POST("routes/{id}/ship")
    suspend fun shipRoute(
        @Path("id") id: Long,
        @Body request: ShipRouteRequest,
    )

    @GET("courier/encrypt")
    suspend fun getEncryptedCourierQrCode(
        @Query("courierUid") courierUid: Long,
        @Query("randomNumber") randomNumber: Long,
        @Query("shipmentDate") shipmentDate: String,
    ): String
}