package ru.yandex.direct.core.testing.steps.campaign

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.campaign.model.MobileContentCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestMobileContents.defaultMobileContentWithUrl
import ru.yandex.direct.core.testing.info.campaign.MobileContentCampaignInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.MobileAppSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.metrika.client.MetrikaClient
import kotlin.reflect.KClass

@Component
class MobileContentCampaignSteps(
    dslContextProvider: DslContextProvider,
    campaignModifyRepository: CampaignModifyRepository,
    campaignAddOperationSupportFacade: CampaignAddOperationSupportFacade,
    clientSteps: ClientSteps,
    userSteps: UserSteps,
    private val mobileAppSteps: MobileAppSteps,
    metrikaClient: MetrikaClient
) : CampaignSteps<MobileContentCampaign, MobileContentCampaignInfo>(dslContextProvider, campaignModifyRepository,
    campaignAddOperationSupportFacade, clientSteps, userSteps, metrikaClient) {

    override fun getCampaignInfo(): MobileContentCampaignInfo {
        return MobileContentCampaignInfo()
    }

    override fun getCampaignInfoClass(): KClass<MobileContentCampaignInfo> {
        return MobileContentCampaignInfo::class
    }

    override fun createRelations(campaignInfo: MobileContentCampaignInfo) {
        val mobileAppInfo = campaignInfo.mobileAppInfo
        mobileAppInfo.clientInfo = campaignInfo.clientInfo

        //костыль для обратной совместимости
        if (campaignInfo.campaign == null) {
            campaignInfo.withCampaign(TestCampaigns.activeMobileContentCampaign(null, null))
        }

        if (mobileAppInfo.mobileContentId == null) {
            if (mobileAppInfo.mobileContentInfo.mobileContent == null) {
                mobileAppInfo.mobileContentInfo.mobileContent = defaultMobileContentWithUrl()
            }
            mobileAppSteps.createMobileApp(mobileAppInfo)
        }

        if (campaignInfo.typedCampaign.mobileAppId == null) {
            campaignInfo.typedCampaign.mobileAppId = mobileAppInfo.mobileAppId
        }
    }
}
