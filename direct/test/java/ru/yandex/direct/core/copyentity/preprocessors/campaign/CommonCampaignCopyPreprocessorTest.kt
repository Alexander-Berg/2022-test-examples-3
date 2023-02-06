package ru.yandex.direct.core.copyentity.preprocessors.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.spring.AbstractSpringTest
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.defaultBetweenShardsCopyContainer
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.defaultCopyContainer
import ru.yandex.direct.core.copyentity.translations.RenameProcessor
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.currency.CurrencyCode

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CommonCampaignCopyPreprocessorTest : AbstractSpringTest() {

    @Autowired
    private lateinit var renameProcessor: RenameProcessor

    @Autowired
    private lateinit var commonCampaignCopyPreprocessor: CommonCampaignCopyPreprocessor

    @Test
    fun testCampaignIsRenamed() {
        val campaignName = "Campaign name"
        val campaign = fullTextCampaign()
            .withName(campaignName)
        val copyContainer = defaultCopyContainer()

        commonCampaignCopyPreprocessor.preprocess(campaign, copyContainer)

        val expectedName = renameProcessor.generateCampaignCopyName(campaignName, campaign.id, copyContainer.locale)
        assertThat(campaign.name).isEqualTo(expectedName)
    }

    @Test
    fun testCampaignCopiedFrom() {
        val oldCampaignId: Long = 100
        val campaign = fullTextCampaign()
            .withId(oldCampaignId)
        val copyContainer = defaultCopyContainer()

        commonCampaignCopyPreprocessor.preprocess(campaign, copyContainer)

        assertThat(campaign.copiedFrom).isEqualTo(oldCampaignId)
    }

    @Test
    @Parameters(
        "RUB, RUB, RUB",
        "USD, USD, USD",
        "USD, USD, RUB",
    )
    fun testCampaignCurrency(
        campaignCurrency: CurrencyCode,
        fromCurrency: CurrencyCode,
        toCurrency: CurrencyCode,
    ) {
        val campaign = fullTextCampaign()
            .withCurrency(campaignCurrency)
        val copyContainer = defaultBetweenShardsCopyContainer(fromCurrency, toCurrency)

        commonCampaignCopyPreprocessor.preprocess(campaign, copyContainer)

        assertThat(campaign.currency).isEqualTo(toCurrency)
    }
}
