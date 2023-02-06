package ru.yandex.market.sc.test.data.manual.courier

import ru.yandex.market.sc.core.utils.data.ExternalId

data class ManualOutboundCourier(
    val courierExternalId: ExternalId? = null,
    val courierName: String? = null,
    val courierLegalName: String? = null,
)
