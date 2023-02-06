package ru.yandex.market.sc.test.network.mocks

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import ru.yandex.market.sc.core.data.acceptance.InitialAcceptanceResponse
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.cell.CellForRouteBase
import ru.yandex.market.sc.core.data.cell.CellToSort
import ru.yandex.market.sc.core.data.cell.CellWithOrders
import ru.yandex.market.sc.core.data.courier.Courier
import ru.yandex.market.sc.core.data.inbound.Inbound
import ru.yandex.market.sc.core.data.inbound.InboundCharacteristics
import ru.yandex.market.sc.core.data.inbound.InboundTask
import ru.yandex.market.sc.core.data.inbound.InboundTaskStatus
import ru.yandex.market.sc.core.data.lot.CellLot
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.core.data.lot.LotAcceptanceResponse
import ru.yandex.market.sc.core.data.lot.MovingLot
import ru.yandex.market.sc.core.data.lot.MovingLotInfo
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.data.order.RouteOrderId
import ru.yandex.market.sc.core.data.outbound.Outbound
import ru.yandex.market.sc.core.data.outbound.OutboundStatus
import ru.yandex.market.sc.core.data.place.Place
import ru.yandex.market.sc.core.data.place.PlaceStatus
import ru.yandex.market.sc.core.data.route.OutgoingCourierRouteType
import ru.yandex.market.sc.core.data.route.Route
import ru.yandex.market.sc.core.data.sortable.SortResponse
import ru.yandex.market.sc.core.data.sortable.SortResponse.Destination
import ru.yandex.market.sc.core.data.sortable.SortResponse.DestinationType
import ru.yandex.market.sc.core.data.sortable.SortableType
import ru.yandex.market.sc.core.data.sorting_center.ApiSortingCenter
import ru.yandex.market.sc.core.data.stage.Stage
import ru.yandex.market.sc.core.data.tasks.PrintTaskBase
import ru.yandex.market.sc.core.data.tasks.PrinterTask
import ru.yandex.market.sc.core.data.tasks.RouteTask
import ru.yandex.market.sc.core.data.tasks.RouteTaskEntry
import ru.yandex.market.sc.core.data.tasks.TemplateType
import ru.yandex.market.sc.core.data.transportation.TransportationInbound
import ru.yandex.market.sc.core.data.user.CheckUserData
import ru.yandex.market.sc.core.data.user.PropertyName
import ru.yandex.market.sc.core.data.user.Setting
import ru.yandex.market.sc.core.data.version.Version
import ru.yandex.market.sc.core.data.warehouse.Warehouse
import ru.yandex.market.sc.core.network.arch.data.PageImpl
import ru.yandex.market.sc.core.network.arch.data.PageableImpl
import ru.yandex.market.sc.core.network.arch.data.SortImpl
import ru.yandex.market.sc.core.utils.data.DateWithTime
import ru.yandex.market.sc.core.utils.data.DateWithoutTime
import ru.yandex.market.sc.core.utils.data.ExternalId
import java.util.Date
import ru.yandex.market.sc.core.data.destination.Destination as ControlDestination

object TestFactory {
    const val possibleOutgoingDateMock = "2020-12-01"

    private fun getSort(empty: Boolean = false, sorted: Boolean = false): SortImpl {
        return SortImpl(
            empty = empty,
            sorted = sorted,
            unsorted = !sorted,
        )
    }

    private fun getPageable(
        page: Int = 0,
        size: Int = 20,
        sort: SortImpl = getSort()
    ): PageableImpl {
        return PageableImpl(
            offset = 0,
            pageNumber = page,
            pageSize = size,
            paged = true,
            sort = sort,
            unpaged = false,
        )
    }

