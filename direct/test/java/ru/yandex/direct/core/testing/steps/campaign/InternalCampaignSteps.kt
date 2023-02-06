package ru.yandex.direct.core.testing.steps.campaign

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.InternalAdProductSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.metrika.client.MetrikaClient

abstract class InternalCampaignSteps<C : CommonCampaign, T : CampaignInfo<C>>(
    dslContextProvider: DslContextProvider,
    campaignModifyRepository: CampaignModifyRepository,
    campaignAddOperationSupportFacade: CampaignAddOperationSupportFacade,
    clientSteps: ClientSteps,
    userSteps: UserSteps,
    metrikaClient: MetrikaClient,
    private val internalAdProductSteps: InternalAdProductSteps
) : CampaignSteps<C, T>(
    dslContextProvider, campaignModifyRepository,
    campaignAddOperationSupportFacade, clientSteps, userSteps, metrikaClient) {

    override fun processClient(clientInfo: ClientInfo, typedCampaign: C) {
        internalAdProductSteps.createDefaultInternalAdProduct(clientInfo, emptySet(), "")
    }
}
