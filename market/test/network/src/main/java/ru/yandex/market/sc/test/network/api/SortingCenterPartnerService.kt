package ru.yandex.market.sc.test.network.api

import retrofit2.http.*
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.network.arch.data.PageImpl
import ru.yandex.market.sc.test.data.partner.cell.*
import ru.yandex.market.sc.test.data.partner.lot.PartnerLotDto
import ru.yandex.market.sc.test.data.partner.lot.PartnerLotRequest
import ru.yandex.market.sc.test.data.partner.lot.PartnerLotsDtoWrapper
import ru.yandex.market.sc.test.data.partner.sort.SortParam
import ru.yandex.market.sc.test.data.partner.warehouse.PartnerWarehousesDto
import ru.yandex.market.sc.test.data.partner.zone.PartnerZoneDto
import ru.yandex.market.sc.test.data.partner.zone.PartnerZoneRequest
import ru.yandex.market.sc.test.network.data.partner.cell.PartnerCellRequest

/**
 * [Swagger](https://sc-int.tst.vs.market.yandex.net/swagger-ui.html#/)
 */
interface SortingCenterPartnerService {
    @POST("{scPartnerId}/cells")
    suspend fun createCell(
        @Path("scPartnerId") scPartnerId: Long,
        @Body partnerCellRequest: PartnerCellRequest,
    ): PartnerCellDtoWrapper

    @GET("{scPartnerId}/cells/{id}")
    suspend fun getCell(
        @Path("scPartnerId") scPartnerId: Long,
        @Path("id") cellId: Long,
    ): PartnerCellDtoWrapper

    @PUT("{scPartnerId}/cells/{id}")
    suspend fun updateCell(
        @Path("scPartnerId") scPartnerId: Long,
        @Path("id") cellId: Long,
        @Body partnerCellRequest: PartnerCellRequest,
    ): PartnerCellDtoWrapper

    @DELETE("{scPartnerId}/cells/{id}")
    suspend fun deleteCell(
        @Path("scPartnerId") scPartnerId: Long,
        @Path("id") cellId: Long,
    ): PartnerCellDtoWrapper

    @GET("{scPartnerId}/cells/buffer")
    suspend fun getBufferCells(
        @Path("scPartnerId") scPartnerId: Long,
    ): PartnerBufferCellsDto

    @GET("{scPartnerId}/cells/page")
    suspend fun getCells(
        @Path("scPartnerId") scPartnerId: Long,
        @Query("deleted") deleted: Boolean? = null,
        @Query("number") number: String? = null,
        @Query("offset") offset: Long? = null,
        @Query("page") page: Int? = null,
        @Query("pageNumber") pageNumber: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("paged") paged: Boolean? = null,
        @Query("unpaged") unpaged: Boolean? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: List<SortParam>? = null,
        @Query("status") status: Cell.Status? = null,
        @Query("type") type: Cell.Type? = null,
        @Query("subType") subType: Cell.SubType? = null,
    ): PageImpl<PartnerCellDto>

    @GET("{scPartnerId}/cellTypes")
    suspend fun getCellTypes(
        @Path("scPartnerId") scPartnerId: Long,
    ): PartnerCellTypesDto

    @GET("{scPartnerId}/cellSubTypes")
    suspend fun getCellSubTypes(
        @Path("scPartnerId") scPartnerId: Long,
    ): PartnerCellSubTypesDto

    @GET("{scPartnerId}/lots")
    suspend fun getLots(
        @Path("scPartnerId") scPartnerId: Long,
        @Query("cellId") cellIds: List<Long>? = null,
        @Query("number") number: String? = null,
        @Query("offset") offset: Long? = null,
        @Query("page") page: Int? = null,
        @Query("pageNumber") pageNumber: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("paged") paged: Boolean? = null,
        @Query("unpaged") unpaged: Boolean? = null,
        @Query("size") size: Int? = null,
        @Query("sort") sort: List<SortParam>? = null,
    ): PageImpl<PartnerLotDto>

    @POST("{scPartnerId}/lots")
    suspend fun createLot(
        @Path("scPartnerId") scPartnerId: Long,
        @Body partnerLotRequest: PartnerLotRequest,
    ): PartnerLotsDtoWrapper

    @POST("{scPartnerId}/lots/{id}")
    suspend fun getLot(
        @Path("scPartnerId") scPartnerId: Long,
        @Path("id") lotId: Long,
    ): PartnerLotsDtoWrapper

    @DELETE("{scPartnerId}/lots/{id}")
    suspend fun deleteLot(
        @Path("scPartnerId") scPartnerId: Long,
        @Path("id") lotId: Long,
    ): PartnerLotsDtoWrapper

    @PUT("{scPartnerId}/lots/{routeId}")
    suspend fun shipLots(
        @Path("scPartnerId") scPartnerId: Long,
        @Path("routeId") routeId: Long,
    )

    @GET("{scPartnerId}/warehouses")
    suspend fun getWarehouses(
        @Path("scPartnerId") scPartnerId: Long,
    ): PartnerWarehousesDto

    @POST("{scPartnerId}/zones")
    suspend fun createZone(
        @Path("scPartnerId") scPartnerId: Long,
        @Body partnerZoneRequest: PartnerZoneRequest,
    ): PartnerZoneDto

    @GET("{scPartnerId}/zones/{id}")
    suspend fun getZone(
        @Path("scPartnerId") scPartnerId: Long,
        @Path("id") id: Long,
    ): PartnerZoneDto

    @DELETE("{scPartnerId}/zones/{id}")
    suspend fun deleteZone(
        @Path("scPartnerId") scPartnerId: Long,
        @Path("id") id: Long,
    ): PartnerZoneDto
}
