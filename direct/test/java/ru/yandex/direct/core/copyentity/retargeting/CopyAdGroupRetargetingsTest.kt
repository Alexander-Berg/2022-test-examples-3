package ru.yandex.direct.core.copyentity.retargeting

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.adgroup.BaseCopyAdGroupTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.entity.retargeting.model.Retargeting
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.RetargetingInfo
import ru.yandex.direct.feature.FeatureName

@CoreTest
class CopyAdGroupRetargetingsTest : BaseCopyAdGroupTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun `copy adgroup with retargeting`() {
        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(client)
        val retargeting = steps.retargetingSteps().createDefaultRetargeting(adGroup)

        val result = sameCampaignAdGroupCopyOperation(adGroup).copy()

        copyAssert.assertRetargetingIsCopied(retargeting.retargetingId, result)
    }

    @Test
    fun `copy adgroup with interest retargeting`() {
        steps.featureSteps().addClientFeature(client.clientId!!, FeatureName.TEXT_BANNER_INTERESTS_RET_COND_ENABLED, true)

        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(client)
        val retCondition = steps.retConditionSteps().createDefaultInterestRetCondition(client)
        val retargeting = steps.retargetingSteps().createRetargeting(RetargetingInfo()
            .withAdGroupInfo(adGroup)
            .withRetConditionInfo(retCondition))

        val result = sameCampaignAdGroupCopyOperation(adGroup).copy()

        val copiedRetargeting: Retargeting = copyAssert.getCopiedEntity(retargeting.retargetingId, result)
        assertThat(copiedRetargeting.retargetingConditionId).isNotEqualTo(retCondition.retConditionId)
    }
}