    fun <T> getPage(items: List<T>): PageImpl<T> {
        val sortImpl = getSort(empty = items.isEmpty())
        val pageableImpl = getPageable(sort = sortImpl)
        return PageImpl(
            content = items,
            empty = items.isEmpty(),
            first = true,
            last = false,
            number = 0,
            numberOfElements = items.size,
            pageable = pageableImpl,
            size = items.size,
            sort = sortImpl,
            totalElements = items.size.toLong(),
            totalPages = 1,
        )
    }

    private fun generateCellName(
        type: Cell.Type,
        subType: Cell.SubType,
    ): String = type.name[0] + subType.name.split("_").joinToString { it[0].toString() }

    private fun getCell(
        number: String? = null,
        placeCount: Int = 0,
        status: Cell.Status = Cell.Status.ACTIVE,
        type: Cell.Type = Cell.Type.COURIER,
        subType: Cell.SubType = Cell.SubType.DEFAULT,
        cargoType: Cell.CargoType = Cell.CargoType.UNKNOWN
    ): Cell {
        val cellId = IdManager.getId()
        val prefix = generateCellName(type, subType)
        val cellNumber = if (status == Cell.Status.ACTIVE) number ?: "$prefix-$cellId" else "null"

        return Cell(
            id = cellId,
            number = cellNumber,
            placeCount = placeCount,
            status = status,
            type = type,
            subType = subType,
            cargoType = cargoType
        )
    }

    fun getNotActiveCell() = getCell(number = null, status = Cell.Status.NOT_ACTIVE)

    fun getCourierCell(number: String? = null) = getCell(
        number = number,
        type = Cell.Type.COURIER,
    )

    fun getBufferCell(number: String? = null, subType: Cell.SubType = Cell.SubType.DEFAULT) =
        getCell(number, type = Cell.Type.BUFFER, subType = subType)

    fun getDroppedCell(number: String? = null) =
        getBufferCell(number, subType = Cell.SubType.DROPPED_ORDERS)

    fun getUtilizationCell(number: String? = null) =
        getCell(number, type = Cell.Type.RETURN, subType = Cell.SubType.UTILISATION)

    private fun getListOfAvailableReturnCells(amount: Int): List<Cell> =
        List(amount) { getReturnCell("R-$it") }

    fun getReturnCell(number: String? = null) = getCell(number, type = Cell.Type.RETURN)

    fun mapToCellWithOrders(
        cell: Cell,
        ordersAssignedToCell: Int = 0,
        routeId: Long = IdManager.getId(),
        ordersInCell: Int = 0,
        placeCount: Int = 0,
        acceptedButNotSortedPlaceCount: Int = 0,
        cellPrepared: Boolean = false,
        filledStatus: Boolean = false,
        showFilledStatus: Boolean = false,
    ) = CellWithOrders(
        id = cell.id,
        number = cell.number,
        status = cell.status,
        type = cell.type,
        subType = cell.subType,
        ordersAssignedToCell = ordersAssignedToCell,
        routeId = routeId,
        ordersInCell = ordersInCell,
        placeCount = placeCount,
        acceptedButNotSortedPlaceCount = acceptedButNotSortedPlaceCount,
        cellPrepared = cellPrepared,
        filledStatus = filledStatus,
        showFilledStatus = showFilledStatus,
    )

    fun getCellToSort(number: String, ordersCount: Int = 0, ordersToSortCount: Int = 0) =
        CellToSort(IdManager.getId(), number, ordersCount, ordersToSortCount)

    fun getSortResponse(cell: Cell, parentRequired: Boolean = false): SortResponse {
        val destination = Destination(
            ExternalId(cell.id),
            requireNotNull(cell.number),
            DestinationType.CELL
        )
        return SortResponse(destination, parentRequired)
    }

    fun getSortResponse(lot: CellLot, parentRequired: Boolean = false): SortResponse {
        val destination = Destination(lot.externalId, lot.name, DestinationType.LOT)
        return SortResponse(destination, parentRequired)
    }

