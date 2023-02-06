package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.common

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.campaign.model.CampaignWithPlacementTypes
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.PlacementType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AdGroupCommonConstantsTest {
    private fun targetTagsVariants() = listOf(
        GetTargetTagsCase(
            "No tags on group, no campaign, default tags",
            AdGroupType.CPM_GEOPRODUCT, null, null,
            expected = listOf("app-metro")
        ),
        GetTargetTagsCase(
            "Tags on group, no campaign",
            AdGroupType.CPM_GEOPRODUCT, listOf("xxx"), null,
            expected = listOf("xxx")
        ),
        GetTargetTagsCase(
            "Tags on group, campaign",
            AdGroupType.CPM_GEOPRODUCT, listOf("xxx"), createCampaignWithPlacementTypes(setOf(PlacementType.ADV_GALLERY)),
            expected = listOf("xxx", "bko-only")
        ),
        GetTargetTagsCase(
            "No tags on group, no campaign, no default tags",
            AdGroupType.DYNAMIC, null, null,
            expected = null
        ),
        GetTargetTagsCase(
            "Tags on group, no campaign",
            AdGroupType.DYNAMIC, listOf("xxx"), null,
            expected = listOf("xxx")
        ),
        GetTargetTagsCase(
            "Tags on group, campaigns with empty placementTypes",
            AdGroupType.DYNAMIC, listOf("xxx"), createCampaignWithPlacementTypes(setOf()),
            expected = listOf("xxx")
        ),
        GetTargetTagsCase(
            "Tags on group, campaigns with adv_gallery",
            AdGroupType.DYNAMIC, listOf("xxx"), createCampaignWithPlacementTypes(setOf(PlacementType.ADV_GALLERY)),
            expected = listOf("xxx", "bko-only")
        ),
        GetTargetTagsCase(
            "Tags on group, campaigns with search_page",
            AdGroupType.DYNAMIC, listOf("xxx"), createCampaignWithPlacementTypes(setOf(PlacementType.SEARCH_PAGE)),
            expected = listOf("xxx")
        ),
        GetTargetTagsCase(
            "Tags on group, campaigns with adv_gallery and search_page",
            AdGroupType.DYNAMIC, listOf("xxx"), createCampaignWithPlacementTypes(setOf(PlacementType.SEARCH_PAGE, PlacementType.ADV_GALLERY)),
            expected = listOf("xxx")
        ),
    )

    @ParameterizedTest
    @MethodSource("targetTagsVariants")
    fun getTargetTagsTest(testCase: GetTargetTagsCase) {
        val adGroup = AdGroup()
            .withType(testCase.adGroupType)
            .withTargetTags(testCase.adGroupTargetTags)
        val targetTags = getTargetTags(adGroup, testCase.campaign)
        Assertions.assertThat(targetTags).isEqualTo(testCase.expected)
    }

    private fun createCampaignWithPlacementTypes(placementTypes: Set<PlacementType>?): CampaignWithPlacementTypes {
        return DynamicCampaign().withPlacementTypes(placementTypes)
    }

    data class GetTargetTagsCase(
        val displayName: String,
        val adGroupType: AdGroupType,
        val adGroupTargetTags: List<String>?,
        val campaign: CampaignWithPlacementTypes?,
        val expected: List<String>?,
    ) {
        override fun toString(): String = displayName
    }
}
