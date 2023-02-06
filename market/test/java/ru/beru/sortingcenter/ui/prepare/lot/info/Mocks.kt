package ru.beru.sortingcenter.ui.prepare.lot.info

import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.test.network.mocks.TestFactory

internal object Mocks {
    object Lots {
        val withReadyForShipmentAction = TestFactory
            .createLot()
            .setStatus(Lot.Status.PROCESSING)
            .setActions(listOf(Lot.Action.READY_FOR_SHIPMENT))
            .build()

        val withReadyForPackingAction = TestFactory
            .createLot()
            .setStatus(Lot.Status.PROCESSING)
            .setActions(listOf(Lot.Action.READY_FOR_PACKING))
            .build()

        val withNotReadyForShipmentAction = TestFactory
            .createLot()
            .setStatus(Lot.Status.READY)
            .setActions(listOf(Lot.Action.NOT_READY_FOR_SHIPMENT))
            .build()

        val withAddStampAction = TestFactory
            .createLot()
            .setStatus(Lot.Status.PROCESSING)
            .setActions(listOf(Lot.Action.ADD_STAMP))
            .build()

        val withDeleteStampAction = TestFactory
            .createLot()
            .setStatus(Lot.Status.PROCESSING)
            .setActions(listOf(Lot.Action.DELETE_STAMP, Lot.Action.READY_FOR_SHIPMENT))
            .build()

        val transferableLot = TestFactory
            .createLot()
            .setTransferable(true)
            .build()
    }
}
