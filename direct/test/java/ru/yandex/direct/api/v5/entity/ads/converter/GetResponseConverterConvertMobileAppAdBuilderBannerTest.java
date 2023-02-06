package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdBuilderAdGetItem;
import com.yandex.direct.api.v5.ads.MobileAppAdBuilderAdGet;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.creative.model.Creative;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter.convertMobileAppAdBuilderBanner;

@ParametersAreNonnullByDefault
public class GetResponseConverterConvertMobileAppAdBuilderBannerTest {
    @Test
    public void trackingUrlIsConverted_not_null() {
        String href = "href";
        var ad = buildMobileAppAdBuilderAd().withHref(href);
        AdsGetContainer adsGetContainer = getContainerBuilder(ad).withCreative(new Creative().withId(1L)).build();

        MobileAppAdBuilderAdGet result = convertMobileAppAdBuilderBanner(adsGetContainer);
        assertThat(result.getTrackingUrl().getValue()).isEqualTo(href);
    }

    @Test
    public void trackingUrlIsConverted_null() {
        AdsGetContainer adsGetContainer = getContainerBuilder().withCreative(new Creative().withId(1L)).build();

        MobileAppAdBuilderAdGet result = convertMobileAppAdBuilderBanner(adsGetContainer);
        assertThat(result.getTrackingUrl().isNil()).isTrue();
    }

    @Test
    public void creativeIsConverted_only_id() {
        Long id = 1L;
        AdsGetContainer adsGetContainer = getContainerBuilder().withCreative(new Creative().withId(id)).build();

        MobileAppAdBuilderAdGet result = convertMobileAppAdBuilderBanner(adsGetContainer);
        assertThat(result.getCreative())
                .isEqualToComparingFieldByFieldRecursively(new AdBuilderAdGetItem().withCreativeId(id));
    }

    @Test
    public void creativeIsConverted() {
        Long id = 1L;
        String previewUrl = "previewUrl";
        String thumbnailUrl = "thumbnailUrl";
        AdsGetContainer adsGetContainer = getContainerBuilder().withCreative(
                new Creative().withId(id).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl)).build();

        MobileAppAdBuilderAdGet result = convertMobileAppAdBuilderBanner(adsGetContainer);
        assertThat(result.getCreative())
                .isEqualToComparingFieldByFieldRecursively(
                        new AdBuilderAdGetItem().withCreativeId(id).withPreviewUrl(previewUrl)
                                .withThumbnailUrl(thumbnailUrl));
    }

    //region utils
    private ImageBanner buildMobileAppAdBuilderAd() {
        return new ImageBanner()
                .withId(0L)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NEW)
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true)
                .withCreativeId(1L)
                .withIsMobileImage(true);
    }

    private AdsGetContainer.Builder getContainerBuilder(BannerWithSystemFields ad) {
        return new AdsGetContainer.Builder()
                .withAd(ad)
                .withCampaign(new Campaign()
                        .withStatusActive(true)
                        .withStatusArchived(false)
                        .withStatusShow(true));
    }

    private AdsGetContainer.Builder getContainerBuilder() {
        return getContainerBuilder(buildMobileAppAdBuilderAd());
    }

    private AdsGetContainer getContainer(BannerWithSystemFields ad) {
        return getContainerBuilder(ad).build();
    }

    private AdsGetContainer getContainer() {
        return getContainer(buildMobileAppAdBuilderAd());
    }
    //endregion
}
