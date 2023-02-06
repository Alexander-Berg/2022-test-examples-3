package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.lot.Lot.Action
import ru.yandex.market.sc.core.data.lot.Lot.Status
import ru.yandex.market.sc.core.data.lot.LotDimensions
import ru.yandex.market.sc.core.data.sortable.SortableType

class LotBuilder private constructor() {
    private var id: Long = IdManager.getId()
    private var externalId = IdManager.getExternalId(id)
    private var name: String = "без названия"
    private var status: Status = Status.CREATED
    private var type: SortableType = SortableType.PALLET
    private var labelCanBePrinted: Boolean = false
    private var transferable: Boolean = false
    private var actions: MutableList<Action> = mutableListOf()
    private var dimensions: LotDimensions = testLotDimensions()

    fun setName(name: String): LotBuilder = apply {
        this.name = name
    }

    fun setStatus(status: Status): LotBuilder = apply {
        this.status = status
    }

    fun setType(type: SortableType): LotBuilder = apply {
        this.type = type
    }

    fun setCanPrintQr(labelCanBePrinted: Boolean) = apply {
        this.labelCanBePrinted = labelCanBePrinted
    }

    fun setActions(actions: List<Action>): LotBuilder = apply {
        this.actions = actions.toMutableList()
    }

    fun addAction(action: Action): LotBuilder = apply {
        this.actions.add(action)
    }

    fun setTransferable(transferable: Boolean): LotBuilder = apply {
        this.transferable = transferable
    }

    fun removeAction(action: Action): LotBuilder = apply {
        this.actions = this.actions.filter { it != action }.toMutableList()
    }

    fun setLotDimensions(dimensions: LotDimensions) = apply {
        this.dimensions = dimensions
    }

    fun build() = Lot(
        id = id,
        externalId = externalId,
        name = name,
        status = status,
        type = type,
        transferable = transferable,
        labelCanBePrinted = labelCanBePrinted,
        actions = actions,
        dimensions = dimensions,
    )

    companion object {
        fun create(): LotBuilder {
            return LotBuilder()
        }
    }
}
