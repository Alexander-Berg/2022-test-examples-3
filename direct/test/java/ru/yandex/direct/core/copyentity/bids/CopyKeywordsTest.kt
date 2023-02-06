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
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.keyword.model.Keyword
import ru.yandex.direct.core.entity.keyword.model.Place
import ru.yandex.direct.core.entity.showcondition.Constants.DEFAULT_AUTOBUDGET_PRIORITY
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestKeywords
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.Currency
import java.math.BigDecimal

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyKeywordsTest : BaseCopyBidsTest() {
    private val KEYWORDS_COMPARE_STRATEGY = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("id", "adGroupId", "campaignId", "showsForecast", "modificationTime")
        .build()

    @Before
    fun before() {
        initClients()
        initTextCampaigns()
        initCmpCampaigns()
    }

    @Test
    fun copyNullAbPriorityKeywordSameTextSearchNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextNotAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceTextAdGroupInfo, currencyTo.minPrice, currencyTo.minPrice, null)
        val copiedKeyword: Keyword = copyKeywordSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            sourceTextAdGroupInfo.campaignId
        )

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullAbPriorityKeywordTextSearchNotAbToAbCampaign() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedKeyword = createExpectedKeyword(sourceTextAdGroupInfo, null, null, null)
        val copiedKeyword: Keyword = copyKeywordSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            destinationTextAbSearchCampaignInfo.campaignId
        )

        expectedKeyword.autobudgetPriority = DEFAULT_AUTOBUDGET_PRIORITY

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullAbPriorityKeywordTextSearchNotAbCampaignBetweenClients() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextNotAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceTextAdGroupInfo, currencyTo.minPrice, currencyTo.minPrice, null)
        val copiedKeyword: Keyword = copyKeywordBetweenClients(sourceTextAdGroupInfo.campaignId)

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullPriceKeywordSameTextSearchAbCampaign() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedKeyword: Keyword = copyKeywordSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            sourceTextAdGroupInfo.campaignId
        )

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun `copy keyword with '!' modifier and double number in minus words`() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)

        val expectedKeyword: Keyword = TestKeywords.createKeyword(null, null, DEFAULT_AUTOBUDGET_PRIORITY)
            .withPlace(Place.ROTATION)
            .withNeedCheckPlaceModified(true)
            .withNormPhrase("!вывоз !мусора")
            .withWordsCount(2)
            .withPhrase("!вывоз !мусора -!0.75 -!0.8 -!20 -строй -строймусор")

        steps.keywordSteps().addKeyword(sourceTextAdGroupInfo, expectedKeyword)

        val copiedKeyword: Keyword = copyKeywordSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            sourceTextAdGroupInfo.campaignId
        )

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullPriceKeywordTextSearchAbToNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedKeyword: Keyword = copyKeywordSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            destinationTextNotAbSearchCampaignInfo.campaignId
        )

        expectedKeyword.price = currencyTo.minPrice

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullPriceKeywordCpmSearchAbToNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceCpmAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveCpmBannerAdGroupWithKeywordsCriterionType(sourceCpmAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceCpmAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedKeyword: Keyword = copyKeywordSameClient(
            sourceCpmAdGroupInfo,
            sourceCpmAdGroupInfo.campaignId,
            destinationCpmNotAbSearchCampaignInfo.campaignId
        )

        expectedKeyword.price = currencyTo.minCpmPrice

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullPriceKeywordTextSearchAbCampaignBetweenClients() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedKeyword: Keyword = copyKeywordBetweenClients(sourceTextAdGroupInfo.campaignId)

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullPriceContextKeywordSameTextSearchAbCampaign() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedKeyword: Keyword = copyKeywordSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            sourceTextAdGroupInfo.campaignId
        )

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullPriceContextKeywordTextSearchAbToTextContextNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedKeyword: Keyword = copyKeywordSameClient(
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            destinationTextNotAbContextCampaignInfo.campaignId
        )

        expectedKeyword.priceContext = currencyTo.minPrice

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullPriceKeywordCpmSearchAbToContextNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceCpmAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveCpmBannerAdGroupWithKeywordsCriterionType(sourceCpmAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceCpmAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedKeyword: Keyword = copyKeywordSameClient(
            sourceCpmAdGroupInfo,
            sourceCpmAdGroupInfo.campaignId,
            destinationCpmNotAbContextCampaignInfo.campaignId
        )

        expectedKeyword.priceContext = currencyTo.minCpmPrice

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    @Test
    fun copyNullPriceContextKeywordTextSearchAbCampaignBetweenClients() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedKeyword: Keyword =
            createExpectedKeyword(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedKeyword: Keyword = copyKeywordBetweenClients(sourceTextAdGroupInfo.campaignId)

        assertKeywords(expectedKeyword, copiedKeyword)
    }

    private fun assertKeywords(expectedKeyword: Keyword, actualKeyword: Keyword) {
        assertThat(actualKeyword)
            .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
            .usingRecursiveComparison(KEYWORDS_COMPARE_STRATEGY)
            .isEqualTo(expectedKeyword)
    }

    private fun copyKeywordSameClient(
        sourceAdGroupInfo: AdGroupInfo,
        sourceCampaignId: Long,
        destinationCampaignId: Long
    ): Keyword {
        val copyConfig: CopyConfig<AdGroup, Long> =
            adGroupCopyConfig(client, sourceAdGroupInfo.adGroupId, sourceCampaignId, destinationCampaignId, client.uid)
        return getCopiedKeyword(copyConfig, client)
    }

    private fun copyKeywordBetweenClients(sourceCampaignId: Long): Keyword {
        val copyConfig: CopyConfig<*, *> =
            campaignsBetweenClientsCopyConfig(client, targetClient, sourceCampaignId, superClient.uid)
        return getCopiedKeyword(copyConfig, targetClient)
    }

    private fun getCopiedKeyword(copyConfig: CopyConfig<*, *>, targetClient: ClientInfo): Keyword {
        val copyOperation: CopyOperation<*, *> = copyOperationFactory.build(copyConfig)
        val copiedAdGroupId: Long = copyValidEntity(AdGroup::class.java, copyOperation, true)[0]
        val copiedKeywords: List<Keyword> = steps.keywordSteps()
            .getKeywordsByAdGroupId(targetClient.shard, copiedAdGroupId)
        assertThat(copiedKeywords.size).describedAs("Copied keywords count").isEqualTo(1)
        return copiedKeywords[0]
    }

    private fun createExpectedKeyword(
        adGroupInfo: AdGroupInfo,
        price: BigDecimal?,
        priceContext: BigDecimal?,
        autobudgetPriority: Int?
    ): Keyword {
        val expectedKeyword: Keyword = TestKeywords.createKeyword(price, priceContext, autobudgetPriority)
            .withPlace(Place.ROTATION)
            .withNeedCheckPlaceModified(true)

        steps.keywordSteps().addKeyword(adGroupInfo, expectedKeyword)

        return expectedKeyword
    }
}
