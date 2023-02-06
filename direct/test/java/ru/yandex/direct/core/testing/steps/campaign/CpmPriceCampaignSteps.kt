package ru.yandex.direct.core.testing.steps.campaign

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestPricePackages.allowedPricePackageClient
import ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage
import ru.yandex.direct.core.testing.data.campaign.TestCpmPriceCampaigns
import ru.yandex.direct.core.testing.info.campaign.CpmPriceCampaignInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.PricePackageSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.metrika.client.MetrikaClient
import kotlin.reflect.KClass

@Component
class CpmPriceCampaignSteps(
    dslContextProvider: DslContextProvider,
    campaignModifyRepository: CampaignModifyRepository,
    campaignAddOperationSupportFacade: CampaignAddOperationSupportFacade,
    clientSteps: ClientSteps,
    userSteps: UserSteps,
    private val pricePackageSteps: PricePackageSteps,
    metrikaClient: MetrikaClient
) : CampaignSteps<CpmPriceCampaign, CpmPriceCampaignInfo>(dslContextProvider, campaignModifyRepository,
    campaignAddOperationSupportFacade, clientSteps, userSteps, metrikaClient) {

    override fun getCampaignInfo(): CpmPriceCampaignInfo {
        return CpmPriceCampaignInfo()
    }

    override fun getCampaignInfoClass(): KClass<CpmPriceCampaignInfo> {
        return CpmPriceCampaignInfo::class
    }

    override fun createRelations(campaignInfo: CpmPriceCampaignInfo) {
        val pricePackageInfo = campaignInfo.pricePackageInfo

        //костыль для обратной совместимости
        if (campaignInfo.campaign == null) {
            campaignInfo.withCampaign(TestCampaigns.activeCpmPriceCampaign(null, null))
        }

        //создаем заапрувленный пакет, т.к. по умолчанию создается активная кампания
        if (pricePackageInfo.pricePackage == null) {
            pricePackageInfo.pricePackage = approvedPricePackage()
        }

        if (pricePackageInfo.pricePackage?.id == null) {
            pricePackageSteps.createPricePackage(pricePackageInfo)
        }

        if (pricePackageInfo.pricePackage.clients == null) {
            pricePackageInfo.pricePackage.clients = listOf(allowedPricePackageClient(campaignInfo.clientInfo))
        }

        if (campaignInfo.typedCampaign.pricePackageId == null) {
            TestCpmPriceCampaigns.enrichCampaignWithPricePackage(campaignInfo.typedCampaign, pricePackageInfo.pricePackage)
        }
    }
}
