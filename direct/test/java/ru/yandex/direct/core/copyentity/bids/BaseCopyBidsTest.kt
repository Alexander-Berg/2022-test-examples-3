package ru.yandex.direct.core.copyentity.bids

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.spring.AbstractSpringTest
import ru.yandex.direct.core.copyentity.CopyOperationFactory
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.StrategyData
import ru.yandex.direct.core.entity.campaign.model.StrategyName
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestCpmYndxFrontPageCampaigns
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.CpmBannerCampaignInfo
import ru.yandex.direct.core.testing.info.campaign.CpmYndxFrontpageCampaignInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.currencies.CurrencyRub
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.rbac.RbacRole
import java.math.BigDecimal
import java.time.LocalDateTime

abstract class BaseCopyBidsTest : AbstractSpringTest() {
    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var copyOperationFactory: CopyOperationFactory

    @Autowired
    protected lateinit var testCpmYndxFrontpageRepository: TestCpmYndxFrontpageRepository

    protected lateinit var client: ClientInfo

    protected lateinit var targetClient: ClientInfo

    protected lateinit var superClient: ClientInfo

    protected lateinit var sourceTextAbSearchCampaignInfo: TextCampaignInfo
    protected lateinit var sourceTextNotAbSearchCampaignInfo: TextCampaignInfo
    protected lateinit var sourceTextAbContextCampaignInfo: TextCampaignInfo
    protected lateinit var sourceTextNotAbContextCampaignInfo: TextCampaignInfo
    protected lateinit var destinationTextAbSearchCampaignInfo: TextCampaignInfo
    protected lateinit var destinationTextNotAbSearchCampaignInfo: TextCampaignInfo
    protected lateinit var destinationTextNotAbContextCampaignInfo: TextCampaignInfo

    protected lateinit var sourceCpmAbSearchCampaignInfo: CpmBannerCampaignInfo
    protected lateinit var destinationCpmNotAbSearchCampaignInfo: CpmBannerCampaignInfo
    protected lateinit var destinationCpmNotAbContextCampaignInfo: CpmBannerCampaignInfo

    protected lateinit var sourceYandexFrontpageAbSearchCampaignInfo: CpmYndxFrontpageCampaignInfo
    protected lateinit var destinationYandexFrontpageNotAbSearchCampaignInfo: CpmYndxFrontpageCampaignInfo

    protected fun initClients() {
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        client = steps.clientSteps().createDefaultClient()
        targetClient = steps.clientSteps().createDefaultClient()
    }

    protected fun initTextCampaigns() {
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        client = steps.clientSteps().createDefaultClient()
        targetClient = steps.clientSteps().createDefaultClient()

        sourceTextAbSearchCampaignInfo = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withStrategy(autobudgetSearchStrategy())
        )

        sourceTextNotAbSearchCampaignInfo = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withStrategy(manualSearchStrategy())
        )

        sourceTextAbContextCampaignInfo = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withStrategy(autobudgetContextStrategy())
        )

        sourceTextNotAbContextCampaignInfo = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withStrategy(manualContextStrategy())
        )

        destinationTextAbSearchCampaignInfo = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withStrategy(autobudgetSearchStrategy())
        )

        destinationTextNotAbSearchCampaignInfo = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withStrategy(manualSearchStrategy())
        )

        destinationTextNotAbContextCampaignInfo = steps.textCampaignSteps().createCampaign(
            client,
            TestTextCampaigns
                .fullTextCampaign()
                .withStrategy(manualContextStrategy())
        )
    }

    protected fun initCmpCampaigns() {
        sourceCpmAbSearchCampaignInfo = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns
                .fullCpmBannerCampaign()
                .withStrategy(autobudgetSearchStrategy())
        )

        destinationCpmNotAbSearchCampaignInfo = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns
                .fullCpmBannerCampaign()
                .withStrategy(manualCpmSearchStrategy())
        )

        destinationCpmNotAbContextCampaignInfo = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns
                .fullCpmBannerCampaign()
                .withStrategy(manualCpmContextStrategy())
        )
    }

    protected fun initCmpYandexFrontpageCampaigns() {
        steps.featureSteps().addClientFeature(client.clientId, FeatureName.CPM_YNDX_FRONTPAGE_PROFILE, true)

        testCpmYndxFrontpageRepository.fillMinBidsTestValues()
        sourceYandexFrontpageAbSearchCampaignInfo = steps.cpmYndxFrontPageSteps().createCampaign(
            client,
            TestCpmYndxFrontPageCampaigns
                .fullCpmYndxFrontpageCampaign()
                .withStrategy(autobudgetSearchStrategy())
        )
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
            sourceYandexFrontpageAbSearchCampaignInfo.getShard(),
            sourceYandexFrontpageAbSearchCampaignInfo.getCampaignId(),
            listOf(FrontpageCampaignShowType.FRONTPAGE)
        )

        destinationYandexFrontpageNotAbSearchCampaignInfo = steps.cpmYndxFrontPageSteps().createCampaign(
            client,
            TestCpmYndxFrontPageCampaigns
                .fullCpmYndxFrontpageCampaign()
                .withStrategy(manualCpmSearchStrategy())
        )
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
            destinationYandexFrontpageNotAbSearchCampaignInfo.getShard(),
            destinationYandexFrontpageNotAbSearchCampaignInfo.getCampaignId(),
            listOf(FrontpageCampaignShowType.FRONTPAGE)
        )
    }

    protected fun autobudgetSearchStrategy(): DbStrategy = DbStrategy()
        .withStrategyName(StrategyName.AUTOBUDGET)
        .withPlatform(CampaignsPlatform.SEARCH)
        .withAutobudget(CampaignsAutobudget.YES)
        .withStrategyData(
            StrategyData()
                .withName(CampaignsStrategyName.autobudget.literal)
                .withLastBidderRestartTime(LocalDateTime.now())
                .withSum(CurrencyRub.getInstance().defaultAutobudget)
                .withBid(BigDecimal.TEN)
        ) as DbStrategy

    protected fun autobudgetContextStrategy(): DbStrategy = autobudgetSearchStrategy()
        .withPlatform(CampaignsPlatform.CONTEXT) as DbStrategy

    protected fun manualSearchStrategy(): DbStrategy = DbStrategy()
        .withStrategyName(StrategyName.DEFAULT_)
        .withAutobudget(CampaignsAutobudget.NO)
        .withPlatform(CampaignsPlatform.SEARCH)
        .withStrategyData(
            StrategyData()
                .withName(CampaignsStrategyName.default_.literal)
                .withVersion(1L)
        ) as DbStrategy

    protected fun manualContextStrategy(): DbStrategy = manualSearchStrategy()
        .withPlatform(CampaignsPlatform.CONTEXT) as DbStrategy

    protected fun manualCpmSearchStrategy(): DbStrategy = DbStrategy()
        .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
        .withAutobudget(CampaignsAutobudget.NO)
        .withStrategyName(StrategyName.CPM_DEFAULT)
        .withPlatform(CampaignsPlatform.SEARCH)
        .withStrategyData(
            StrategyData()
                .withVersion(1L)
                .withName(CampaignsStrategyName.cpm_default.literal)
        ) as DbStrategy

    protected fun manualCpmContextStrategy(): DbStrategy = manualCpmSearchStrategy()
        .withPlatform(CampaignsPlatform.CONTEXT) as DbStrategy

}
