package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.CampaignInfoConverter
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo

abstract class CampaignInfo<T : CommonCampaign>(
    var managerInfo: UserInfo?,
    var agencyInfo: UserInfo?,
    clientInfo: ClientInfo,
    var typedCampaign: T
) : CampaignInfo(clientInfo) {
    val shard: Int get() = clientInfo.shard
    val uid: Long get() = clientInfo.uid

    //val clientId: ClientId? get() = clientInfo.clientId
    val campaignId: Long get() = typedCampaign.id
    val id: Long get() = campaignId

    open fun withTypedCampaign(typedCampaign: T) = apply { this.typedCampaign = typedCampaign }

    fun toCampaignInfo(): CampaignInfo {
        return CampaignInfoConverter.toCampaignInfo(clientInfo, typedCampaign)
    }
}
