package ru.beru.sortingcenter.ui.prepare.lot.scan

import ru.beru.sortingcenter.R
import ru.yandex.market.sc.core.data.lot.Lot
import ru.yandex.market.sc.test.network.mocks.TestFactory

internal object Mocks {
    object Lots {
        val preshippable = TestFactory
            .createLot()
            .setStatus(Lot.Status.PROCESSING)
            .setActions(listOf(Lot.Action.READY_FOR_SHIPMENT))
            .build()

        val notFound = TestFactory
            .createLot()
            .build()
    }

    val stringResources = mapOf(
        R.string.lot_choose_to_preship to "lot_choose_to_preship",
        R.string.lot_not_found to "lot_not_found",
        R.string.lot_cant_be_preshipped to "lot_cant_be_preshipped",
        R.string.unknown_error to "unknown_error"
    )
}