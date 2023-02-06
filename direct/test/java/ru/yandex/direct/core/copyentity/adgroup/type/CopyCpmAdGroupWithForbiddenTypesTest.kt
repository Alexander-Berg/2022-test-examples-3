package ru.yandex.direct.core.copyentity.adgroup.type

import junitparams.JUnitParamsRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.copyentity.adgroup.BaseCopyAdGroupTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefectIds
import ru.yandex.direct.core.testing.configuration.CoreTest

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCpmAdGroupWithForbiddenTypesTest: BaseCopyAdGroupTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun copyCpmAudioAdGroupNegativeTest() {
        val adGroup = steps.adGroupSteps().createActiveCpmAudioAdGroup(client)
        val copyResult = sameCampaignAdGroupCopyOperation(adGroup).copy()
        copyAssert.assertResultContainsAllDefects(copyResult, listOf(AdGroupDefectIds.Gen.AD_GROUP_TYPE_NOT_SUPPORTED))
    }

    @Test
    fun copyCpmIndoorAdGroupNegativeTest() {
        val adGroup = steps.adGroupSteps().createActiveCpmIndoorAdGroup(client)
        val copyResult = sameCampaignAdGroupCopyOperation(adGroup).copy()
        copyAssert.assertResultContainsAllDefects(copyResult, listOf(AdGroupDefectIds.Gen.AD_GROUP_TYPE_NOT_SUPPORTED))
    }

    @Test
    fun copyCpmOutdoorAdGroupNegativeTest() {
        val adGroup = steps.adGroupSteps().createActiveCpmOutdoorAdGroup(client)
        val copyResult = sameCampaignAdGroupCopyOperation(adGroup).copy()
        copyAssert.assertResultContainsAllDefects(copyResult, listOf(AdGroupDefectIds.Gen.AD_GROUP_TYPE_NOT_SUPPORTED))
    }

    @Test
    fun copyCpmGeoPinAdGroupNegativeTest() {
        val adGroup = steps.adGroupSteps().createActiveCpmGeoPinAdGroup(client)
        val copyResult = sameCampaignAdGroupCopyOperation(adGroup).copy()
        copyAssert.assertResultContainsAllDefects(copyResult, listOf(AdGroupDefectIds.Gen.AD_GROUP_TYPE_NOT_SUPPORTED))
    }
    @Test

    fun copyCpmGeoproductAdGroupNegativeTest() {
        val adGroup = steps.adGroupSteps().createActiveCpmGeoproductAdGroup(client)
        val copyResult = sameCampaignAdGroupCopyOperation(adGroup).copy()
        copyAssert.assertResultContainsAllDefects(copyResult, listOf(AdGroupDefectIds.Gen.AD_GROUP_TYPE_NOT_SUPPORTED))
    }

}
