package ru.yandex.market.sc.test.network.utils

import ru.yandex.market.sc.core.data.inbound.InboundCharacteristics

object TestFactory {

    fun createPalletCharacteristics(height: Int = 100, weight: Int = 100) =
        InboundCharacteristics.Pallet(
            height = height,
            weight = weight
        )

    fun createBoxCharacteristics(
        height: Int = 100,
        weight: Int = 100,
        depth: Int = 100,
        width: Int = 100
    ) =
        InboundCharacteristics.Box(
            height = height,
            weight = weight,
            length = depth,
            width = width
        )
}