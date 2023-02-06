package ru.yandex.direct.core.testing.info.campaign

import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign
import ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns.fullContentPromotionCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign

class ContentPromotionCampaignInfo(
    managerInfo: UserInfo? = null,
    agencyInfo: UserInfo? = null,
    clientInfo: ClientInfo = ClientInfo(),
    typedCampaign: ContentPromotionCampaign = fullContentPromotionCampaign()
) : CampaignInfo<ContentPromotionCampaign>(managerInfo, agencyInfo, clientInfo, typedCampaign) {

    override fun withTypedCampaign(typedCampaign: ContentPromotionCampaign): ContentPromotionCampaignInfo {
        return super.withTypedCampaign(typedCampaign) as ContentPromotionCampaignInfo
    }

    override fun withCampaign(campaign: Campaign): ContentPromotionCampaignInfo {
        return super.withCampaign(campaign) as ContentPromotionCampaignInfo
    }

    override fun withClientInfo(clientInfo: ClientInfo): ContentPromotionCampaignInfo {
        return super.withClientInfo(clientInfo) as ContentPromotionCampaignInfo
    }
}
