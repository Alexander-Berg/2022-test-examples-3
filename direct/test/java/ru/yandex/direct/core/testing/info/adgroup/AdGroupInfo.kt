package ru.yandex.direct.core.testing.info.adgroup

import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.dbutil.model.ClientId

abstract class AdGroupInfo<T : AdGroup> {
    lateinit var campaignInfo: CampaignInfo<*>
    lateinit var adGroup: T

    val shard: Int get() = campaignInfo.shard
    val uid: Long get() = campaignInfo.uid

    val clientId: ClientId? get() = campaignInfo.clientId
    val campaignId: Long get() = campaignInfo.id
    val adGroupId: Long get() = adGroup.id
    val adGroupType: AdGroupType get() = adGroup.type

    val clientInfo: ClientInfo get() = campaignInfo.clientInfo

    fun isCampaignInfoInitialized() = ::campaignInfo.isInitialized
    fun isAdGroupInitialized() = ::adGroup.isInitialized

    open fun withCampaignInfo(campaignInfo: CampaignInfo<*>) = apply { this.campaignInfo = campaignInfo }

    open fun withAdGroup(adGroup: T) = apply { this.adGroup = adGroup }
}
