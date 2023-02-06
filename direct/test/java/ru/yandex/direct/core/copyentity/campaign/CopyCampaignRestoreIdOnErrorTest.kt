package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefectIds
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignRestoreIdOnErrorTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun copyCampaignIdsRestoredAfterValidationFailedOnAddOperation() {
        val campaign = steps.cpmBannerCampaignSteps().createCampaign(
            client,
            TestCpmBannerCampaigns.fullCpmBannerCampaign()
                .withStrategy(TestCampaignsStrategy.defaultStrategy())
        )
        val campaignIdBeforeCopy: Long = campaign.campaignId

        val result = sameClientCampaignCopyOperation(campaign).copy()
        val campaignAfterCopy: BaseCampaign = copyAssert.getFirstEntityFromContext(BaseCampaign::class.java, result)
        val newCampaignId: Long? = campaignAfterCopy.id

        softly {
            copyAssert.softlyAssertResultContainsAllDefects(
                result,
                listOf(StrategyDefectIds.Gen.INCONSISTENT_STATE_STRATEGY_TYPE_AND_CAMPAIGN_TYPE),
                this,
            )
            this.assertThat(newCampaignId)
                .describedAs("campaign id must be the same after a failed copy")
                .isEqualTo(campaignIdBeforeCopy as Long?)
        }
    }
}
