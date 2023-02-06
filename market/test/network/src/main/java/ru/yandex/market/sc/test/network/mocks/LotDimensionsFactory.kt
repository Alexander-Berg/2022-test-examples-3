package ru.yandex.market.sc.test.network.mocks

import ru.yandex.market.sc.core.data.lot.LotDimensions
import ru.yandex.market.sc.core.data.lot.LotDimensionsId

fun testLotDimensions(
    id: LotDimensionsId = LotDimensionsId("id"),
    name: String = "name",
    description: String = "description",
) = LotDimensions(id, name, description)
