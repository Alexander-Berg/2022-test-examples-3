package ru.yandex.direct.core.testing.info

import ru.yandex.direct.core.entity.bidmodifier.BidModifier
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo
import ru.yandex.direct.dbutil.model.ClientId

class NewAdGroupBidModifierInfo(
    var adGroupInfo: AdGroupInfo<*>,
    var bidModifiers: List<BidModifier>?,
) {
    val shard: Int get() = adGroupInfo.shard
    val campaignId: Long get() = adGroupInfo.campaignId
    val adGroupId: Long get() = adGroupInfo.adGroupId

    val uid: Long = adGroupInfo.uid
    val clientId: ClientId? = adGroupInfo.clientId

    var bidModifier: BidModifier?
        get() = bidModifiers?.get(0)
        set(bidModifier) {
            bidModifiers = listOf(bidModifier!!)
        }
    val bidModifierId: Long? get() = bidModifier?.id
}
