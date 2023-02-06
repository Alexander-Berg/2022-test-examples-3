package ru.yandex.market.sc.test.network.repository.partner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.network.arch.data.Page
import ru.yandex.market.sc.core.network.data.page.PageMapper
import ru.yandex.market.sc.core.utils.data.functional.Functional.orThrow
import ru.yandex.market.sc.test.data.partner.cell.*
import ru.yandex.market.sc.test.data.partner.sort.SortParam
import ru.yandex.market.sc.test.network.api.SortingCenterPartnerService
import ru.yandex.market.sc.test.network.data.partner.cell.PartnerCellRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartnerCellRepository @Inject constructor(
    private val sortingCenterPartnerService: SortingCenterPartnerService,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val pageCellMapper = PageMapper(PartnerCellMapper)

    suspend fun createCell(scPartnerId: Long, request: PartnerCellRequest): PartnerCellWrapper =
        withContext(ioDispatcher) {
            PartnerCellWrapperMapper.map(
                sortingCenterPartnerService.createCell(
                    scPartnerId,
                    request
                )
            ).orThrow()
        }

    suspend fun getCell(scPartnerId: Long, cellId: Long): PartnerCellWrapper =
        withContext(ioDispatcher) {
            PartnerCellWrapperMapper.map(sortingCenterPartnerService.getCell(scPartnerId, cellId))
                .orThrow()
        }

    suspend fun updateCell(
        scPartnerId: Long,
        cellId: Long,
        request: PartnerCellRequest
    ): PartnerCellWrapper =
        withContext(ioDispatcher) {
            PartnerCellWrapperMapper.map(
                sortingCenterPartnerService.updateCell(
                    scPartnerId,
                    cellId,
                    request
                )
            ).orThrow()
        }

    suspend fun deleteCell(scPartnerId: Long, cellId: Long): PartnerCellWrapper =
        withContext(ioDispatcher) {
            PartnerCellWrapperMapper.map(
                sortingCenterPartnerService.deleteCell(
                    scPartnerId,
                    cellId
                )
            ).orThrow()
        }

    suspend fun getBufferCells(scPartnerId: Long): PartnerBufferCells = withContext(ioDispatcher) {
        PartnerBufferCellsMapper.map(sortingCenterPartnerService.getBufferCells(scPartnerId))
            .orThrow()
    }

    suspend fun getCells(
        scPartnerId: Long,
        deleted: Boolean? = false,
        number: String? = null,
        sort: List<SortParam>? = null,
        status: Cell.Status? = null,
        type: Cell.Type? = null,
        subType: Cell.SubType? = null,
    ): Page<PartnerCell> = withContext(ioDispatcher) {
        pageCellMapper.map(
            sortingCenterPartnerService.getCells(
                scPartnerId = scPartnerId,
                deleted = deleted,
                number = number,
                sort = sort,
                status = status,
                type = type,
                subType = subType,
            )
        ).orThrow()
    }

    suspend fun getCellTypes(scPartnerId: Long): PartnerCellTypes = withContext(ioDispatcher) {
        PartnerCellTypesMapper.map(sortingCenterPartnerService.getCellTypes(scPartnerId)).orThrow()
    }

    suspend fun getCellSubTypes(scPartnerId: Long): PartnerCellSubTypes =
        withContext(ioDispatcher) {
            PartnerCellSubTypesMapper.map(sortingCenterPartnerService.getCellSubTypes(scPartnerId))
                .orThrow()
        }
}
