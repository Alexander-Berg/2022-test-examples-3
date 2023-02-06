package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdBuilderAdGetItem;
import com.yandex.direct.api.v5.ads.MobileAppCpcVideoAdBuilderAdGet;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdGet;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.creative.model.Creative;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter.convertMobileAppCpcVideoBanner;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter.convertSmartAdBuilderBanner;

@ParametersAreNonnullByDefault
public class GetResponseConverterConvertMobileAppCpcVideoBannerTest {

    private static final String PREVIEW_URL = "previewUrl";
    private static final String THUMBNAIL_URL = "thumbnailUrl";

    private static final Long CREATIVE_ID = 57L;

    @Test
    public void trackingUrlIsConverted_not_null() {
        String href = "href";

        Creative creative = new Creative()
                .withId(CREATIVE_ID);
        AdsGetContainer getContainerBuilder = getContainerBuilder(href)
                .withCreative(creative)
                .build();

        MobileAppCpcVideoAdBuilderAdGet result = convertMobileAppCpcVideoBanner(getContainerBuilder);

        AdBuilderAdGetItem expectedCreative = new AdBuilderAdGetItem()
                .withCreativeId(CREATIVE_ID);

        assertThat(result.getCreative())
                .as("креатив")
                .isEqualToComparingFieldByFieldRecursively(expectedCreative);
        assertThat(result.getTrackingUrl().getValue())
                .as("трекинговая ссылка")
                .isEqualTo(href);
    }

    @Test
    public void trackingUrlIsConverted_null() {
        AdsGetContainer getContainerBuilder = getContainerBuilder(null).build();

        MobileAppCpcVideoAdBuilderAdGet result = convertMobileAppCpcVideoBanner(getContainerBuilder);
        assertThat(result.getTrackingUrl().isNil())
                .as("трекинговая ссылка")
                .isTrue();
    }

    @Test
    public void creativeIsConverted() {
        Creative creative = new Creative()
                .withId(CREATIVE_ID)
                .withPreviewUrl(THUMBNAIL_URL)
                .withLivePreviewUrl(PREVIEW_URL);
        AdsGetContainer adsGetContainer = getContainerBuilder(null)
                .withCreative(creative)
                .build();

        SmartAdBuilderAdGet result = convertSmartAdBuilderBanner(adsGetContainer);
        AdBuilderAdGetItem expectedCreative = new AdBuilderAdGetItem()
                .withCreativeId(CREATIVE_ID)
                .withPreviewUrl(PREVIEW_URL)
                .withThumbnailUrl(THUMBNAIL_URL);
        assertThat(result.getCreative())
                .as("креатив")
                .isEqualToComparingFieldByFieldRecursively(expectedCreative);
    }

    private AdsGetContainer.Builder getContainerBuilder(@Nullable String href) {
        return new AdsGetContainer.Builder()
                .withAd(buildMobileAppCpcVideoAd(href))
                .withCampaign(new Campaign());
    }

    private CpcVideoBanner buildMobileAppCpcVideoAd(@Nullable String href) {
        return new CpcVideoBanner()
                .withId(0L)
                .withIsMobileVideo(true)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NEW)
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true)
                .withHref(href);
    }
}

