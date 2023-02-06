package ru.yandex.direct.core.copyentity.bids

import junitparams.JUnitParamsRunner
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.assertj.core.util.BigDecimalComparator
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.copyentity.CopyConfig
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.adGroupCopyConfig
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.campaignsBetweenClientsCopyConfig
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.copyValidEntity
import ru.yandex.direct.core.copyentity.CopyOperation
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.CpmYndxFrontpageMinBid
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType
import ru.yandex.direct.core.entity.retargeting.model.ConditionType
import ru.yandex.direct.core.entity.retargeting.model.Retargeting
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.entity.showcondition.Constants.DEFAULT_AUTOBUDGET_PRIORITY
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestClients.defaultClient
import ru.yandex.direct.core.testing.data.TestRetargetingConditions
import ru.yandex.direct.core.testing.data.TestRetargetings
import ru.yandex.direct.core.testing.data.campaign.TestCpmYndxFrontPageCampaigns
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository.DEFAULT_BIDS_FOR_TEST
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.Currency
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.currencies.CurrencyChf
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.regions.Region
import java.math.BigDecimal

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyRetargetingsTest : BaseCopyBidsTest() {
    private val RETARGETINGS_COMPARE_STRATEGY = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("id", "adGroupId", "campaignId", "retargetingConditionId", "lastChangeTime")
        .build()

    private var interestsRetConditionId: Long = 0
    private var defaultRetConditionId: Long = 0

    @Before
    fun before() {
        initClients()
        initTextCampaigns()
        initCmpCampaigns()
        initCmpYandexFrontpageCampaigns()

        val interestsRetCondition: RetargetingCondition = TestRetargetingConditions
            .defaultRetCondition(client.clientId)
            .withType(ConditionType.interests) as RetargetingCondition

        interestsRetConditionId =
            steps.retConditionSteps().createRetCondition(interestsRetCondition, client).retConditionId

        val defaultRetCondition: RetargetingCondition = TestRetargetingConditions
            .defaultRetCondition(client.clientId) as RetargetingCondition

        defaultRetConditionId = steps.retConditionSteps().createRetCondition(defaultRetCondition, client).retConditionId
    }

    @Test
    fun copyNullAbPriorityRetargetingSameTextContextNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextNotAbContextCampaignInfo)
        val expectedRetargeting: Retargeting =
            createExpectedRetargeting(sourceTextAdGroupInfo, currencyTo.minPrice, null)
        val copiedRetargeting: Retargeting = copyRetargetingSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            sourceTextAdGroupInfo.campaignId
        )

        expectedRetargeting.autobudgetPriority = DEFAULT_AUTOBUDGET_PRIORITY

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    @Test
    fun copyNullAbPriorityRetargetingTextContextNotAbToAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbContextCampaignInfo)
        val expectedRetargeting = createExpectedRetargeting(sourceTextAdGroupInfo, null, null)
        val copiedRetargeting: Retargeting = copyRetargetingSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            destinationTextNotAbContextCampaignInfo.campaignId
        )

        expectedRetargeting.priceContext = currencyTo.minPrice
        expectedRetargeting.autobudgetPriority = DEFAULT_AUTOBUDGET_PRIORITY

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    @Test
    fun copyNullAbPriorityRetargetingTextContextNotAbCampaignBetweenClients() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextNotAbContextCampaignInfo)
        val expectedRetargeting: Retargeting =
            createExpectedRetargeting(sourceTextAdGroupInfo, currencyTo.minPrice, null)
        val copiedRetargeting: Retargeting = copyRetargetingBetweenClients(sourceTextAdGroupInfo.campaignId)

        expectedRetargeting.autobudgetPriority = DEFAULT_AUTOBUDGET_PRIORITY

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    @Test
    fun copyNullPriceContextRetargetingSameTextSearchAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRetargeting: Retargeting =
            createExpectedRetargeting(sourceTextAdGroupInfo, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedRetargeting: Retargeting = copyRetargetingSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            sourceTextAdGroupInfo.campaignId
        )

        expectedRetargeting.priceContext = currencyTo.minPrice

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    @Test
    fun copyNullPriceContextRetargetingTextSearchAbToTextContextNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRetargeting: Retargeting =
            createExpectedRetargeting(sourceTextAdGroupInfo, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedRetargeting: Retargeting = copyRetargetingSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            destinationTextNotAbContextCampaignInfo.campaignId
        )

        expectedRetargeting.priceContext = currencyTo.minPrice

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    @Test
    fun copyNullPriceContextRetargetingCpmSearchAbToContextNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceCpmAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveCpmBannerAdGroup(sourceCpmAbSearchCampaignInfo)
        val expectedRetargeting: Retargeting =
            createExpectedRetargeting(sourceCpmAdGroupInfo, null, DEFAULT_AUTOBUDGET_PRIORITY, interestsRetConditionId)
        val copiedRetargeting: Retargeting = copyRetargetingSameClient(
            sourceCpmAdGroupInfo,
            sourceCpmAdGroupInfo.campaignId,
            destinationCpmNotAbContextCampaignInfo.campaignId
        )

        expectedRetargeting.priceContext = currencyTo.minCpmPrice

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    @Test
    fun copyNullPriceContextRetargetingCpmYandexFrontpageAbToNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceCpmYandexFrontpageAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(sourceYandexFrontpageAbSearchCampaignInfo)
        steps.adGroupSteps()
            .setAdGroupProperty(sourceCpmYandexFrontpageAdGroupInfo, AdGroup.GEO, listOf(Region.RUSSIA_REGION_ID))
        val expectedRetargeting: Retargeting =
            createExpectedRetargeting(
                sourceCpmYandexFrontpageAdGroupInfo,
                null,
                DEFAULT_AUTOBUDGET_PRIORITY,
                interestsRetConditionId
            )
        val copiedRetargeting: Retargeting = copyRetargetingSameClient(
            sourceCpmYandexFrontpageAdGroupInfo,
            sourceCpmYandexFrontpageAdGroupInfo.campaignId,
            destinationYandexFrontpageNotAbSearchCampaignInfo.campaignId
        )

        expectedRetargeting.priceContext = currencyTo.minCpmPrice

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    @Test
    fun copyNullPriceContextRetargetingCpmYandexFrontpageWithChfCurrencyTest() {
        val (expectedRetargeting: Retargeting, copiedRetargeting: Retargeting) =
            createAndCopyYndxFrontpageRetargetingsWithChfCurrency(null)

        val minBid: CpmYndxFrontpageMinBid = getCpmYndxFrontpageMinBidInChfCurrency()

        expectedRetargeting.priceContext = minBid.minBid

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    @Test
    fun copyLessThanMinPriceContextRetargetingCpmYandexFrontpageWithChfCurrencyTest() {
        val minRegionalBid: BigDecimal = getCpmYndxFrontpageMinBidInChfCurrency().minBid
        val minCurrencyBid: BigDecimal = CurrencyChf.getInstance().minCpmPrice
        val expectedPriceContext: BigDecimal = minCurrencyBid.add(minRegionalBid).divide(BigDecimal(2))

        val (expectedRetargeting: Retargeting, copiedRetargeting: Retargeting) =
            createAndCopyYndxFrontpageRetargetingsWithChfCurrency(expectedPriceContext)

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    @Test
    fun copyNullPriceContextRetargetingTextSearchAbCampaignBetweenClients() {
        val currencyTo: Currency = Currencies.getCurrency(targetClient.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRetargeting: Retargeting =
            createExpectedRetargeting(sourceTextAdGroupInfo, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedRetargeting: Retargeting = copyRetargetingBetweenClients(sourceTextAdGroupInfo.campaignId)

        expectedRetargeting.priceContext = currencyTo.minPrice

        assertRetargetings(expectedRetargeting, copiedRetargeting)
    }

    private fun createAndCopyYndxFrontpageRetargetingsWithChfCurrency(
        priceContext: BigDecimal?,
    ): Pair<Retargeting, Retargeting> {
        val clientChf: ClientInfo = steps.clientSteps().createClient(defaultClient().withWorkCurrency(CurrencyCode.CHF))

        steps.featureSteps().addClientFeature(clientChf.clientId, FeatureName.CPM_YNDX_FRONTPAGE_PROFILE, true)

        val sourceYandexFrontpageChfCampaign = steps.cpmYndxFrontPageSteps().createCampaign(
            clientChf,
            TestCpmYndxFrontPageCampaigns
                .fullCpmYndxFrontpageCampaign()
                .withStrategy(autobudgetSearchStrategy())
        )

        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
            sourceYandexFrontpageChfCampaign.getShard(),
            sourceYandexFrontpageChfCampaign.getCampaignId(),
            listOf(FrontpageCampaignShowType.FRONTPAGE)
        )

        val destinationYandexFrontpageChfCampaign = steps.cpmYndxFrontPageSteps().createCampaign(
            clientChf,
            TestCpmYndxFrontPageCampaigns
                .fullCpmYndxFrontpageCampaign()
                .withStrategy(manualCpmContextStrategy())
        )

        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
            destinationYandexFrontpageChfCampaign.getShard(),
            destinationYandexFrontpageChfCampaign.getCampaignId(),
            listOf(FrontpageCampaignShowType.FRONTPAGE)
        )

        val sourceCpmYandexFrontpageAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(sourceYandexFrontpageChfCampaign)

        steps.adGroupSteps()
            .setAdGroupProperty(sourceCpmYandexFrontpageAdGroupInfo, AdGroup.GEO, listOf(Region.RUSSIA_REGION_ID))

        val retargetingCondition: RetargetingCondition = TestRetargetingConditions
            .defaultRetCondition(clientChf.clientId)
            .withType(ConditionType.interests) as RetargetingCondition

        val retConditionChfId: Long =
            steps.retConditionSteps().createRetCondition(retargetingCondition, clientChf).retConditionId

        val expectedRetargeting: Retargeting =
            createExpectedRetargeting(
                sourceCpmYandexFrontpageAdGroupInfo,
                priceContext,
                DEFAULT_AUTOBUDGET_PRIORITY,
                retConditionChfId
            )

        val copiedRetargeting: Retargeting = copyRetargetingSameClient(
            sourceCpmYandexFrontpageAdGroupInfo,
            sourceCpmYandexFrontpageAdGroupInfo.campaignId,
            destinationYandexFrontpageChfCampaign.campaignId,
            clientChf
        )

        return Pair(expectedRetargeting, copiedRetargeting)
    }

    private fun getCpmYndxFrontpageMinBidInChfCurrency(): CpmYndxFrontpageMinBid =
        DEFAULT_BIDS_FOR_TEST.first {
            it.frontpageCampaignShowType == FrontpageCampaignShowType.FRONTPAGE &&
                it.regionId == Region.RUSSIA_REGION_ID
        }

    private fun assertRetargetings(expectedRetargeting: Retargeting, actualRetargeting: Retargeting) {
        assertThat(actualRetargeting)
            .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
            .usingRecursiveComparison(RETARGETINGS_COMPARE_STRATEGY)
            .isEqualTo(expectedRetargeting)
    }

    private fun copyRetargetingSameClient(
        sourceAdGroupInfo: AdGroupInfo,
        sourceCampaignId: Long,
        destinationCampaignId: Long,
        clientInfo: ClientInfo = client
    ): Retargeting {
        val copyConfig: CopyConfig<*, *> =
            adGroupCopyConfig(
                clientInfo,
                sourceAdGroupInfo.adGroupId,
                sourceCampaignId,
                destinationCampaignId,
                clientInfo.uid
            )
        return getCopiedRetargeting(copyConfig, clientInfo)
    }

    private fun copyRetargetingBetweenClients(sourceCampaignId: Long): Retargeting {
        val copyConfig: CopyConfig<*, *> =
            campaignsBetweenClientsCopyConfig(client, targetClient, sourceCampaignId, superClient.uid)
        return getCopiedRetargeting(copyConfig, targetClient)
    }

    private fun getCopiedRetargeting(copyConfig: CopyConfig<*, *>, targetClient: ClientInfo): Retargeting {
        val copyOperation: CopyOperation<*, *> = copyOperationFactory.build(copyConfig)
        val copiedAdGroupId: Long = copyValidEntity(AdGroup::class.java, copyOperation, true)[0]
        val copiedRetargetings: List<Retargeting> = steps.retargetingSteps()
            .getRetargetingsByAdGroupId(targetClient.shard, copiedAdGroupId)
        assertThat(copiedRetargetings.size).describedAs("Copied retargetings count").isEqualTo(1)
        return copiedRetargetings[0]
    }

    private fun createExpectedRetargeting(
        adGroupInfo: AdGroupInfo,
        priceContext: BigDecimal?,
        autobudgetPriority: Int?,
        retargetingConditionId: Long = defaultRetConditionId
    ): Retargeting {
        val expectedRetargeting: Retargeting =
            TestRetargetings.defaultRetargeting(adGroupInfo.campaignId, adGroupInfo.adGroupId, retargetingConditionId)
                .withPriceContext(priceContext)
                .withAutobudgetPriority(autobudgetPriority)
                .withStatusBsSynced(StatusBsSynced.NO)

        steps.retargetingSteps().addRetargeting(adGroupInfo, expectedRetargeting)

        return expectedRetargeting
    }
}