    fun getTransportationInbound(
        transportationId: ExternalId,
        warehouseFrom: String? = null
    ): TransportationInbound {
        val id = IdManager.getId()

        return TransportationInbound(
            externalId = IdManager.getExternalId(id),
            transportationId = transportationId,
            warehouseFrom = warehouseFrom ?: "test"
        )
    }

    fun getInbound(
        type: Inbound.Type = Inbound.Type.DEFAULT,
        status: Inbound.Status = Inbound.Status.CREATED,
        informationListCode: ExternalId? = null,
        destination: String? = null,
        sortableType: SortableType? = null,
        boxes: List<String> = listOf(),
        pallets: List<String> = listOf(),
        unpackedBoxes: List<String> = listOf(),
        unsortedPallets: List<String> = listOf(),
    ): Inbound {
        val id = IdManager.getId()
        return Inbound(
            id = id,
            externalId = IdManager.getExternalId(id),
            type = type,
            status = status,
            informationListCode = informationListCode,
            sortableType = sortableType,
            info = Inbound.InboundInfo(
                boxes,
                pallets,
                unpackedBoxes,
                unsortedPallets
            ),
            destination = destination
        )
    }

    fun getInboundTask(
        keepedPallets: Int = 0,
        preparedPallets: Int = 0,
        sortedPallets: Int = 0,
        cells: List<String> = listOf(),
        status: InboundTaskStatus = InboundTaskStatus.NOT_STARTED,
    ) = InboundTask(
        informationListCode = IdManager.getExternalId().value,
        keepedPallets = keepedPallets,
        preparedPallets = preparedPallets,
        sortedPallets = sortedPallets,
        status = status,
        cells = cells,
    )

    fun createPalletCharacteristics(height: Int = 100, weight: Int = 100) =
        InboundCharacteristics.Pallet(
            height = height,
            weight = weight
        )

    fun getOutbound(
        externalId: ExternalId = IdManager.getExternalId(),
        destination: String = "destination",
        inboundCount: Int = 1,
        preparedPalletCount: Int = 2,
        plannedPalletCount: Int = 3,
        shipCell: String = "SHIP",
        status: OutboundStatus = OutboundStatus.NOT_FULLY_PREPARED
    ) = Outbound(
        destination = destination,
        externalId = externalId,
        inboundCount = inboundCount,
        plannedPalletCount = plannedPalletCount,
        preparedPalletCount = preparedPalletCount,
        shipCell = shipCell,
        status = status,
    )

    fun getCourier(name: String): Courier =
        Courier(name, IdManager.getId(), IdManager.getId())

    fun getDestination(id: ExternalId, name: String, type: DestinationType) =
        Destination(id, name, type)

    private fun getPlaceNotFound(orderExternalId: ExternalId) = Place(
        externalId = orderExternalId,
        status = PlaceStatus.ERROR,
        lotSortAvailable = false,
        middleMile = false,
        availableCells = listOf(),
        availableLots = listOf(),
        deliveryServiceName = null,
        warehouse = null,
        routeTo = null,
        cell = null,
        currentLot = null,
        possibleOutgoingRouteDate = null,
        useZoneForBufferReturnCells = false,
    )

    fun getOrderNotFound(externalId: ExternalId = IdManager.getExternalId(IdManager.getId())) =
        Order(
            id = null,
            externalId = externalId,
            places = listOf(getPlaceNotFound(externalId)),
        )

    fun getOrderReturnAlreadyDispatched(externalId: ExternalId) = Order(
        id = null,
        externalId = externalId,
        places = listOf(getPlaceNotFound(externalId)),
    )

    fun createPrinterTask(
        status: PrinterTask.Status = PrinterTask.Status.CREATED,
        fields: List<String> = listOf()
    ) = PrinterTask(
        id = IdManager.getId(),
        jobId = null,
        initiatorId = null,
        destination = "mocked printer",
        destinationType = "ZPL",
        status = status,
        fields = fields,
        copies = 2,
        templateType = TemplateType.LOT,
        createdAt = Date(),
        statusUpdatedAt = Date(),
        lastError = null,
    )

