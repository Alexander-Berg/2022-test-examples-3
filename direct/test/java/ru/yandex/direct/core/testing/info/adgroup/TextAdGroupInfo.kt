package ru.yandex.direct.core.testing.info.adgroup

import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo

class TextAdGroupInfo : AdGroupInfo<TextAdGroup>() {

    val typedCampaignInfo get() = campaignInfo as TextCampaignInfo

    override fun withCampaignInfo(campaignInfo: CampaignInfo<*>) = apply { this.campaignInfo = campaignInfo }

    override fun withAdGroup(adGroup: TextAdGroup) = apply { this.adGroup = adGroup }
}
