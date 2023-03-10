package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.NewContentPromoBannerInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.contentpromotion.ContentPromotionTypeConverters.contentPromotionContentTypeToContentPromotionAdgroupType;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCommonData.defaultContentPromotion;
import static ru.yandex.direct.core.testing.data.adgroup.TestContentPromotionAdGroups.fullContentPromotionAdGroup;

@Deprecated
//use ContentPromotionBannerSteps
public class OldContentPromotionBannerSteps {

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private ContentPromotionSteps contentPromotionSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private TestContentPromotionBanners testNewContentPromotionBanners;

    @SuppressWarnings("ConstantConditions")
    public NewContentPromoBannerInfo createContentPromotionBanner(ContentPromotionContentType type) {
        return createContentPromotionBanner(
                new NewContentPromoBannerInfo().withContent(defaultContentPromotion(null, type)));
    }

    /**
     * ???????? ?? info ???? ???????????? ???? ????????????, ???? ??????????????, ???? ?????????????? ???????????????? ?? ??????????-????????????????????????.
     *
     * ???????? ???????????? ???????????? ????????????, ???? ?????????????? ?????????????? ???????????????????????????????? ???????? ?? ???????????? ?? ???????? ??????????????????.
     * ???????? ???????????? ???? ?????????????? ?? ????????, ???? ?????????????? ???? (?????? ?????????????????????????? ?? ?????????????????? ?? ????????????????).
     *
     * ???????? ?????????? ???????????? ??????????????, ???? ?????????????? ?????? ???????????????? ?? ?????????????? ???????????????????????????????? ???????????????? ????????.
     *
     * ?? ??????????, ?????????? ?????????????????? ?? info ?????????? ?????????????? ????????????????????, ?? ?????????? ???????????????? ?????? ??????????????????????.
     */
    public NewContentPromoBannerInfo createContentPromotionBanner(NewContentPromoBannerInfo bannerInfo) {
        checkBannerInfoConsistency(bannerInfo);

        // ???????? ???????????? ???? ??????????????, ?????????????? ????
        // (???????? ???????????????? ???????????? ?????????????? ???????????????? ?? ?????????????? ?????? ??????????????????????????)
        if (bannerInfo.getAdGroupId() == null) {
            if (bannerInfo.getAdGroupInfo().getAdGroup() == null) {
                ContentPromotionAdgroupType type = bannerInfo.getContent() != null ?
                        contentPromotionContentTypeToContentPromotionAdgroupType(bannerInfo.getContent().getType()) :
                        ContentPromotionAdgroupType.VIDEO;

                bannerInfo.getAdGroupInfo()
                        .withAdGroup(fullContentPromotionAdGroup(type));
            }
            adGroupSteps.createAdGroup(bannerInfo.getAdGroupInfo());
        }

        // ???????? ?????????????? ??????????, ???? ???? ?????????????? ?? ????????, ???? ???????????????????? ?????? (?? ???????? ?????????????????????????? id)
        if (bannerInfo.getContent() != null && bannerInfo.getContent().getId() == null) {
            bannerInfo.getContent().withClientId(bannerInfo.getClientId().asLong());
            contentPromotionSteps.createContentPromotionContent(
                    bannerInfo.getClientId(), bannerInfo.getContent());
        }

        // ???????? ?????????????? ???? ??????????, ?????????????? ??????
        if (bannerInfo.getContent() == null) {
            ContentPromotionAdGroup adGroup = (ContentPromotionAdGroup) bannerInfo.getAdGroupInfo().getAdGroup();
            ContentPromotionContentType type = adGroup != null ?
                    ContentPromotionContentType.valueOf(adGroup.getContentPromotionType().toString()) :
                    ContentPromotionContentType.VIDEO;
            ContentPromotionContent content = contentPromotionSteps.createContentPromotionContent(
                    bannerInfo.getClientId(), type);
            bannerInfo.withContent(content);
        }

        // ???????? ???????????? ???? ??????????, ?????????????????????????? ??????????????????
        if (bannerInfo.getBanner() == null) {
            ContentPromotionAdgroupType type =
                    contentPromotionContentTypeToContentPromotionAdgroupType(bannerInfo.getContent().getType());
            if (type == ContentPromotionAdgroupType.SERVICE) {
                bannerInfo.withBanner(testNewContentPromotionBanners.fullContentPromoServiceBanner(null, null));
            } else {
                bannerInfo.withBanner(testNewContentPromotionBanners.fullContentPromoBanner(null, null));
            }
        }

        // ?????????????????????? ???????? ?????????????? ?????????? ??????????????????????
        ContentPromotionBanner banner = bannerInfo.getBanner();
        banner.withAdGroupId(bannerInfo.getAdGroupId())
                .withCampaignId(bannerInfo.getCampaignId())
                .withContentPromotionId(bannerInfo.getContent().getId())
                .withHref(bannerInfo.getContent().getUrl());

        var container = new BannerRepositoryContainer(bannerInfo.getShard());

        bannerModifyRepository.add(dslContextProvider.ppc(bannerInfo.getShard()), container, singletonList(banner));

        return bannerInfo;
    }

    private static void checkBannerInfoConsistency(NewContentPromoBannerInfo bannerInfo) {
        AdGroup adGroup = bannerInfo.getAdGroupInfo().getAdGroup();
        ContentPromotionContent content = bannerInfo.getContent();
        Banner banner = bannerInfo.getBanner();

        if (adGroup != null) {
            checkState(adGroup instanceof ContentPromotionAdGroup, "adGroupType must be CONTENT_PROMOTION");
        }
        if (adGroup != null && content != null) {
            ContentPromotionAdGroup typedAdGroup = (ContentPromotionAdGroup) adGroup;
            String adGroupContentType = typedAdGroup.getContentPromotionType().name().toLowerCase();
            String contentType = content.getType().name().toLowerCase();
            checkState(adGroupContentType.equals(contentType),
                    "contentPromotionAdGroupType must correspond to content type");
        }
        if (banner != null) {
            checkState(banner instanceof ContentPromotionBanner,
                    "banner must be NewContentPromotionBanner");
        }
    }
}