    fun mapToBase(printerTask: PrinterTask) =
        PrintTaskBase(
            id = printerTask.id,
            templateType = printerTask.templateType,
            status = printerTask.status,
        )

    fun createRouteTask(
        cell: Cell? = getCell(),
        cellIndex: Int = 1,
        totalCellCount: Int = 1,
    ) = RouteTask(cell, cellIndex, totalCellCount)

    fun createRouteTaskEntry(name: String = "route task entry") =
        RouteTaskEntry(id = IdManager.getId(), name)

    fun createCellLot(
        lotName: String? = null,
        lotType: SortableType = SortableType.PALLET,
        parentCellId: Long = IdManager.getId(),
        externalId: ExternalId? = null,
        status: Lot.Status = Lot.Status.UNKNOWN
    ): CellLot {
        val lotId = IdManager.getId()
        val lotExternalId = externalId ?: ExternalId("ext-$lotId")

        return CellLot(
            externalId = lotExternalId,
            id = IdManager.getId(),
            name = lotName ?: lotExternalId.value,
            type = lotType,
            parentCellId = parentCellId,
            status = status
        )
    }

    fun createLot(): LotBuilder = LotBuilder.create()

    fun createStamp(): ExternalId = IdManager.generateExternalId()

    fun createOrder(): OrderBuilder = OrderBuilder.create()

    fun createLotInfo(): LotInfoBuilder = LotInfoBuilder.create()

    fun createSortResponseWithCell(
        destinationId: ExternalId = ExternalId("Cell-1"),
        parentRequired: Boolean = false
    ) = SortResponseBuilder.create()
        .setDestination(SortResponseBuilder.cellDestination(id = destinationId))
        .setParentRequired(parentRequired)
        .build()

    fun createMovingLotWithErrorCode(
        errorCode: MovingLot.ErrorCode,
        status: Lot.Status = Lot.Status.CREATED
    ) = MovingLotBuilder.create()
        .setErrorCode(errorCode)
        .setStatus(status)
        .build()

    fun createMovingLotInoWithErrorCode(
        errorCode: MovingLotInfo.ErrorCode,
    ) = MovingLotInfoBuilder.create()
        .setErrorCode(errorCode)
        .build()

    fun createSortResponseWithLot(
        destinationId: ExternalId = ExternalId("Lot-1"),
        parentRequired: Boolean = false
    ) = SortResponseBuilder.create()
        .setDestination(SortResponseBuilder.lotDestination(id = destinationId))
        .setParentRequired(parentRequired)
        .build()

    fun createOrderForTodayWithLotsAvailable(numberOfPlaces: Int = 1) =
        OrderBuilder.createForToday(numberOfPlaces)
            .updatePalletizationRequired(true)
            .withAvailableLots(listOf(createCellLot()))
            .build()

    fun createSortedOrderWithLotSortAvailable(cell: Cell) =
        OrderBuilder.createForToday(1)
            .sort(cell)
            .updatePalletizationRequired(true)
            .withAvailableLots(listOf(createCellLot()))
            .withAvailableCells(listOf(cell))
            .build()

    fun createOrderInLot(status: Lot.Status): Order {
        val cellLot = createCellLot(status = status)

        return OrderBuilder.createForToday(1)
            .updatePalletizationRequired(true)
            .withAvailableLots(listOf(createCellLot(), cellLot))
            .updateCurrentLot(cellLot)
            .build()
    }

    fun createOrderWithOnePlaceInLot(numberOfPlaces: Int = 1, status: Lot.Status): Order {
        val cellLot = createCellLot(status = status)

        return OrderBuilder.createForToday(numberOfPlaces - 1)
            .addPlaceWithCurrentLot(cellLot)
            .updatePalletizationRequired(true)
            .withAvailableLots(listOf(createCellLot(), cellLot))
            .build()
    }

