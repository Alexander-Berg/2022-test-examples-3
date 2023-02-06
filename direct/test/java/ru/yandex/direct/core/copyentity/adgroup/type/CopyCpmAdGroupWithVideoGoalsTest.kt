package ru.yandex.direct.core.copyentity.adgroup.type

import junitparams.JUnitParamsRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.adgroup.BaseCopyAdGroupTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCpmAdGroupWithVideoGoalsTest : BaseCopyAdGroupTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(client.clientId)
    }

    @Test
    fun copyCpmBannerAdGroupWithAllVideoShowTypes() {
        val adGroup = steps.adGroupSteps().createActiveCpmBannerAdGroupWithAllVideoShowTypes(client)
        val copyResult = sameCampaignAdGroupCopyOperation(adGroup).copy()
        copyAssert.assertAdGroupIsCopied(adGroup.adGroupId, copyResult)
    }

    @Test
    fun copyCpmVideoAdGroupWithAllVideoShowTypes() {
        val adGroup = steps.adGroupSteps().createActiveCpmVideoAdGroupWithAllVideoGoalTypes(client)
        val copyResult = sameCampaignAdGroupCopyOperation(adGroup).copy()
        copyAssert.assertAdGroupIsCopied(adGroup.adGroupId, copyResult)
    }
}
