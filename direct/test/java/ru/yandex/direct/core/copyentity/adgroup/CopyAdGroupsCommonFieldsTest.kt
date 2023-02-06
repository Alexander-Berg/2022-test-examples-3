package ru.yandex.direct.core.copyentity.adgroup

import junitparams.JUnitParamsRunner
import org.apache.commons.lang.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupValidationService
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.info.AdGroupInfo

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyAdGroupsCommonFieldsTest : BaseCopyAdGroupTest() {

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun testNonZeroPriorityId() {
        val adGroup = steps.adGroupSteps().createDefaultAdGroup(client)

        assertThat(adGroup.adGroup.priorityId).isNotZero

        val operation = sameCampaignAdGroupCopyOperation(adGroup)
        val adGroupId = copyValidAdGroup(operation)[0]
        val copiedAdGroup: TextAdGroup = getAdGroup(adGroupId)

        softly {
            assertThat(copiedAdGroup.priorityId).isZero
        }
    }

    @Test
    fun `copy adgroup with name exceeding max length to same campaign`() {
        val name = RandomStringUtils.randomAlphanumeric(AdGroupValidationService.MAX_NAME_LENGTH + 10)
        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withAdGroup(TestGroups.activeTextAdGroup()
                .withName(name)))

        val copiedAdGroup: TextAdGroup = copyValidAdGroup(adGroup)

        assertThat(copiedAdGroup.name).isNotEqualTo(adGroup.adGroup.name)
    }

    @Test
    fun `copy adgroup with name exceeding max length to different campaign`() {
        val name = RandomStringUtils.randomAlphanumeric(AdGroupValidationService.MAX_NAME_LENGTH + 10)
        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withAdGroup(TestGroups.activeTextAdGroup()
                .withName(name)))
        val targetCampaign = steps.textCampaignSteps().createDefaultCampaign(client)

        val operation = otherCampaignAdGroupCopyOperation(adGroup, targetCampaign)
        val adGroupId = copyValidAdGroup(operation).first()
        val copiedAdGroup: TextAdGroup = getAdGroup(adGroupId)

        assertThat(copiedAdGroup.name).isEqualTo(name.substring(0 until AdGroupValidationService.MAX_NAME_LENGTH))
    }
}