    fun createOrderForToday(numberOfPlaces: Int) = OrderBuilder.createForToday(numberOfPlaces)

    fun getOrderToKeep(numberOfPlaces: Int = 1, possibleOutgoingRouteDate: String): Order =
        createOrderForToday(numberOfPlaces)
            .keep()
            .updatePossibleOutgoingRouteDate(possibleOutgoingRouteDate)
            .build()

    fun getCanceledOrder(
        numberOfPlaces: Int = 1,
        cell: Cell,
        amountOfAvailableCells: Int = 0
    ): Order =
        createOrderForToday(numberOfPlaces)
            .cancel()
            .sort(cell)
            .withAvailableCells(getListOfAvailableReturnCells(amountOfAvailableCells))
            .build()

    fun getInitialAcceptanceResponse(
        placeExternalId: ExternalId? = null,
        warehouse: Warehouse? = null,
        acceptDateTime: String? = null,
        userAcceptedBy: String? = null,
        stage: Stage? = null,
        status: InitialAcceptanceResponse.StatusCode,
    ): InitialAcceptanceResponse {
        return InitialAcceptanceResponse(
            placeExternalId = placeExternalId,
            acceptStatusCode = status,
            warehouse = warehouse,
            acceptDateTime = acceptDateTime?.let { DateWithTime(it) },
            userAcceptedBy = userAcceptedBy,
            stage = stage,
        )
    }

    fun getLotAcceptanceResponse(
        lotExternalId: ExternalId,
        inboundExternalId: ExternalId? = null,
        acceptDateTime: String? = null,
        userAcceptedBy: String? = null,
        inboundDate: String? = null,
        placeCount: Int,
        scFrom: String = "default2",
        scTo: String = "default1",
        lotStatus: Lot.Status = Lot.Status.PROCESSING,
        status: LotAcceptanceResponse.StatusCode,
    ): LotAcceptanceResponse {
        return LotAcceptanceResponse(
            acceptStatusCode = status,
            acceptDateTime = acceptDateTime?.let { DateWithTime(it) },
            userAcceptedBy = userAcceptedBy,
            scTo = scTo,
            scFrom = scFrom,
            placeCount = placeCount,
            lotStatus = lotStatus,
            inboundDate = inboundDate?.let { DateWithoutTime(it) },
            inboundExternalId = inboundExternalId,
            lotExternalId = lotExternalId,
            isCrossDock = false,
        )
    }

    fun getReturnFromCourier(
        numberOfPlaces: Int = 1,
        cellTo: Cell? = null,
        cell: Cell? = null,
        possibleOutgoingRouteDate: String = possibleOutgoingDateMock,
        amountOfAvailableCells: Int = 2,
    ): Order = createOrderForToday(numberOfPlaces)
        .sort(cell)
        .updateCellTo(cellTo)
        .withAvailableCells(getListOfAvailableReturnCells(amountOfAvailableCells))
        .cancel()
        .updatePossibleOutgoingRouteDate(possibleOutgoingRouteDate)
        .build()

    fun getOrderOk(
        numberOfPlaces: Int = 1,
        possibleOutgoingRouteDate: String = possibleOutgoingDateMock,
        cell: Cell = OrderBuilder.courierCell(),
    ): Order =
        createOrderForToday(numberOfPlaces)
            .updatePossibleOutgoingRouteDate(possibleOutgoingRouteDate)
            .updateCellTo(cell)
            .sort(cell)
            .build()

    fun getOrderToKeepInCell(
        numberOfPlaces: Int = 1,
        cell: Cell,
        possibleOutgoingRouteDate: String
    ): Order =
        createOrderForToday(numberOfPlaces)
            .keep()
            .sort(cell)
            .updatePossibleOutgoingRouteDate(possibleOutgoingRouteDate)
            .build()

