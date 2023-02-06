package ru.yandex.market.sc.test.data.manual.courier

import ru.yandex.market.sc.core.data.courier.Courier

object CourierMapper {
    fun mapToRequest(courier: Courier): ManualCourier {
        return ManualCourier(
            id = requireNotNull(courier.id),
            name = courier.name,
            null,
            null,
            null,
            null
        )
    }
}
