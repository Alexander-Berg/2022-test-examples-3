package ru.yandex.market.sc.test.network.domain.cell

import kotlinx.coroutines.runBlocking
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.network.arch.ext.unwrap
import ru.yandex.market.sc.core.network.domain.NetworkOrderUseCases
import ru.yandex.market.sc.test.data.partner.cell.PartnerCell
import ru.yandex.market.sc.test.data.partner.cell.PartnerCellWrapper
import ru.yandex.market.sc.test.data.partner.cell.PartnerCellWrapper.Companion.wrap
import ru.yandex.market.sc.test.network.data.partner.cell.PartnerCellRequest
import ru.yandex.market.sc.test.network.domain.common.TestSortingCenterUtils
import ru.yandex.market.sc.test.network.repository.manual.ManualOrderRepository
import ru.yandex.market.sc.test.network.repository.partner.PartnerCellRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestCellUseCases @Inject constructor(
    private val partnerCellRepository: PartnerCellRepository,
    private val networkOrderUseCases: NetworkOrderUseCases,
    private val manualOrderRepository: ManualOrderRepository,
    private val sortingCenterUtils: TestSortingCenterUtils,
) {
    fun getBufferCell(
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        index: Int = 0
    ): PartnerCell {
        return getOrCreateCell(
            index = index,
            type = Cell.Type.BUFFER,
            subType = Cell.SubType.DEFAULT,
            scPartnerId = scPartnerId,
        )
    }

    fun getBufferXdocCell(
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        index: Int = 0
    ): PartnerCell {
        return getOrCreateCell(
            index = index,
            type = Cell.Type.BUFFER,
            subType = Cell.SubType.BUFFER_XDOC,
            scPartnerId = scPartnerId,
        )
    }

    fun getDroppedOrdersCell(
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        index: Int = 0,
    ): PartnerCell {
        return getOrCreateCell(
            index = index,
            type = Cell.Type.BUFFER,
            subType = Cell.SubType.DROPPED_ORDERS,
            scPartnerId = scPartnerId,
        )
    }

    fun getReturnCell(
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        index: Int = 0
    ): PartnerCell {
        return getOrCreateCell(
            index = index,
            type = Cell.Type.RETURN,
            subType = Cell.SubType.DEFAULT,
            scPartnerId = scPartnerId,
        )
    }

    fun getUtilizationCell(
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        index: Int = 0
    ): PartnerCell {
        return getOrCreateCell(
            index = index,
            type = Cell.Type.RETURN,
            subType = Cell.SubType.UTILISATION,
            scPartnerId = scPartnerId,
        )
    }

    fun getDamagedCell(
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        index: Int = 0
    ): PartnerCell {
        return getOrCreateCell(
            index = index,
            type = Cell.Type.RETURN,
            subType = Cell.SubType.RETURN_DAMAGED,
            scPartnerId = scPartnerId,
        )
    }

    fun getClientReturnCell(
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
        index: Int = 0,
    ): PartnerCell {
        return getOrCreateCell(
            index = index,
            type = Cell.Type.RETURN,
            subType = Cell.SubType.CLIENT_RETURN,
            scPartnerId = scPartnerId,
        )
    }

    fun activateCell(
        cellId: Long,
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
    ): PartnerCellWrapper = runBlocking {
        val (cell) = partnerCellRepository.getCell(scPartnerId, cellId)
        changeCellStatus(cell, Cell.Status.ACTIVE)
    }

    fun activateCell(cell: PartnerCell): PartnerCellWrapper {
        return changeCellStatus(cell, Cell.Status.ACTIVE)
    }

    fun cleanCell(
        targetCellId: Long,
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId()
    ) = runBlocking {
        val bufferCell = getBufferCell(scPartnerId = scPartnerId)
        networkOrderUseCases.getRouteOrderIdsByCellId(cellId = targetCellId).map { routeOrder ->
            if (!routeOrder.multiPlace) {
                manualOrderRepository.acceptAndSortOrder(
                    cellId = bufferCell.id,
                    externalOrderId = routeOrder.externalId.value,
                    ignoreTodayRouteOnKeep = true,
                )
            } else routeOrder.places.forEach { place ->
                manualOrderRepository.acceptAndSortOrder(
                    cellId = bufferCell.id,
                    externalOrderId = routeOrder.externalId.value,
                    externalPlaceId = place.externalId.value,
                    ignoreTodayRouteOnKeep = true,
                )
            }
        }
    }

    private fun getOrCreateCell(
        index: Int = 0,
        type: Cell.Type,
        subType: Cell.SubType,
        status: Cell.Status = Cell.Status.ACTIVE,
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
    ): PartnerCell = runBlocking {
        partnerCellRepository.getCells(
            scPartnerId = scPartnerId,
            type = type,
            subType = subType,
            status = status
        ).unwrap().getOrElse(index) {
            val request = PartnerCellRequest(
                type = type,
                subType = subType,
                status = status,
                number = "$type-$subType-${it + 1}"
            )
            partnerCellRepository.createCell(scPartnerId, request).unwrap()
        }
    }

    private fun changeCellStatus(
        cell: PartnerCell,
        status: Cell.Status,
        scPartnerId: Long = sortingCenterUtils.getCurrentScPartnerId(),
    ): PartnerCellWrapper = runBlocking {
        if (cell.status == status) wrap(cell) else {
            val request = mapToRequest(cell).copy(status = status, number = cell.id.toString())

            partnerCellRepository.updateCell(
                scPartnerId = scPartnerId,
                cellId = cell.id,
                request = request
            )
        }
    }

    private fun mapToRequest(cell: PartnerCell) = PartnerCellRequest(
        number = cell.number ?: "",
        status = cell.status,
        type = cell.type,
        subType = cell.subType,
        courierId = cell.courierId,
        warehouseYandexId = cell.warehouseYandexId?.value,
    )
}
