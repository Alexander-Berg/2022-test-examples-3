package ru.yandex.direct.core.testing.info.banner

import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo

class ContentPromotionBannerInfo
    : BannerInfo<ContentPromotionBanner>() {

    lateinit var content: ContentPromotionContent

    val contentPromotionAdGroupInfo: ContentPromotionAdGroupInfo get() = adGroupInfo as ContentPromotionAdGroupInfo
    val contentId: Long get() = content.id

    fun isContentInitialized() = ::content.isInitialized

    override fun withAdGroupInfo(adGroupInfo: AdGroupInfo<*>): ContentPromotionBannerInfo {
        return super.withAdGroupInfo(adGroupInfo) as ContentPromotionBannerInfo
    }

    fun withContent(content: ContentPromotionContent) = apply {
        this.content = content
    }

    override fun withBanner(banner: ContentPromotionBanner): ContentPromotionBannerInfo {
        return super.withBanner(banner) as ContentPromotionBannerInfo
    }
}
