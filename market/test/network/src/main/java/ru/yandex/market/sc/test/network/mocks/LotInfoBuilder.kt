package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.lot.LotInfo
import ru.yandex.market.sc.core.utils.data.ExternalId

class LotInfoBuilder private constructor() {
    companion object {
        fun create() = LotInfoBuilder()
    }

    private var externalId: ExternalId = ExternalId("externalId")
    private var name: String = "без названия"
    private var boxCount: Int = 0
    private var boxes: List<String> = emptyList()
    private var inbounds: List<ExternalId> = emptyList()
    private var destination: String = "Some destination"
    private var cellName: String = "без названия"
    private var lotStatus: LotInfo.Status = LotInfo.Status.CREATED

    fun setExternalId(externalId: ExternalId): LotInfoBuilder {
        this.externalId = externalId
        return this
    }

    fun setName(name: String): LotInfoBuilder {
        this.name = name
        return this
    }

    fun setBoxCount(boxCount: Int): LotInfoBuilder {
        this.boxCount = boxCount
        return this
    }

    fun setBoxes(boxes: List<String>): LotInfoBuilder {
        this.boxes = boxes
        return this
    }

    fun setInbounds(inbounds: List<ExternalId>): LotInfoBuilder {
        this.inbounds = inbounds
        return this
    }

    fun setDestination(destination: String): LotInfoBuilder {
        this.destination = destination
        return this
    }

    fun setCellName(cellName: String): LotInfoBuilder {
        this.cellName = cellName
        return this
    }

    fun setLotStatus(status: LotInfo.Status): LotInfoBuilder {
        this.lotStatus = status
        return this
    }

    fun build(): LotInfo =
        LotInfo(
            externalId = externalId,
            name = name,
            boxCount = boxCount,
            informationListCodes = inbounds,
            destination = destination,
            cellName = cellName,
            lotStatus = lotStatus
        )
}
