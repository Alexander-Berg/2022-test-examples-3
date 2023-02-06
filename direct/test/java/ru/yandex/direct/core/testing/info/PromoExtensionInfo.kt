package ru.yandex.direct.core.testing.info

import ru.yandex.direct.core.entity.promoextension.model.PromoExtension

class PromoExtensionInfo (
    val clientInfo: ClientInfo,
    val promoExtension: PromoExtension
) {
    val shard: Int get() = clientInfo.shard
    val promoExtensionId: Long get() = promoExtension.promoExtensionId!!
}
