package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.lot.MovingLotInfo
import ru.yandex.market.sc.core.data.lot.MovingLotInfo.ErrorCode

class MovingLotInfoBuilder private constructor() {
    private var zoneName: String = "Some zone name"
    private var cellDestination: String = "Some destination"
    private var cellCapacity: Int? = 0
    private var lotsInCell: Int? = 0
    private var freeLotsInZone: Int? = 0
    private var cellAddress: String = "A-0001-00001-12"
    private var errorCode: ErrorCode = ErrorCode.UNKNOWN

    fun setErrorCode(errorCode: ErrorCode): MovingLotInfoBuilder = apply {
        this.errorCode = errorCode
    }

    fun build() = MovingLotInfo(
        cellAddress,
        zoneName,
        cellDestination,
        cellCapacity,
        lotsInCell,
        freeLotsInZone,
        errorCode
    )

    companion object {
        fun create(): MovingLotInfoBuilder {
            return MovingLotInfoBuilder()
        }
    }
}