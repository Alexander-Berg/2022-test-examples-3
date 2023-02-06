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
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch
import ru.yandex.direct.core.entity.showcondition.Constants.DEFAULT_AUTOBUDGET_PRIORITY
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.currency.Currencies
import ru.yandex.direct.currency.Currency
import java.math.BigDecimal

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyRelevanceMatchesTest : BaseCopyBidsTest() {
    private val RELEVANCE_MATCH_COMPARE_STRATEGY = RecursiveComparisonConfiguration.builder()
        .withIgnoredFields("id", "adGroupId", "campaignId", "isSuspended", "isDeleted", "lastChangeTime")
        .build()

    @Before
    fun before() {
        initClients()
        initTextCampaigns()
    }

    @Test
    fun copyNullAbPriorityRelevanceMatchSameTextSearchNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextNotAbSearchCampaignInfo)
        val expectedRelevanceMatch: RelevanceMatch = steps.relevanceMatchSteps()
            .createRelevanceMatch(sourceTextAdGroupInfo, currencyTo.minPrice, currencyTo.minPrice, null)
        val copiedRelevanceMatch: RelevanceMatch = copyRelevanceMatchSameClient(
            expectedRelevanceMatch,
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            sourceTextAdGroupInfo.campaignId
        )

        assertRelevanceMatches(expectedRelevanceMatch, copiedRelevanceMatch)
    }

    @Test
    fun copyNullAbPriorityRelevanceMatchTextSearchNotAbToAbCampaign() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRelevanceMatch: RelevanceMatch =
            steps.relevanceMatchSteps().createRelevanceMatch(sourceTextAdGroupInfo, null, null, null)
        val copiedRelevanceMatch: RelevanceMatch = copyRelevanceMatchSameClient(
            expectedRelevanceMatch,
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            destinationTextAbSearchCampaignInfo.campaignId
        )

        expectedRelevanceMatch.autobudgetPriority = DEFAULT_AUTOBUDGET_PRIORITY

        assertRelevanceMatches(expectedRelevanceMatch, copiedRelevanceMatch)
    }

    @Test
    fun copyNullAbPriorityRelevanceMatchTextSearchNotAbCampaignBetweenClients() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextNotAbSearchCampaignInfo)
        val expectedRelevanceMatch: RelevanceMatch = steps.relevanceMatchSteps()
            .createRelevanceMatch(sourceTextAdGroupInfo, currencyTo.minPrice, currencyTo.minPrice, null)
        val copiedRelevanceMatch: RelevanceMatch = copyRelevanceMatchBetweenClients(
            expectedRelevanceMatch,
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
        )

        assertRelevanceMatches(expectedRelevanceMatch, copiedRelevanceMatch)
    }

    @Test
    fun copyNullPriceRelevanceMatchSameTextSearchAbCampaign() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRelevanceMatch: RelevanceMatch = steps.relevanceMatchSteps()
            .createRelevanceMatch(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedRelevanceMatch: RelevanceMatch = copyRelevanceMatchSameClient(
            expectedRelevanceMatch,
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            sourceTextAdGroupInfo.campaignId
        )

        assertRelevanceMatches(expectedRelevanceMatch, copiedRelevanceMatch)
    }

    @Test
    fun copyNullPriceRelevanceMatchTextSearchAbToNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRelevanceMatch = steps.relevanceMatchSteps()
            .createRelevanceMatch(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedRelevanceMatch: RelevanceMatch = copyRelevanceMatchSameClient(
            expectedRelevanceMatch,
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            destinationTextNotAbSearchCampaignInfo.campaignId
        )

        expectedRelevanceMatch.price = currencyTo.minPrice

        assertRelevanceMatches(expectedRelevanceMatch, copiedRelevanceMatch)
    }

    @Test
    fun copyNullPriceRelevanceMatchTextSearchAbCampaignBetweenClients() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRelevanceMatch: RelevanceMatch = steps.relevanceMatchSteps()
            .createRelevanceMatch(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedRelevanceMatch: RelevanceMatch = copyRelevanceMatchBetweenClients(
            expectedRelevanceMatch,
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
        )

        assertRelevanceMatches(expectedRelevanceMatch, copiedRelevanceMatch)
    }

    @Test
    fun copyNullPriceContextRelevanceMatchSameTextSearchAbCampaign() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRelevanceMatch: RelevanceMatch = steps.relevanceMatchSteps()
            .createRelevanceMatch(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedRelevanceMatch: RelevanceMatch = copyRelevanceMatchSameClient(
            expectedRelevanceMatch,
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            sourceTextAdGroupInfo.campaignId
        )

        assertRelevanceMatches(expectedRelevanceMatch, copiedRelevanceMatch)
    }

    @Test
    fun copyNullPriceContextRelevanceMatchTextSearchAbToTextContextNotAbCampaign() {
        val currencyTo: Currency = Currencies.getCurrency(client.client!!.workCurrency)
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRelevanceMatch: RelevanceMatch = steps.relevanceMatchSteps()
            .createRelevanceMatch(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedRelevanceMatch: RelevanceMatch = copyRelevanceMatchSameClient(
            expectedRelevanceMatch,
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId,
            destinationTextNotAbContextCampaignInfo.campaignId
        )

        expectedRelevanceMatch.priceContext = currencyTo.minPrice

        assertRelevanceMatches(expectedRelevanceMatch, copiedRelevanceMatch)
    }

    @Test
    fun copyNullPriceContextRelevanceMatchTextSearchAbCampaignBetweenClients() {
        val sourceTextAdGroupInfo: AdGroupInfo =
            steps.adGroupSteps().createActiveTextAdGroup(sourceTextAbSearchCampaignInfo)
        val expectedRelevanceMatch: RelevanceMatch = steps.relevanceMatchSteps()
            .createRelevanceMatch(sourceTextAdGroupInfo, null, null, DEFAULT_AUTOBUDGET_PRIORITY)
        val copiedRelevanceMatch: RelevanceMatch = copyRelevanceMatchBetweenClients(
            expectedRelevanceMatch,
            sourceTextAdGroupInfo,
            sourceTextAdGroupInfo.campaignId
        )

        assertRelevanceMatches(expectedRelevanceMatch, copiedRelevanceMatch)
    }

    private fun assertRelevanceMatches(expectedRelevanceMatch: RelevanceMatch, actualRelevanceMatch: RelevanceMatch) {
        assertThat(actualRelevanceMatch)
            .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
            .usingRecursiveComparison(RELEVANCE_MATCH_COMPARE_STRATEGY)
            .isEqualTo(expectedRelevanceMatch)
    }

    private fun copyRelevanceMatchSameClient(
        relevanceMatch: RelevanceMatch,
        sourceAdGroupInfo: AdGroupInfo,
        sourceCampaignId: Long,
        destinationCampaignId: Long
    ): RelevanceMatch {
        steps.relevanceMatchSteps().addRelevanceMatchToAdGroup(
            listOf(relevanceMatch), sourceAdGroupInfo
        )
        val copyConfig: CopyConfig<*, *> =
            adGroupCopyConfig(client, sourceAdGroupInfo.adGroupId, sourceCampaignId, destinationCampaignId, client.uid)
        return getCopiedRelevanceMatch(copyConfig, client)
    }

    private fun copyRelevanceMatchBetweenClients(
        relevanceMatch: RelevanceMatch,
        sourceAdGroupInfo: AdGroupInfo,
        sourceCampaignId: Long
    ): RelevanceMatch {
        steps.relevanceMatchSteps().addRelevanceMatchToAdGroup(listOf(relevanceMatch), sourceAdGroupInfo)
        val copyConfig: CopyConfig<*, *> =
            campaignsBetweenClientsCopyConfig(client, targetClient, sourceCampaignId, superClient.uid)
        return getCopiedRelevanceMatch(copyConfig, targetClient)
    }

    private fun getCopiedRelevanceMatch(copyConfig: CopyConfig<*, *>, targetClient: ClientInfo): RelevanceMatch {
        val copyOperation: CopyOperation<*, *> = copyOperationFactory.build(copyConfig)
        val copiedAdGroupId: Long = copyValidEntity(AdGroup::class.java, copyOperation, true)[0]
        val copiedRelevanceMatches: List<RelevanceMatch> = steps.relevanceMatchSteps()
            .getRelevanceMatchesByAdGroupId(targetClient.shard, targetClient.clientId, copiedAdGroupId, false)
        assertThat(copiedRelevanceMatches.size).describedAs("Copied relevance match count").isEqualTo(1)
        return copiedRelevanceMatches[0]
    }
}
