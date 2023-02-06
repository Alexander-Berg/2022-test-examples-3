package ru.yandex.direct.core.testing.data.banner;

import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.testing.data.TestNewBanners;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

public class TestContentPromotionBanners {

    private final BannersUrlHelper bannersUrlHelper;

    public TestContentPromotionBanners(BannersUrlHelper bannersUrlHelper) {
        this.bannersUrlHelper = bannersUrlHelper;
    }

    public static ContentPromotionBanner clientContentPromoBanner(Long contentPromoId) {
        ContentPromotionBanner banner = new ContentPromotionBanner();
        fillContentPromotionBannerClientFields(banner, contentPromoId);
        return banner;
    }

    public static ContentPromotionBanner clientContentPromoServiceBanner(Long contentPromoId) {
        ContentPromotionBanner banner = new ContentPromotionBanner();
        fillContentPromotionServiceBannerClientFields(banner, contentPromoId);
        return banner;
    }

    public static ContentPromotionBanner clientContentPromoEdaBanner(Long contentPromoId) {
        ContentPromotionBanner banner = new ContentPromotionBanner();
        fillContentPromotionEdaBannerClientFields(banner, contentPromoId);
        return banner;
    }


    public ContentPromotionBanner fullContentPromoBanner() {
        return fullContentPromoBanner(null, null);
    }

    public ContentPromotionBanner fullContentPromoBanner(Long contentPromoId, String href) {
        ContentPromotionBanner banner = new ContentPromotionBanner();
        fillContentPromotionBannerClientFields(banner, contentPromoId);
        fillContentPromotionBannerSystemFields(banner, href);
        return banner;
    }

    public ContentPromotionBanner fullContentPromoCollectionBanner(Long contentPromoId, String href) {
        ContentPromotionBanner banner = new ContentPromotionBanner();
        fillContentPromotionCollectionBannerClientFields(banner, contentPromoId);
        fillContentPromotionBannerSystemFields(banner, href);
        return banner;
    }

    public ContentPromotionBanner fullContentPromoServiceBanner(Long contentPromoId, String href) {
        ContentPromotionBanner banner = new ContentPromotionBanner();
        fillContentPromotionServiceBannerClientFields(banner, contentPromoId);
        fillContentPromotionBannerSystemFields(banner, href);
        return banner;
    }

    public ContentPromotionBanner fullContentPromoEdaBanner(Long contentPromoId, String href) {
        ContentPromotionBanner banner = new ContentPromotionBanner();
        fillContentPromotionEdaBannerClientFields(banner, contentPromoId);
        fillContentPromotionBannerSystemFields(banner, href);
        return banner;
    }

    private static void fillContentPromotionCollectionBannerClientFields(
            ContentPromotionBanner banner, Long contentPromoId) {
        banner
                .withContentPromotionId(contentPromoId)
                .withVisitUrl("https://www.yandex.ru");
    }

    private static void fillContentPromotionBannerClientFields(
            ContentPromotionBanner banner, Long contentPromoId) {
        banner.withTitle("Content Promo Title")
                .withBody("Content Promo Body")
                .withContentPromotionId(contentPromoId)
                .withVisitUrl("https://www.yandex.ru");
    }

    private static void fillContentPromotionServiceBannerClientFields(
            ContentPromotionBanner banner, Long contentPromoId) {
        banner.withTitle("Content Promo Title")
                .withContentPromotionId(contentPromoId);
    }

    private static void fillContentPromotionEdaBannerClientFields(
            ContentPromotionBanner banner, Long contentPromoId) {
        banner.withTitle("test eda promotion banner title " + randomNumeric(5))
                .withBody("test eda promotion banner body " + randomNumeric(5))
                .withContentPromotionId(contentPromoId);
    }

    private void fillContentPromotionBannerSystemFields(
            ContentPromotionBanner banner, String href) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        banner
                .withLanguage(Language.EN)
                .withHref(href)
                .withGeoFlag(false)
                .withDomain(bannersUrlHelper.extractHostFromHrefWithWwwOrNull(href));
    }
}
