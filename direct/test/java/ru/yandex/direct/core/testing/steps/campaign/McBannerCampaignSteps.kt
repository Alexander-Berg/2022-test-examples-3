package ru.yandex.direct.core.testing.steps.campaign

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.campaign.model.McBannerCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.campaign.McBannerCampaignInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.metrika.client.MetrikaClient
import kotlin.reflect.KClass

@Component
class McBannerCampaignSteps(
    dslContextProvider: DslContextProvider,
    campaignModifyRepository: CampaignModifyRepository,
    campaignAddOperationSupportFacade: CampaignAddOperationSupportFacade,
    clientSteps: ClientSteps,
    userSteps: UserSteps,
    metrikaClient: MetrikaClient
) : CampaignSteps<McBannerCampaign, McBannerCampaignInfo>(dslContextProvider, campaignModifyRepository,
    campaignAddOperationSupportFacade, clientSteps, userSteps, metrikaClient) {

    override fun getCampaignInfo(): McBannerCampaignInfo {
        return McBannerCampaignInfo()
    }

    override fun getCampaignInfoClass(): KClass<McBannerCampaignInfo> {
        return McBannerCampaignInfo::class
    }

    override fun createRelations(campaignInfo: McBannerCampaignInfo) {
        //костыль для обратной совместимости
        if (campaignInfo.campaign == null) {
            campaignInfo.withCampaign(TestCampaigns.activeMcBannerCampaign(null, null))
        }
    }

}