    fun getOrderDropped(
        numberOfPlaces: Int = 1,
        cellTo: Cell,
        possibleOutgoingRouteDate: String? = null
    ): Order =
        createOrderForToday(numberOfPlaces)
            .drop(cellTo)
            .updatePossibleOutgoingRouteDate(possibleOutgoingRouteDate)
            .build()

    fun getOrderDroppedInCell(
        numberOfPlaces: Int = 1,
        cellTo: Cell,
        cell: Cell,
        possibleOutgoingRouteDate: String = possibleOutgoingDateMock,
    ): Order = createOrderForToday(numberOfPlaces)
        .sort(cell)
        .drop(cellTo)
        .updatePossibleOutgoingRouteDate(possibleOutgoingRouteDate)
        .build()

    fun getOrderToCourier(
        numberOfPlaces: Int = 1,
        cellTo: Cell,
        cell: Cell? = null,
        isLotsAvailable: Boolean = false
    ): Order = createOrderForToday(numberOfPlaces)
        .updateCellTo(cellTo)
        .sort(cell)
        .updatePalletizationRequired(isLotsAvailable)
        .build()

    fun getOrderToUtilize(
        numberOfPlaces: Int = 1,
    ): Order = createOrderForToday(numberOfPlaces)
        .utilize()
        .withAvailableCells(
            listOf(
                OrderBuilder.utilizationCell("util-1"),
                OrderBuilder.utilizationCell("util-2")
            )
        )
        .build()

    fun getOrderToReturn(
        numberOfPlaces: Int = 1,
        cellTo: Cell,
        cell: Cell? = null,
        routeId: Long? = null,
    ): Order = createOrderForToday(numberOfPlaces)
        .cancel(cellTo)
        .sort(cell)
        .optional(routeId != null) {
            it.updateRouteTo(requireNotNull(routeId))
        }
        .build()

    fun getOrderToReturn(
        placeExternalIds: List<String>,
        cellTo: Cell,
        cell: Cell? = null,
        routeId: Long? = null,
    ): Order = createOrderForToday(placeExternalIds.size)
        .cancel(cellTo)
        .places(*placeExternalIds.toTypedArray())
        .sort(cell)
        .optional(routeId != null) {
            it.updateRouteTo(requireNotNull(routeId))
        }
        .build()

    fun getOrderFromCourier(
        numberOfPlaces: Int = 1,
        cellTo: Cell? = null,
        cell: Cell? = null,
        possibleOutgoingRouteDate: String = possibleOutgoingDateMock,
    ): Order = createOrderForToday(numberOfPlaces)
        .sort(cell)
        .updateCellTo(cellTo)
        .updatePossibleOutgoingRouteDate(possibleOutgoingRouteDate)
        .build()

    fun getOrderWithLots(
        numberOfPlaces: Int = 1,
        lot: CellLot? = null,
        cell: Cell? = null,
        lotsTo: List<CellLot> = listOf(),
        routeId: Long? = null,
    ): Order {
        return createOrderForToday(numberOfPlaces)
            .sort(cell)
            .updateCellTo(cell)
            .withAvailableLots(lotsTo)
            .updateCurrentLot(lot)
            .optional(routeId != null) {
                it.updateRouteTo(routeId!!)
            }
            .build()
    }

    fun getOrderWithLotsNotInCell(numberOfPlaces: Int = 1, routeId: Long? = null): Order {
        return createOrderForToday(numberOfPlaces)
            .cancel()
            .optional(routeId != null) {
                it.updateRouteTo(routeId!!)
            }
            .build()
    }

    data class RoutePlaceIdBlueprint(val status: RouteOrderId.Status, val cell: Cell? = null)

