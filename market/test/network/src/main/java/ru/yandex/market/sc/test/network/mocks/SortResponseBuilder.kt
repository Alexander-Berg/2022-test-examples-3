package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.sortable.SortResponse
import ru.yandex.market.sc.core.data.sortable.SortResponse.Destination
import ru.yandex.market.sc.core.utils.data.ExternalId

class SortResponseBuilder private constructor() {
    private lateinit var destination: Destination
    private var parentRequired: Boolean = false

    fun setParentRequired(parentRequired: Boolean) = apply {
        this.parentRequired = parentRequired
    }

    fun setDestination(destination: Destination) = apply {
        this.destination = destination
    }

    fun build() = SortResponse(destination, parentRequired)

    companion object {
        fun create(): SortResponseBuilder {
            return SortResponseBuilder()
        }

        fun cellDestination(id: ExternalId, name: String = "Ячейка"): Destination =
            TestFactory.getDestination(
                type = SortResponse.DestinationType.CELL,
                id = id,
                name = name
            )

        fun lotDestination(id: ExternalId, name: String = "Лот"): Destination {
            return TestFactory.getDestination(
                type = SortResponse.DestinationType.LOT,
                id = id,
                name = name
            )
        }

    }
}