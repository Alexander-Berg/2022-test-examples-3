package ru.yandex.direct.core.copyentity.preprocessors.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.spring.AbstractSpringTest
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.defaultCopyContainer
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.AUTO_CONTEXT_LIMIT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.NO_CONTEXT_LIMIT
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.SHOWS_DISABLED_CONTEXT_LIMIT
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.defaultStrategy
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CampaignWithNetworkSettingsCopyPreprocessorTest : AbstractSpringTest() {

    @Autowired
    private lateinit var campaignWithNetworkSettingsCopyPreprocessor: CampaignWithNetworkSettingsCopyPreprocessor

    fun contextLimitParams() = arrayOf(
        arrayOf(50, CampaignsPlatform.BOTH, 50),
        arrayOf(100, CampaignsPlatform.BOTH, 100),
        arrayOf(SHOWS_DISABLED_CONTEXT_LIMIT, CampaignsPlatform.SEARCH, SHOWS_DISABLED_CONTEXT_LIMIT),

        arrayOf(AUTO_CONTEXT_LIMIT, CampaignsPlatform.BOTH, null),
        arrayOf(SHOWS_DISABLED_CONTEXT_LIMIT, CampaignsPlatform.BOTH, null),
        arrayOf(NO_CONTEXT_LIMIT, CampaignsPlatform.BOTH, null),
    )

    @Test
    @Parameters(method = "contextLimitParams")
    fun testContextLimit(
        contextLimit: Int,
        platform: CampaignsPlatform,
        expectedContextLimit: Int?,
    ) {
        val campaign = fullTextCampaign()
            .withContextLimit(contextLimit)
            .withStrategy(defaultStrategy().withPlatform(platform) as DbStrategy)

        campaignWithNetworkSettingsCopyPreprocessor.preprocess(campaign, defaultCopyContainer())

        val actualContextLimit: Int? = campaign.contextLimit
        assertThat(actualContextLimit).isEqualTo(expectedContextLimit)
    }

}
