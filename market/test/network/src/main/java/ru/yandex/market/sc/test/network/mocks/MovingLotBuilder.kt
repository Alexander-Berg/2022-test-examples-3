package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.lot.Lot.Status
import ru.yandex.market.sc.core.data.lot.MovingLot
import ru.yandex.market.sc.core.data.lot.MovingLot.ErrorCode
import ru.yandex.market.sc.core.utils.data.ExternalId

class MovingLotBuilder private constructor() {
    private var externalId: ExternalId = ExternalId("externalId")
    private var status: Status = Status.CREATED
    private var destination: String = "Some destination"
    private var shipDate: String? = null
    private var cellAddress: String = "A-0001-00001-12"
    private var cellsAddressList: List<String> = emptyList()
    private var errorCode: ErrorCode = ErrorCode.UNKNOWN

    fun setExternalId(externalId: ExternalId): MovingLotBuilder = apply {
        this.externalId = externalId
    }

    fun setStatus(status: Status): MovingLotBuilder = apply {
        this.status = status
    }

    fun setDestination(destination: String): MovingLotBuilder = apply {
        this.destination = destination
    }

    fun setShipDate(shipDate: String): MovingLotBuilder = apply {
        this.shipDate = shipDate
    }

    fun setErrorCode(errorCode: ErrorCode): MovingLotBuilder = apply {
        this.errorCode = errorCode
    }

    fun setCellAddress(address: String): MovingLotBuilder = apply {
        this.cellAddress = address
    }

    fun build() = MovingLot(
        externalId,
        status,
        shipDate,
        destination,
        cellsAddressList,
        cellAddress,
        errorCode
    )

    companion object {
        fun create(): MovingLotBuilder {
            return MovingLotBuilder()
        }
    }
}
