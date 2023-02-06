package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.place.PlaceInfo
import ru.yandex.market.sc.core.utils.data.ExternalId

object PlaceInfoTestFactory {
    fun forOrder(orderExternalId: ExternalId): PlaceInfoMapBuilder {
        return PlaceInfoMapBuilder(orderExternalId)
    }

    class PlaceInfoMapBuilder(val orderExternalId: ExternalId) {
        private val placeInfoMap: MutableMap<ExternalId, PlaceInfo> = mutableMapOf()

        fun withPlace(inCell: Boolean, placeExternalId: ExternalId? = null): PlaceInfoMapBuilder {
            val placeExternalIdNonNull: ExternalId =
                placeExternalId ?: getNewPlaceExternalId(orderExternalId)

            placeInfoMap[placeExternalIdNonNull] = PlaceInfo(
                inCell = inCell,
                orderExternalId = orderExternalId,
                placeExternalId = placeExternalIdNonNull,
            )

            return this
        }

        private fun getNewPlaceExternalId(orderExternalId: ExternalId): ExternalId {
            var placeInd = 0
            while (true) {
                val placeExternalId = IdManager.getIndexedExternalId(orderExternalId, placeInd++)
                if (!placeInfoMap.containsKey(placeExternalId)) {
                    return placeExternalId
                }
            }
        }

        fun buildPlaceInfoMap(): Map<ExternalId, PlaceInfo> {
            return placeInfoMap.toMap()
        }
    }

    // если нужно указать конкретный placeExternalId,
    // проще создать PlaceInfo через конструктор, чем использовать TestFactory
    fun getPlaceInfo(
        orderExternalId: ExternalId,
        inCell: Boolean,
    ): PlaceInfo {
        val placeExternalId = IdManager.getIndexedExternalId(orderExternalId)

        return PlaceInfo(
            inCell = inCell,
            orderExternalId = orderExternalId,
            placeExternalId = placeExternalId,
        )
    }
}
