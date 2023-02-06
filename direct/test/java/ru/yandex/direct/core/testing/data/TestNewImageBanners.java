package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.NewStatusImageModerate;

public class TestNewImageBanners {

    private TestNewImageBanners() {
    }

    public static ImageBanner fullImageBannerWithImage(String imageHash) {
        return fullImageBannerWithImage(null, null, imageHash);
    }

    public static ImageBanner fullImageBannerWithImage(Long campaignId, Long adgroupId, String imageHash) {
        ImageBanner banner = clientImageBannerWithImage(imageHash)
                .withAdGroupId(adgroupId)
                .withCampaignId(campaignId);
        fillImageBannerWithImageSystemFields(banner);
        return banner;
    }

    private static void fillImageBannerWithImageSystemFields(ImageBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        fillImageBannerSystemFields(banner);
        banner.withImageStatusModerate(NewStatusImageModerate.YES);
    }

    public static ImageBanner clientImageBannerWithImage(String imageHash) {
        ImageBanner banner = new ImageBanner();
        fillImageBannerWithImageClientFields(banner, imageHash);
        return banner;
    }

    private static void fillImageBannerWithImageClientFields(ImageBanner banner, String imageHash) {
        fillImageBannerClientFields(banner);
        banner.withImageHash(imageHash);
    }

    public static ImageBanner fullImageBannerWithCreative(Long creativeId) {
        ImageBanner banner = clientImageBannerWithCreative(creativeId);
        fillImageBannerWithCreativeSystemFields(banner);
        return banner;
    }

    public static ImageBanner fullImageBannerWithCreative(Long campaignId, Long adgroupId, Long creativeId) {
        ImageBanner banner = clientImageBannerWithCreative(creativeId)
                .withAdGroupId(adgroupId)
                .withCampaignId(campaignId);
        fillImageBannerWithCreativeSystemFields(banner);
        return banner;
    }

    private static void fillImageBannerWithCreativeSystemFields(ImageBanner banner) {
        TestNewBanners.fillSystemFieldsForActiveBanner(banner);
        fillImageBannerSystemFields(banner);
        banner.withCreativeStatusModerate(BannerCreativeStatusModerate.YES);
    }

    public static ImageBanner clientImageBannerWithCreative(Long creativeId) {
        ImageBanner banner = new ImageBanner();
        fillImageBannerWithCreativeClientFields(banner, creativeId);
        return banner;
    }

    private static void fillImageBannerWithCreativeClientFields(ImageBanner banner, Long creativeId) {
        fillImageBannerClientFields(banner);
        banner.withCreativeId(creativeId);
    }

    private static void fillImageBannerClientFields(ImageBanner banner) {
        banner.withHref("https://www.yandex.ru");
    }

    private static void fillImageBannerSystemFields(ImageBanner banner) {
        banner.withIsMobileImage(false)
                .withHref("https://www.yandex.ru/company")
                .withDomain("www.yandex.ru")
                .withGeoFlag(false);
    }

}
