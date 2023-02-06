package ru.yandex.direct.core.testing.data.adgroup

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType
import ru.yandex.direct.core.testing.data.adgroup.TestAdGroups.fillCommonClientFields
import ru.yandex.direct.core.testing.data.adgroup.TestAdGroups.fillCommonSystemFieldsForActiveAdGroup
import ru.yandex.direct.core.testing.data.adgroup.TestAdGroups.fillCommonSystemFieldsForDraftAdGroup

object TestContentPromotionAdGroups {

    @JvmStatic
    fun clientContentPromotionAdGroup(contentPromotionType: ContentPromotionAdgroupType): ContentPromotionAdGroup {
        val adGroup = ContentPromotionAdGroup()
        adGroup.contentPromotionType = contentPromotionType
        fillContentPromotionAdGroupClientFields(adGroup)
        return adGroup
    }

    @JvmStatic
    fun fullContentPromotionAdGroup(campaignId: Long, contentPromotionType: ContentPromotionAdgroupType): ContentPromotionAdGroup {
        return fullContentPromotionAdGroup(contentPromotionType).withCampaignId(campaignId)
    }

    @JvmStatic
    fun fullContentPromotionAdGroup(contentPromotionType: ContentPromotionAdgroupType): ContentPromotionAdGroup {
        val adGroup = clientContentPromotionAdGroup(contentPromotionType)
        fillContentPromotionAdGroupSystemFields(adGroup)
        return adGroup
    }

    @JvmStatic
    fun fullDraftContentPromotionAdGroup(contentPromotionType: ContentPromotionAdgroupType): ContentPromotionAdGroup {
        val adGroup = fullContentPromotionAdGroup(contentPromotionType)

        fillCommonSystemFieldsForDraftAdGroup(adGroup)
        return adGroup
    }

    private fun fillContentPromotionAdGroupClientFields(adGroup: ContentPromotionAdGroup) {
        fillCommonClientFields(adGroup)

        val tags = when (adGroup.contentPromotionType!!) {
            ContentPromotionAdgroupType.VIDEO -> listOf("content-promotion-video")
            ContentPromotionAdgroupType.COLLECTION -> listOf("content-promotion-collection")
            ContentPromotionAdgroupType.SERVICE -> listOf("yndx-services")
            ContentPromotionAdgroupType.EDA -> listOf("yndx-eda")
        }
        adGroup
            .withType(AdGroupType.CONTENT_PROMOTION)
            .withPageGroupTags(tags)
            .withTargetTags(tags)
    }

    private fun fillContentPromotionAdGroupSystemFields(adGroup: ContentPromotionAdGroup) {
        fillCommonSystemFieldsForActiveAdGroup(adGroup)
        adGroup

    }
}