    fun getRouteOrderId(
        status: RouteOrderId.Status = RouteOrderId.Status.IN_RIGHT_CELL,
        cell: Cell? = null,
        numberOfPlaces: Int = 1,
        places: List<RoutePlaceIdBlueprint>? = null,
        baseOnOrder: Order? = null,
    ): RouteOrderId {
        val id = if (baseOnOrder?.id != null) {
            baseOnOrder.id!!
        } else {
            IdManager.getId()
        }
        val externalId = IdManager.getExternalId(id)

        val routePlaceIds = when {
            places != null -> places.mapIndexed { index, placeBlueprint ->
                RouteOrderId.RoutePlaceId(
                    externalId = IdManager.getIndexedExternalId(externalId, index),
                    status = placeBlueprint.status,
                    cell = placeBlueprint.cell
                )
            }
            numberOfPlaces > 0 -> arrayOfNulls<RouteOrderId.RoutePlaceId>(numberOfPlaces).mapIndexed { index, _ ->
                RouteOrderId.RoutePlaceId(
                    externalId = IdManager.getIndexedExternalId(externalId, index),
                    status = status,
                    cell = cell,
                )
            }
            else -> listOf()
        }

        return RouteOrderId(
            id = id,
            externalId = externalId,
            places = routePlaceIds,
        )
    }

    fun getRouteOrderIds(): List<RouteOrderId> = listOf(
        getRouteOrderId(RouteOrderId.Status.IN_RIGHT_CELL, getCourierCell()),
        getRouteOrderId(RouteOrderId.Status.NOT_ACCEPTED_AT_SORTING_CENTER, null)
    )

    fun getRoute(
        status: Route.Status = Route.Status.NOT_STARTED,
        warehouseName: String? = null,
        lotsTotal: Int = 0,
        ordersTotal: Int = 0,
        ordersShipped: Int? = null,
        courier: Courier? = null,
        cells: List<CellForRouteBase> = listOf(),
        outgoingCourierRouteType: OutgoingCourierRouteType = OutgoingCourierRouteType.COURIER,
    ) = Route(
        id = IdManager.getId(),
        status = status,
        warehouseName = warehouseName,
        lotsTotal = lotsTotal,
        ordersTotal = ordersTotal,
        ordersShipped = ordersShipped,
        courier = courier,
        cells = cells,
        outgoingCourierRouteType = outgoingCourierRouteType,
    )

    fun getWarehouse(name: String = "warehouse") =
        Warehouse(IdManager.getId(), name, type = Warehouse.Type.SHOP)

    const val ERROR_MESSAGE = "Simple message"

    fun <T> getResponseError(
        code: Int,
        errorMessage: String = ERROR_MESSAGE,
        error: String = "ERROR_CODE",
    ): Response<T> {
        val mediaType = "application/json".toMediaType()
        val body = ("{" +
            "\"timestamp\":\"2020-07-21T09:07:54.157360Z\"," +
            "\"status\":$code," +
            "\"error\":\"$error\"," +
            "\"message\":\"$errorMessage\"," +
            "\"path\":\"path/to/api/method\"" +
            "}").toResponseBody(mediaType)
        return Response.error(code, body)
    }

    fun getVersion(
        versionCode: Int,
        url: String = "https://yadi.sk/d/mock-apk-url",
        description: String? = null,
        delaysAmount: Int? = null,
        updateAfter: Date? = null,
    ) = Version(versionCode, description, updateAfter, delaysAmount, Version.UrlHolder(url, url))

    private fun createSortingCenter(
        id: Long = IdManager.getId(),
        name: String = "sc-$id"
    ): ApiSortingCenter {
        return ApiSortingCenter(id, name)
    }

    fun createCheckUserData(
        sortingCenter: ApiSortingCenter = createSortingCenter(),
        properties: Map<PropertyName, Boolean> = mapOf(),
        settings: Map<Setting, Boolean> = mapOf(),
        controls: List<ControlDestination> = listOf(),
    ) = CheckUserData(sortingCenter, properties, settings, controls)
}
