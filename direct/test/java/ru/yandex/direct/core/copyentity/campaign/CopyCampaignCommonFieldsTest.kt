package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform.BOTH
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform.SEARCH
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.AUTO_CONTEXT_LIMIT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_CONTEXT_LIMIT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_CONTEXT_LIMIT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.NO_CONTEXT_LIMIT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.SHOWS_DISABLED_CONTEXT_LIMIT
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultAutobudgetStrategy
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultStrategy
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import java.math.BigDecimal

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignCommonFieldsTest : BaseCopyCampaignTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun testCopyBannersPerPage() {
        val campaign = steps.textCampaignSteps().createCampaign(
            client, fullTextCampaign()
                .withBannersPerPage(20L)
        )

        val copiedCampaign: TextCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.bannersPerPage).isEqualTo(20L)
    }

    private fun contextLimitParams() = listOf(
        listOf(AUTO_CONTEXT_LIMIT, BOTH, AUTO_CONTEXT_LIMIT),
        listOf(MIN_CONTEXT_LIMIT, BOTH, MIN_CONTEXT_LIMIT),
        listOf(50, BOTH, 50),
        listOf(MAX_CONTEXT_LIMIT, BOTH, MAX_CONTEXT_LIMIT),
        listOf(SHOWS_DISABLED_CONTEXT_LIMIT, SEARCH, SHOWS_DISABLED_CONTEXT_LIMIT),
        listOf(SHOWS_DISABLED_CONTEXT_LIMIT, BOTH, AUTO_CONTEXT_LIMIT),
        listOf(NO_CONTEXT_LIMIT, BOTH, AUTO_CONTEXT_LIMIT),
    )

    @Test
    @Parameters(method = "contextLimitParams")
    fun testCopyCampaignContextLimit(
        contextLimit: Int,
        platform: CampaignsPlatform,
        expectedContextLimit: Int,
    ) {
        val strategy = defaultStrategy(differentPlaces = false)
            .withPlatform(platform) as DbStrategy
        val campaign = steps.textCampaignSteps().createCampaign(
            client, fullTextCampaign()
                .withContextLimit(contextLimit)
                .withStrategy(strategy)
        )

        val copiedCampaign: TextCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.contextLimit).isEqualTo(expectedContextLimit)
    }

    @Test
    fun `copy campaign with forbidden day budget`() {
        val strategy = defaultAutobudgetStrategy()
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withStrategy(strategy)
            .withDayBudget(1000.toBigDecimal()))

        val copiedCampaign = copyValidCampaign(campaign)

        assertThat(copiedCampaign.dayBudget)
            .isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun testCopyCampaignWithInternalYandexDisabledIps() {
        val campaign = steps.textCampaignSteps().createCampaign(
            client, fullTextCampaign()
                .withDisabledIps(listOf("37.9.68.144", "66.249.95.253"))
        )

        val copiedCampaign: TextCampaign = copyValidCampaign(campaign)

        // В тестовой среде используется network-config.allow-all.json, где все ip объявляются внутренними,
        // поэтому, после копирования, список disabledIps должен стать null
        assertThat(copiedCampaign.disabledIps).isNull()
    }
}
