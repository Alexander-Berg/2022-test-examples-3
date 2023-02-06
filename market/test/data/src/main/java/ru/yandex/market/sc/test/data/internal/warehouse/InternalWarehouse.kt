package ru.yandex.market.sc.test.data.internal.warehouse

import ru.yandex.market.sc.core.utils.data.ExternalId

data class InternalWarehouse(
    val id: Long,
    val incorporation: String? = null,
    val partnerId: String? = null,
    val yandexId: ExternalId? = null,
)
