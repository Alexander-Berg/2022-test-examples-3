package ru.yandex.market.sc.test.network.data.internal.courier

import ru.yandex.market.sc.test.data.internal.courier.InternalCourier

data class ClientReturnRequest(
    val barcode: String,
    val returnDate: String,
    val courier: InternalCourier,
    val sortingCenterId: Long,
    val token: String,
)
