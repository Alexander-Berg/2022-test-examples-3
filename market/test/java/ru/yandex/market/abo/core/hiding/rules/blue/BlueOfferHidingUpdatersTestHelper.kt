package ru.yandex.market.abo.core.hiding.rules.blue

import java.util.Date
import ru.yandex.market.abo.api.entity.offer.hidden.blue.BlueOfferHidingReason.MANUALLY_HIDDEN
import ru.yandex.market.abo.util.FakeUsers

private const val SUPPLIER_ID = 111L

fun buildSskuRule(ssku: String) = buildBaseRule().apply {
    shopSku = ssku
    supplierId = SUPPLIER_ID
}

fun buildMskuRule(msku: Long) = buildBaseRule().apply {
    marketSku = msku
}

fun buildSupplierMskuRule(msku: Long) = buildBaseRule().apply {
    marketSku = msku
    supplierId = SUPPLIER_ID
}

fun buildModelRule(mId: Long) = buildBaseRule().apply {
    modelId = mId
}

private fun buildBaseRule(): BlueOfferHidingRule = BlueOfferHidingRule().apply {
    hidingReason = MANUALLY_HIDDEN
    comment = "comment"
    createdUserId = FakeUsers.BLUE_OFFER_HIDING_PROCESSOR.id
    creationTime = Date()
    deleted = false
}
