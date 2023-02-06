package ru.yandex.direct.core.testing.steps.banner

import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository
import ru.yandex.direct.core.entity.contentpromotion.ContentPromotionTypeConverters.contentPromotionContentTypeToContentPromotionAdgroupType
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType
import ru.yandex.direct.core.testing.data.TestContentPromotionCommonData.defaultContentPromotion
import ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo
import ru.yandex.direct.core.testing.info.banner.ContentPromotionBannerInfo
import ru.yandex.direct.core.testing.info.campaign.ContentPromotionCampaignInfo
import ru.yandex.direct.core.testing.steps.ContentPromotionSteps
import ru.yandex.direct.core.testing.steps.adgroup.AdGroupStepsFactory
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import kotlin.reflect.KClass

@Lazy
@Component
class ContentPromotionBannerSteps(
    adGroupStepsFactory: AdGroupStepsFactory,
    dslContextProvider: DslContextProvider,
    bannerModifyRepository: BannerModifyRepository,
    private val contentPromotionSteps: ContentPromotionSteps,
    private val testContentPromotionBanners: TestContentPromotionBanners)
    : BannerSteps<ContentPromotionBanner, ContentPromotionBannerInfo>(adGroupStepsFactory,
    dslContextProvider, bannerModifyRepository, ALLOWED_ADGROUP_CLASSES) {

    companion object {
        private val ALLOWED_ADGROUP_CLASSES = setOf(ContentPromotionAdGroup::class)
    }

    fun createDefaultBanner(type: ContentPromotionContentType): ContentPromotionBannerInfo {
        return createDefaultBanner(ClientInfo(), type)
    }

    fun createDefaultBanner(clientInfo: ClientInfo, type: ContentPromotionContentType): ContentPromotionBannerInfo {
        return createDefaultBanner(ContentPromotionAdGroupInfo()
            .withCampaignInfo(ContentPromotionCampaignInfo(clientInfo = clientInfo)), type)
    }

    fun createDefaultBanner(content: ContentPromotionContent): ContentPromotionBannerInfo {
        return createDefaultBanner(ContentPromotionAdGroupInfo(), content)
    }

    fun createDefaultBanner(adGroupInfo: AdGroupInfo<*>, type: ContentPromotionContentType): ContentPromotionBannerInfo {
        return createDefaultBanner(adGroupInfo, defaultContentPromotion(null, type))
    }

    fun createDefaultBanner(adGroupInfo: AdGroupInfo<*>, content: ContentPromotionContent): ContentPromotionBannerInfo {
        return createBanner(ContentPromotionBannerInfo()
            .withAdGroupInfo(adGroupInfo)
            .withContent(content))
    }

    fun createBanner(type: ContentPromotionContentType, banner: ContentPromotionBanner): ContentPromotionBannerInfo {
        return createBanner(ContentPromotionAdGroupInfo(), defaultContentPromotion(null, type), banner)
    }

    fun createBanner(adGroupInfo: AdGroupInfo<*>, content: ContentPromotionContent,
                     banner: ContentPromotionBanner): ContentPromotionBannerInfo {
        return createBanner(ContentPromotionBannerInfo()
            .withAdGroupInfo(adGroupInfo)
            .withContent(content)
            .withBanner(banner))
    }

    override fun initializeBannerInfo(bannerInfo: ContentPromotionBannerInfo) {
        if (!bannerInfo.isAdGroupInfoInitialized()) {
            bannerInfo.adGroupInfo = ContentPromotionAdGroupInfo()
        }

        if (!bannerInfo.adGroupInfo.isAdGroupInitialized()) {
            val type = if (bannerInfo.isContentInitialized())
                contentPromotionContentTypeToContentPromotionAdgroupType(bannerInfo.content.type)
            else ContentPromotionAdgroupType.VIDEO

            val adGroupInfo = bannerInfo.adGroupInfo as ContentPromotionAdGroupInfo
            adGroupInfo.adGroup = fullContentPromotionAdGroup(type)
        }

        if (!bannerInfo.isContentInitialized()) {
            val adGroup = bannerInfo.adGroupInfo.adGroup as ContentPromotionAdGroup
            val type = ContentPromotionContentType.valueOf(adGroup.contentPromotionType.toString())
            bannerInfo.content = defaultContentPromotion(null, type)
        }

        if (!bannerInfo.isBannerInitialized()) {
            val banner = when (bannerInfo.content.type!!) {
                ContentPromotionContentType.VIDEO -> testContentPromotionBanners.fullContentPromoBanner(null, null)
                ContentPromotionContentType.COLLECTION -> testContentPromotionBanners.fullContentPromoCollectionBanner(null, null)
                ContentPromotionContentType.SERVICE -> testContentPromotionBanners.fullContentPromoServiceBanner(null, null)
                ContentPromotionContentType.EDA -> testContentPromotionBanners.fullContentPromoBanner(null, null)
            }
            bannerInfo.banner = banner
        }
    }

    override fun getBannerInfo(): ContentPromotionBannerInfo {
        return ContentPromotionBannerInfo()
            .withAdGroupInfo(ContentPromotionAdGroupInfo())
            .withContent(defaultContentPromotion(null, ContentPromotionContentType.VIDEO))
            .withBanner(testContentPromotionBanners.fullContentPromoBanner(null, null))
    }

    override fun getBannerInfoClass(): KClass<ContentPromotionBannerInfo> {
        return ContentPromotionBannerInfo::class
    }

    override fun createRelations(bannerInfo: ContentPromotionBannerInfo) {
        // если контент задан, но не записан в базу, то записываем его (в него проставляется id)
        if (bannerInfo.content.id == null) {
            bannerInfo.content.clientId = bannerInfo.clientId!!.asLong()
            contentPromotionSteps.createContentPromotionContent(bannerInfo.clientId!!, bannerInfo.content)
        }
        bannerInfo.banner.contentPromotionId = bannerInfo.content.id
    }

    override fun checkBannerInfoConsistency(bannerInfo: ContentPromotionBannerInfo) {
        super.checkBannerInfoConsistency(bannerInfo)

        if (bannerInfo.isBannerInitialized() && !bannerInfo.isContentInitialized()
            && (!bannerInfo.isAdGroupInfoInitialized()
                || bannerInfo.isAdGroupInfoInitialized() && !bannerInfo.adGroupInfo.isAdGroupInitialized())) {
            throw IllegalStateException("cannot guess content type using only banner, add content or adGroup")
        }

        if (bannerInfo.adGroupInfo.isAdGroupInitialized() && bannerInfo.isContentInitialized()) {
            val adGroup: AdGroup = bannerInfo.adGroupInfo.adGroup
            val content: ContentPromotionContent = bannerInfo.content

            if (adGroup is ContentPromotionAdGroup) {
                val adGroupContentType = adGroup.contentPromotionType.name.toLowerCase()
                val contentType = content.type.name.toLowerCase()
                check(adGroupContentType == contentType) {
                    "contentPromotionAdGroupType must correspond to content type"
                }
            }
        }
    }
}
