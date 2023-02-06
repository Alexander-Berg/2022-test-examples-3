package ru.yandex.direct.core.testing.info.adgroup

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo

class ContentPromotionAdGroupInfo : AdGroupInfo<ContentPromotionAdGroup>() {

    val typedCampaignInfo: ContentPromotionCampaignInfo get() = campaignInfo as ContentPromotionCampaignInfo

    override fun withCampaignInfo(campaignInfo: CampaignInfo<*>) = apply { this.campaignInfo = campaignInfo }

    override fun withAdGroup(adGroup: ContentPromotionAdGroup) = apply { this.adGroup = adGroup }
}
