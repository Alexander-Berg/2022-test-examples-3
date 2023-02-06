package ru.yandex.direct.core.copyentity.adgroup

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.adgroup.model.CpmYndxFrontpageAdGroup
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestGroups.activeCpmYndxFrontpageAdGroup
import ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.rbac.RbacRole

@CoreTest
class CopyAdGroupBsTagsTest : BaseCopyAdGroupTest() {

    @Autowired
    private lateinit var testCpmYndxFrontpageRepository: TestCpmYndxFrontpageRepository

    private val customTag = "video_tgo_zen_debug"

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        steps.featureSteps().setCurrentClient(client.clientId)
        testCpmYndxFrontpageRepository.fillMinBidsTestValues()
    }

    @Test
    fun `copy text adgroup with custom page group bs tag without access`() {
        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withAdGroup(activeTextAdGroup(null)
                .withPageGroupTags(listOf(customTag))))

        val copiedAdGroup: TextAdGroup = copyValidAdGroup(adGroup)

        assertThat(copiedAdGroup.pageGroupTags).isEmpty()
    }

    @Test
    fun `copy text adgroup with custom page group bs tag with client access`() {
        steps.featureSteps().addClientFeature(client.clientId!!, FeatureName.TARGET_TAGS_ALLOWED, true)

        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withAdGroup(activeTextAdGroup(null)
                .withPageGroupTags(listOf(customTag))))

        val copiedAdGroup: TextAdGroup = copyValidAdGroup(adGroup)

        assertThat(copiedAdGroup.pageGroupTags).containsExactly(customTag)
    }

    @Test
    fun `copy text adgroup with custom page group bs tag with operator access`() {
        val operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        steps.featureSteps().addClientFeature(operator.clientId!!, FeatureName.TARGET_TAGS_ALLOWED, true)

        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withAdGroup(activeTextAdGroup(null)
                .withPageGroupTags(listOf(customTag))))

        val operation = sameCampaignAdGroupCopyOperation(adGroup, operatorUid = operator.uid)
        val adGroupId = copyValidAdGroup(operation).first()
        val copiedAdGroup: TextAdGroup = getAdGroup(adGroupId)

        assertThat(copiedAdGroup.pageGroupTags).containsExactly(customTag)
    }

    @Test
    fun `copy text adgroup with custom target bs tag without access`() {
        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withAdGroup(activeTextAdGroup(null)
                .withTargetTags(listOf(customTag))))

        val copiedAdGroup: TextAdGroup = copyValidAdGroup(adGroup)

        assertThat(copiedAdGroup.targetTags).isEmpty()
    }

    @Test
    fun `copy text adgroup with custom target bs tag with client access`() {
        steps.featureSteps().addClientFeature(client.clientId!!, FeatureName.TARGET_TAGS_ALLOWED, true)

        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withAdGroup(activeTextAdGroup(null)
                .withTargetTags(listOf(customTag))))

        val copiedAdGroup: TextAdGroup = copyValidAdGroup(adGroup)

        assertThat(copiedAdGroup.targetTags).containsExactly(customTag)
    }

    @Test
    fun `copy text adgroup with custom target bs tag with operator access`() {
        val operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
        steps.featureSteps().addClientFeature(operator.clientId!!, FeatureName.TARGET_TAGS_ALLOWED, true)

        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withAdGroup(activeTextAdGroup(null)
                .withTargetTags(listOf(customTag))))

        val operation = sameCampaignAdGroupCopyOperation(adGroup, operatorUid = operator.uid)
        val adGroupId = copyValidAdGroup(operation).first()
        val copiedAdGroup: TextAdGroup = getAdGroup(adGroupId)

        assertThat(copiedAdGroup.targetTags).containsExactly(customTag)
    }

    @Test
    fun `copying yndx_frontpage adgroup preserves default tags`() {
        val adGroup = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(client)

        val copiedAdGroup: CpmYndxFrontpageAdGroup = copyValidAdGroup(adGroup)

        softly {
            assertThat(copiedAdGroup.pageGroupTags).isNotEmpty
            assertThat(copiedAdGroup.targetTags).isNotEmpty
        }
    }

    @Test
    fun `copy yndx_frontpage adgroup custom tags without access`() {
        val campaign = steps.cpmYndxFrontPageSteps().createDefaultCampaign(client)
        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withCampaignInfo(campaign)
            .withAdGroup(activeCpmYndxFrontpageAdGroup(null)
                .withPageGroupTags(listOf(customTag))
                .withTargetTags(listOf(customTag))))

        val copiedAdGroup: CpmYndxFrontpageAdGroup = copyValidAdGroup(adGroup)

        softly {
            assertThat(copiedAdGroup.pageGroupTags).doesNotContain(customTag)
            assertThat(copiedAdGroup.targetTags).doesNotContain(customTag)
        }
    }

    @Test
    fun `copy yndx_frontpage adgroup custom tags with access`() {
        steps.featureSteps().addClientFeature(client.clientId!!, FeatureName.TARGET_TAGS_ALLOWED, true)

        val campaign = steps.cpmYndxFrontPageSteps().createDefaultCampaign(client)
        val adGroup = steps.adGroupSteps().createAdGroup(AdGroupInfo()
            .withClientInfo(client)
            .withCampaignInfo(campaign)
            .withAdGroup(activeCpmYndxFrontpageAdGroup(null)
                .withPageGroupTags(listOf(customTag))
                .withTargetTags(listOf(customTag))))

        val copiedAdGroup: CpmYndxFrontpageAdGroup = copyValidAdGroup(adGroup)

        softly {
            assertThat(copiedAdGroup.pageGroupTags).contains(customTag)
            assertThat(copiedAdGroup.targetTags).contains(customTag)
        }
    }
}
