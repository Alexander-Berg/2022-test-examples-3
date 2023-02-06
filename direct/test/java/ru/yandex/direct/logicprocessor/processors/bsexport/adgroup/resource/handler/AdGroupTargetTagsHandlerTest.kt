package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.adgroup.AdGroup
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.PlacementType
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration

@Suppress("UsePropertyAccessSyntax")
@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
internal class AdGroupTargetTagsHandlerTest @Autowired constructor(
    private val steps: Steps,
    private val adGroupTargetTagsHandler: AdGroupTargetTagsHandler,
) {
    @Test
    fun `dynamic campaign without placement types, adgroup with target tag`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup(adGroupTags = listOf("xxx")) {
            placementTypes = setOf()
        }
        val builder = callAdGroupTargetTagsHandler(adGroupInfo)
        Assertions.assertThat(builder.targetTagsList).containsExactly("xxx")
    }

    @Test
    fun `dynamic campaign without placement types, adgroup without target tag`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup(adGroupTags = null) {
            placementTypes = setOf()
        }
        val builder = callAdGroupTargetTagsHandler(adGroupInfo)
        Assertions.assertThat(builder.targetTagsList).isEmpty()
    }

    @Test
    fun `dynamic campaign with both placement types, adgroup with target tag`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup(adGroupTags = listOf("xxx")) {
            placementTypes = setOf(PlacementType.SEARCH_PAGE, PlacementType.ADV_GALLERY)
        }
        val builder = callAdGroupTargetTagsHandler(adGroupInfo)
        Assertions.assertThat(builder.targetTagsList).containsExactly("xxx")
    }

    @Test
    fun `dynamic campaign with placement type adv_gallery, adgroup with target tag`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup(adGroupTags = listOf("xxx")) {
            placementTypes = setOf(PlacementType.ADV_GALLERY)
        }
        val builder = callAdGroupTargetTagsHandler(adGroupInfo)
        Assertions.assertThat(builder.targetTagsList).containsExactly("xxx", "bko-only")
    }

    @Test
    fun `dynamic campaign with placement type adv_gallery, adgroup without target tag`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup(adGroupTags = emptyList()) {
            placementTypes = setOf(PlacementType.ADV_GALLERY)
        }
        val builder = callAdGroupTargetTagsHandler(adGroupInfo)
        Assertions.assertThat(builder.targetTagsList).containsExactly("bko-only")
    }

    @Test
    fun `dynamic campaign with placement type search_page, adgroup with target tag`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup(adGroupTags = listOf("xxx")) {
            placementTypes = setOf(PlacementType.SEARCH_PAGE)
        }
        val builder = callAdGroupTargetTagsHandler(adGroupInfo)
        Assertions.assertThat(builder.targetTagsList).containsExactly("xxx")
    }

    private fun callAdGroupTargetTagsHandler(adGroupInfo: AdGroupInfo): AdGroup.Builder {
        val builder = AdGroup.newBuilder().setAdGroupId(adGroupInfo.adGroupId)
        val adGroupWithBuilder = AdGroupWithBuilder(adGroupInfo.adGroup, builder)
        adGroupTargetTagsHandler.handle(adGroupInfo.shard, mapOf(adGroupInfo.adGroupId to adGroupWithBuilder))
        return builder
    }

    private fun createDynamicCampaignAndAdGroup(adGroupTags: List<String>?, init: DynamicCampaign.() -> Unit): AdGroupInfo {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val campaign = TestCampaigns.defaultDynamicCampaignWithSystemFields()
        campaign.init()
        val dynamicCampaignInfo = steps.dynamicCampaignSteps().createCampaign(clientInfo, campaign)
        val dynamicAdGroup = TestGroups.activeDynamicTextAdGroup(dynamicCampaignInfo.campaignId)
            .withTargetTags(adGroupTags)
        return steps.adGroupSteps().createDynamicTextAdGroup(dynamicCampaignInfo, dynamicAdGroup)
    }
}
