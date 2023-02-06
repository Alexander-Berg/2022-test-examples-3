package ru.yandex.direct.core.copyentity.campaign.type

import junitparams.JUnitParamsRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.campaign.BaseCopyCampaignTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestCpmYndxFrontPageCampaigns

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCpmYndxFrontPageCampaignTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun copyDefaultCpmYndxFrontpageCampaign() {
        val campaign = steps.cpmYndxFrontPageSteps().createCampaign(
            client,
            TestCpmYndxFrontPageCampaigns.fullCpmYndxFrontpageCampaign()
        )
        val result = sameClientCampaignCopyOperation(campaign).copy()
        copyAssert.assertCampaignIsCopied(campaign.id, result)
    }

}
