package ru.yandex.direct.core.testing.data

import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import java.math.BigDecimal
import java.time.LocalDateTime

val DEFAULT_PRICE_SEARCH = BigDecimal("12.30")
val DEFAULT_PRICE_CONTEXT = BigDecimal("11.20")


val defaultOfferRetargeting: OfferRetargeting
    get() = OfferRetargeting()
        .withLastChangeTime(LocalDateTime.now())
        .withIsDeleted(false)
        .withIsSuspended(false)
        .withAutobudgetPriority(null)
        .withStatusBsSynced(StatusBsSynced.NO)
        .withPrice(DEFAULT_PRICE_SEARCH)
        .withPriceContext(DEFAULT_PRICE_CONTEXT)
