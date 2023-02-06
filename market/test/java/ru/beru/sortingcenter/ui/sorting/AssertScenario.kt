package ru.beru.sortingcenter.ui.sorting

import ru.beru.sortingcenter.R
import ru.yandex.market.sc.core.data.cell.Cell
import ru.yandex.market.sc.core.data.order.Order
import ru.yandex.market.sc.core.data.place.PlaceStatus
import ru.yandex.market.sc.core.data.place.Place
import ru.yandex.market.sc.core.data.sortable.SortResponse.DestinationType
import ru.yandex.market.sc.core.data.sortable.SortResponse.DestinationType.CELL
import ru.yandex.market.sc.core.ui.data.formatter.LotFormatter
import ru.yandex.market.sc.core.data.scanner.OverlayState
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.beru.sortingcenter.ui.sorting.orders.models.scanner.ScannerModeImpl as ScannerMode

object AssertScenario {
    private const val expectedDateMock = "01.12.2020"

    fun `order not found`() {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.error,
            labelColor = R.color.red,
        )

        Asserts.`assert description`(
            isDescriptionVisible = true,
            description = R.string.order_not_found,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `already scanned order`(cell: Cell) {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.order_already_scanned,
            labelColor = R.color.black,
        )

        Asserts.`assert description`(
            isDescriptionVisible = false,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = cell.number,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `scan cell with error`(responseErrorMessage: String = TestFactory.ERROR_MESSAGE) {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.Failure,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.error,
            labelColor = R.color.red,
        )

        Asserts.`assert description text`(
            isDescriptionVisible = true,
            description = responseErrorMessage,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `cell not active`() {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Warning,
            overlayMessage = R.string.cell_not_active_warning,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.empty,
            labelColor = R.color.red,
        )

        Asserts.`assert description`(
            isDescriptionVisible = false,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = "null",
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `scan order with error`() {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Failure,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.error,
            labelColor = R.color.red,
        )

        Asserts.`assert description text`(
            isDescriptionVisible = true,
            description = TestFactory.ERROR_MESSAGE,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `ready to scan place`(order: Order) {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.PlaceBarcode,
            overlayState = OverlayState.Success,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.order_external_id,
            labelColor = R.color.black,
            externalId = order.externalId,
        )

        Asserts.`assert description`(
            isDescriptionVisible = true,
            description = R.string.scan_second_barcode,
            isInfoButtonVisible = true,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `waiting for place`(order: Order) {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.PlaceBarcode,
            overlayState = OverlayState.None,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            labelColor = R.color.black,
            label = R.string.order_external_id,
            externalId = order.externalId,
        )

        Asserts.`assert description`(
            isDescriptionVisible = true,
            description = R.string.scan_second_barcode,
            isInfoButtonVisible = true,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `ready to scan cell`(place: Place) {
        val shouldShowAvailableCells = place.status != PlaceStatus.KEEP_TO_WAREHOUSE

        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.CellQRCode,
            overlayState = if (place.status == PlaceStatus.KEEP) OverlayState.Warning else OverlayState.Success,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = when (place.status) {
                PlaceStatus.KEEP -> R.string.label_order_keep
                PlaceStatus.KEEP_TO_WAREHOUSE -> R.string.label_order_keep_to_warehouse
                PlaceStatus.SORT_TO_UTILIZATION -> R.string.label_order_sort_to_utilization
                else -> R.string.empty
            },
            labelColor = R.color.black,
        )

        Asserts.`assert description`(
            isDescriptionVisible = place.isToKeep,
            description = if (place.isToKeep) R.string.cell_any_keep_suitable else null,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = place.isToKeep,
            shouldShowExpectedDateTitle = false,
            dateFormat = if (place.isToKeep) R.string.dispatch_data_unknown else null,
            expectedDate = if (place.isToKeep) expectedDateMock else null,
        )
    }

    fun `wait for destination`(place: Place, withReset: Boolean = false) {
        val currentLot = place.currentLot

        Asserts.`assert current lot information`(
            isTextAvailable = currentLot != null && place.lotSortAvailable,
            name = currentLot?.name,
            status = currentLot?.status?.let { LotFormatter.getStatusName(it) },
        )

        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.DestinationQRCode,
            overlayState = if (withReset) OverlayState.None
            else OverlayState.Success,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.empty,
            labelColor = R.color.black,
        )

        Asserts.`assert description visible`(isDescriptionVisible = true)
    }

    fun `wait for parent cell`(place: Place, withReset: Boolean = false) {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.CellQRCode,
            overlayState = if (withReset) OverlayState.None
            else OverlayState.Success
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.sorting_scan_parent_cell_from_list,
            labelColor = R.color.black
        )

        Asserts.`assert description text`(
            isDescriptionVisible = true,
            description = place.joinedCellInfo,
        )
    }

    fun `scan destination with error`(
        responseErrorMessage: String = TestFactory.ERROR_MESSAGE,
        type: DestinationType = CELL
    ) {
        Asserts.`assert scanner fragment`(
            scannerMode = if (type == CELL) ScannerMode.CellQRCode else ScannerMode.DestinationQRCode,
            overlayState = OverlayState.Failure,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.error,
            labelColor = R.color.red,
        )

        Asserts.`assert description text`(
            isDescriptionVisible = true,
            description = responseErrorMessage,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `order sorted to lot`(externalId: ExternalId, lotName: String) {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Success,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.order_external_id,
            externalId = externalId,
            labelColor = R.color.green,
        )

        Asserts.`assert description text`(
            isDescriptionVisible = true,
            description = R.string.order_was_sorted_in_lot,
            lotName = lotName,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `waiting for cell`(place: Place) {
        val shouldShowAvailableCells = place.status != PlaceStatus.KEEP_TO_WAREHOUSE

        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.CellQRCode,
            overlayState = OverlayState.None,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = when (place.status) {
                PlaceStatus.KEEP -> R.string.label_order_keep
                PlaceStatus.KEEP_TO_WAREHOUSE -> R.string.label_order_keep_to_warehouse
                PlaceStatus.SORT_TO_UTILIZATION -> R.string.label_order_sort_to_utilization
                else -> R.string.empty
            },
            labelColor = R.color.black,
        )

        Asserts.`assert description`(
            isDescriptionVisible = place.isToKeep,
            description = if (place.isToKeep) R.string.cell_any_keep_suitable else null,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = place.isToKeep,
            shouldShowExpectedDateTitle = false,
            dateFormat = if (place.isToKeep) R.string.dispatch_data_unknown else null,
            expectedDate = if (place.isToKeep) expectedDateMock else null,
        )
    }

    fun `order sort to cell`() {
        Asserts.`assert scanner fragment`(
            scannerMode = ScannerMode.OrderBarcode,
            overlayState = OverlayState.Success,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.successfully,
            labelColor = R.color.green,
        )

        Asserts.`assert description`(
            isDescriptionVisible = true,
            description = R.string.order_accepted_scan_next,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }

    fun `scan with wrong format`(mode: ScannerMode) {
        Asserts.`assert scanner fragment`(
            scannerMode = mode,
            overlayState = OverlayState.Failure,
        )

        Asserts.`assert label`(
            isLabelAvailable = true,
            label = R.string.error,
            labelColor = R.color.red,
        )

        Asserts.`assert description`(
            isDescriptionVisible = true,
            description = mode.wrongFormatText,
        )

        Asserts.`assert sort destination info`(
            shouldShow = true,
            destinationNumber = R.string.empty,
        )

        Asserts.`assert expected date`(
            shouldShowExpectedDate = false,
            shouldShowExpectedDateTitle = false,
        )
    }
}
