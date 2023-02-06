package ru.yandex.direct.core.testing.steps.adgroup

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign
import ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo
import ru.yandex.direct.core.testing.steps.campaign.CampaignStepsFactory
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import kotlin.reflect.KClass

@Component
class ContentPromotionAdGroupSteps(
    campaignStepsFactory: CampaignStepsFactory,
    dslContextProvider: DslContextProvider,
    adGroupRepository: AdGroupRepository)
    : AdGroupSteps<ContentPromotionAdGroup, ContentPromotionAdGroupInfo>(campaignStepsFactory,
    dslContextProvider, adGroupRepository, ALLOWED_CAMPAIGN_CLASSES) {

    companion object {
        private val ALLOWED_CAMPAIGN_CLASSES = setOf(ContentPromotionCampaign::class)
    }

    fun createDefaultAdGroup(contentPromotionType: ContentPromotionAdgroupType): ContentPromotionAdGroupInfo {
        return createAdGroup(ContentPromotionAdGroupInfo()
            .withAdGroup(fullContentPromotionAdGroup(contentPromotionType)))
    }

    fun createDefaultAdGroup(clientInfo: ClientInfo): ContentPromotionAdGroupInfo {
        return createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO)
    }

    fun createDefaultAdGroup(clientInfo: ClientInfo,
                             contentPromotionType: ContentPromotionAdgroupType): ContentPromotionAdGroupInfo {
        return createAdGroup(ContentPromotionAdGroupInfo()
            .withCampaignInfo(ContentPromotionCampaignInfo(clientInfo = clientInfo))
            .withAdGroup(fullContentPromotionAdGroup(contentPromotionType)))
    }

    fun createDefaultAdGroup(campaignInfo: ContentPromotionCampaignInfo,
                             contentPromotionType: ContentPromotionAdgroupType): ContentPromotionAdGroupInfo {
        return createAdGroup(ContentPromotionAdGroupInfo()
            .withCampaignInfo(campaignInfo)
            .withAdGroup(fullContentPromotionAdGroup(contentPromotionType)))
    }

    fun createAdGroup(clientInfo: ClientInfo, adgroup: ContentPromotionAdGroup): ContentPromotionAdGroupInfo {
        return createAdGroup(ContentPromotionAdGroupInfo()
            .withAdGroup(adgroup)
            .withCampaignInfo(ContentPromotionCampaignInfo(clientInfo = clientInfo)))
    }

    override fun initializeCampaignInfo(adGroupInfo: ContentPromotionAdGroupInfo) {
        if (!adGroupInfo.isCampaignInfoInitialized()) {
            adGroupInfo.campaignInfo = ContentPromotionCampaignInfo()
        }
        if (!adGroupInfo.isAdGroupInitialized()) {
            adGroupInfo.adGroup = fullContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO)
        }
    }

    override fun getAdGroupInfoClass(): KClass<ContentPromotionAdGroupInfo> {
        return ContentPromotionAdGroupInfo::class
    }

    override fun getAdGroupInfo(): ContentPromotionAdGroupInfo {
        return ContentPromotionAdGroupInfo()
    }
}
