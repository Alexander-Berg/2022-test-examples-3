package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.lot.CellLot
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.data.place.Place
import ru.yandex.market.sc.core.data.place.PlaceStatus
import ru.yandex.market.sc.core.utils.data.DateMapper
import ru.yandex.market.sc.core.utils.data.DateWithoutTime
import ru.yandex.market.sc.core.utils.data.ExternalId
import java.util.Date

class OrderBuilder private constructor() {
    private var id: Long = IdManager.getId()
    private val externalId: ExternalId get() = IdManager.getExternalId(id)
    private val defaultPlace
        get() = Place(
            externalId = IdManager.getIndexedExternalId(externalId, 0),
            status = PlaceStatus.UNKNOWN,
            cell = null,
            currentLot = null,
            availableLots = listOf(),
            availableCells = listOf(),
            lotSortAvailable = false,
            middleMile = false,
            possibleOutgoingRouteDate = null,
            warehouse = null,
            useZoneForBufferReturnCells = true,
            deliveryServiceName = null,
            routeTo = null,
        )

    private var availableCells: List<Cell> = listOf()
    private var availableLots: List<CellLot> = listOf()
    private var lotSortAvailable: Boolean = false
    private var middleMile: Boolean = false
    private var places: List<Place> = listOf(defaultPlace)
    private var routeId: Long? = null
    private var status: PlaceStatus = PlaceStatus.UNKNOWN

    fun id(value: Long): OrderBuilder = apply {
        id = value
    }

    fun sort(cell: Cell? = this.firstAvailableCell): OrderBuilder = apply {
        this.places =
            this.places.map { it.copy(cell = cell, status = updateStatusAfterSort(it.status)) }
        this.status = updateStatusAfterSort(this.status)
    }

    fun sort(
        placeExternalIds: Array<ExternalId>,
        cell: Cell? = this.firstAvailableCell
    ): OrderBuilder = apply {
        this.places = this.places.map {
            if (placeExternalIds.contains(it.externalId)) {
                it.copy(cell = cell, status = updateStatusAfterSort(it.status))
            } else it
        }
        if (this.places.all { it.cell == cell }) {
            this.status = updateStatusAfterSort(this.status)
        }
    }

    fun sort(vararg placeIndexes: Int, cell: Cell? = this.firstAvailableCell): OrderBuilder =
        apply {
            val placesExternalIds = this.places
                .filterIndexed { index, _ -> placeIndexes.contains(index) }
                .map { it.externalId }
                .toTypedArray()

            sort(placesExternalIds, cell = cell)
        }

    private val firstAvailableCell: Cell?
        get() = availableCells.getOrNull(0)

    private fun updateStatusAfterSort(status: PlaceStatus): PlaceStatus = when (status) {
        PlaceStatus.SORT_TO_COURIER,
        PlaceStatus.SORT_TO_WAREHOUSE,
        -> PlaceStatus.OK
        else -> status
    }

    fun keep(): OrderBuilder = apply {
        this.availableCells = listOf()
        this.status = PlaceStatus.KEEP
        this.places =
            this.places.map { it.copy(status = PlaceStatus.KEEP, availableCells = listOf()) }

        updatePossibleOutgoingRouteDate(TestFactory.possibleOutgoingDateMock)
    }

    fun drop(cellTo: Cell = dropCell()): OrderBuilder = apply {
        this.availableCells = listOf(cellTo)
        this.status = PlaceStatus.KEEP
        this.places =
            this.places.map { it.copy(status = PlaceStatus.KEEP, availableCells = listOf(cellTo)) }
    }

    fun cancel(cellTo: Cell = returnCell()): OrderBuilder = apply {
        this.status = PlaceStatus.SORT_TO_WAREHOUSE
        this.availableCells = listOf(cellTo)
        this.places = this.places.map {
            it.copy(
                status = PlaceStatus.SORT_TO_WAREHOUSE,
                availableCells = listOf(cellTo)
            )
        }
    }

    fun utilize(): OrderBuilder = apply {
        this.status = PlaceStatus.SORT_TO_UTILIZATION
        this.places = this.places.map { it.copy(status = PlaceStatus.SORT_TO_UTILIZATION) }
    }

