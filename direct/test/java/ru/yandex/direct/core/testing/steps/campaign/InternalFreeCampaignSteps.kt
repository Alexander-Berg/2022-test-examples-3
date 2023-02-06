package ru.yandex.direct.core.testing.steps.campaign

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.campaign.InternalFreeCampaignInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.InternalAdProductSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.metrika.client.MetrikaClient
import kotlin.reflect.KClass

@Component
class InternalFreeCampaignSteps(
    dslContextProvider: DslContextProvider,
    campaignModifyRepository: CampaignModifyRepository,
    campaignAddOperationSupportFacade: CampaignAddOperationSupportFacade,
    clientSteps: ClientSteps,
    userSteps: UserSteps,
    metrikaClient: MetrikaClient,
    internalAdProductSteps: InternalAdProductSteps
) : InternalCampaignSteps<InternalFreeCampaign, InternalFreeCampaignInfo>(
    dslContextProvider, campaignModifyRepository,
    campaignAddOperationSupportFacade, clientSteps, userSteps, metrikaClient, internalAdProductSteps) {

    override fun getCampaignInfo(): InternalFreeCampaignInfo {
        return InternalFreeCampaignInfo()
    }

    override fun getCampaignInfoClass(): KClass<InternalFreeCampaignInfo> {
        return InternalFreeCampaignInfo::class
    }

    override fun createRelations(campaignInfo: InternalFreeCampaignInfo) {
        //костыль для обратной совместимости
        if (campaignInfo.campaign == null) {
            campaignInfo.withCampaign(TestCampaigns.activeInternalFreeCampaign(null, null))
        }
    }

}
