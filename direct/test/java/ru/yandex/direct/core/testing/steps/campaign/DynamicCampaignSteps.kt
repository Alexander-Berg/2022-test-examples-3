package ru.yandex.direct.core.testing.steps.campaign

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.campaign.DynamicCampaignInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.metrika.client.MetrikaClient
import kotlin.reflect.KClass

@Component
class DynamicCampaignSteps(
    dslContextProvider: DslContextProvider,
    campaignModifyRepository: CampaignModifyRepository,
    campaignAddOperationSupportFacade: CampaignAddOperationSupportFacade,
    clientSteps: ClientSteps,
    userSteps: UserSteps,
    metrikaClient: MetrikaClient
) : CampaignSteps<DynamicCampaign, DynamicCampaignInfo>(dslContextProvider, campaignModifyRepository,
    campaignAddOperationSupportFacade, clientSteps, userSteps, metrikaClient ) {

    override fun getCampaignInfo(): DynamicCampaignInfo {
        return DynamicCampaignInfo()
    }

    override fun getCampaignInfoClass(): KClass<DynamicCampaignInfo> {
        return DynamicCampaignInfo::class
    }

    override fun createRelations(campaignInfo: DynamicCampaignInfo) {
        //костыль для обратной совместимости
        if (campaignInfo.campaign == null) {
            campaignInfo.withCampaign(TestCampaigns.activeDynamicCampaign(null, null))
        }
    }

}