    fun withAvailableCells(cellsList: List<Cell>): OrderBuilder = apply {
        this.availableCells = cellsList
        this.places = this.places.map { it.copy(availableCells = cellsList) }
    }

    fun withAvailableLots(lotsList: List<CellLot>): OrderBuilder = apply {
        this.availableLots = lotsList
        this.places = this.places.map { it.copy(availableLots = lotsList) }
    }

    @Deprecated("must not override placeExternalId")
    fun places(vararg placeExternalIds: String): OrderBuilder = apply {
        this.places = placeExternalIds.map {
            defaultPlace.copy(externalId = ExternalId(it), status = this.status)
        }
    }

    private fun places(placeExternalIds: Array<ExternalId>): OrderBuilder = apply {
        this.places =
            placeExternalIds.map { defaultPlace.copy(externalId = it, status = this.status) }
    }

    private fun places(numberOfPlaces: Int): OrderBuilder = apply {
        val placeExternalIds = Array(numberOfPlaces) { ind ->
            IdManager.getIndexedExternalId(this.externalId, ind)
        }
        places(placeExternalIds)
    }

    fun updateStatus(status: PlaceStatus): OrderBuilder = apply {
        this.status = status
        this.places = this.places.map { it.copy(status = status) }
    }

    fun updateShipmentDate(date: Date): OrderBuilder = apply {
        this.places =
            this.places.map { it.copy(possibleOutgoingRouteDate = DateWithoutTime(DateMapper.format(date))) }
    }

    fun updateCurrentLot(currentLot: CellLot?): OrderBuilder = apply {
        this.places = this.places.map { it.copy(currentLot = currentLot) }
    }

    fun addPlaceWithCurrentLot(currentLot: CellLot): OrderBuilder = apply {
        val placeExternalId = IdManager.getIndexedExternalId(this.externalId, this.places.size + 1)
        val newPlace = defaultPlace.copy(
            externalId = placeExternalId,
            status = this.status,
            currentLot = currentLot
        )
        this.places = listOf(*this.places.toTypedArray(), newPlace)
    }

    fun updateCellTo(cellTo: Cell? = courierCell()): OrderBuilder = apply {
        val cells = if (cellTo != null) listOf(cellTo) else listOf()
        this.availableCells = cells
        this.places = this.places.map { it.copy(availableCells = cells) }
    }

    fun updatePalletizationRequired(lotSortAvailable: Boolean = false): OrderBuilder = apply {
        this.lotSortAvailable = lotSortAvailable
        this.places = this.places.map { it.copy(lotSortAvailable = lotSortAvailable) }
    }

    fun updateIsMiddleMile(middleMile: Boolean = false): OrderBuilder = apply {
        this.middleMile = middleMile
        this.places = this.places.map { it.copy(middleMile = middleMile) }
    }

    fun updatePossibleOutgoingRouteDate(possibleOutgoingRouteDate: String?): OrderBuilder = apply {
        val date = possibleOutgoingRouteDate?.let { DateWithoutTime(it) }
        this.places = this.places.map { it.copy(possibleOutgoingRouteDate = date) }
    }

    fun updateRouteTo(routeId: Long): OrderBuilder = apply {
        this.routeId = routeId
    }

    inline fun optional(condition: Boolean, action: (OrderBuilder) -> OrderBuilder) = when {
        condition -> action(this)
        else -> this
    }

    fun build() = Order(
        id = id,
        externalId = externalId,
        places = places.map { it.copy(routeTo = Place.RouteTo(id = routeId, warehouse = null)) },
    )

    companion object {
        fun create(): OrderBuilder {
            return OrderBuilder()
        }

        fun createForToday(numberOfPlaces: Int): OrderBuilder {
            return create()
                .places(numberOfPlaces)
                .updateStatus(PlaceStatus.SORT_TO_COURIER)
                .updateShipmentDate(Date())
                .updateCellTo(courierCell())
        }

        fun courierCell(): Cell = TestFactory.getCourierCell()

        fun bufferCell(): Cell = TestFactory.getBufferCell()

        fun dropCell(): Cell = TestFactory.getDroppedCell()

        fun utilizationCell(number: String): Cell = TestFactory.getUtilizationCell(number)

        fun returnCell() = TestFactory.getReturnCell()
    }
}
