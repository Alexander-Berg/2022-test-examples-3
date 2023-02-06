package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.adv.direct.adgroup.AdGroup
import ru.yandex.adv.direct.adgroup.SerpPlacementType
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.PlacementType
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultDynamicCampaignWithSystemFields
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration

@Suppress("UsePropertyAccessSyntax")
@ContextConfiguration(classes = [EssLogicProcessorTestConfiguration::class])
@ExtendWith(SpringExtension::class)
internal class AdGroupSerpPlacementTypeHandlerTest @Autowired constructor(
    private val steps: Steps,
    private val adGroupSerpPlacementTypeHandler: AdGroupSerpPlacementTypeHandler,
) {
    @Test
    fun `dynamic campaign without placement types`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup {
            placementTypes = setOf()
        }
        val builder = callAdGroupSerpPlacementTypeHandler(adGroupInfo)
        assertThat(builder.serpPlacementType).isEqualTo(SerpPlacementType.SERP_PLACEMENT_TYPE_UNSPECIFIED_VALUE)
    }

    @Test
    fun `dynamic campaign with both placement types`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup {
            placementTypes = setOf(PlacementType.SEARCH_PAGE, PlacementType.ADV_GALLERY)
        }
        val builder = callAdGroupSerpPlacementTypeHandler(adGroupInfo)
        assertThat(builder.serpPlacementType).isEqualTo(SerpPlacementType.SERP_PLACEMENT_TYPE_UNSPECIFIED_VALUE)
    }

    @Test
    fun `dynamic campaign with placement type adv_gallery`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup {
            placementTypes = setOf(PlacementType.ADV_GALLERY)
        }
        val builder = callAdGroupSerpPlacementTypeHandler(adGroupInfo)
        assertThat(builder.serpPlacementType).isEqualTo(SerpPlacementType.SERP_PLACEMENT_TYPE_PRODUCT_GALLERY_ONLY_VALUE)
    }

    @Test
    fun `dynamic campaign with placement type search_page`() {
        val adGroupInfo = createDynamicCampaignAndAdGroup {
            placementTypes = setOf(PlacementType.SEARCH_PAGE)
        }
        val builder = callAdGroupSerpPlacementTypeHandler(adGroupInfo)
        assertThat(builder.serpPlacementType).isEqualTo(SerpPlacementType.SERP_PLACEMENT_TYPE_NO_PRODUCT_GALLERY_VALUE)
    }

    private fun callAdGroupSerpPlacementTypeHandler(adGroupInfo: AdGroupInfo): AdGroup.Builder {
        val builder = AdGroup.newBuilder().setAdGroupId(adGroupInfo.adGroupId)
        val adGroupWithBuilder = AdGroupWithBuilder(adGroupInfo.adGroup, builder)
        adGroupSerpPlacementTypeHandler.handle(adGroupInfo.shard, mapOf(adGroupInfo.adGroupId to adGroupWithBuilder))
        return builder
    }

    private fun createDynamicCampaignAndAdGroup(init: DynamicCampaign.() -> Unit): AdGroupInfo {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val campaign = defaultDynamicCampaignWithSystemFields()
        campaign.init()
        val dynamicCampaignInfo = steps.dynamicCampaignSteps().createCampaign(clientInfo, campaign)
        return steps.adGroupSteps().createActiveDynamicTextAdGroup(dynamicCampaignInfo)
    }
}
