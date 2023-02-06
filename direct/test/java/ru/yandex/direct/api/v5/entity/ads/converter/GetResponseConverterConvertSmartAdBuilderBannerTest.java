package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdBuilderAdGetItem;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdGet;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.creative.model.Creative;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter.convertSmartAdBuilderBanner;

@ParametersAreNonnullByDefault
public class GetResponseConverterConvertSmartAdBuilderBannerTest {

    private static final String PREVIEW_URL = "previewUrl";
    private static final String THUMBNAIL_URL = "thumbnailUrl";

    private static final Long CREATIVE_ID = 1L;

    @Test
    public void creativeIsConverted_only_id() {
        Creative creative = new Creative().withId(CREATIVE_ID);
        AdsGetContainer adsGetContainer = getContainerBuilder().withCreative(creative).build();

        SmartAdBuilderAdGet result = convertSmartAdBuilderBanner(adsGetContainer);
        AdBuilderAdGetItem expectedCreative = new AdBuilderAdGetItem().withCreativeId(CREATIVE_ID);
        assertThat(result.getCreative()).isEqualToComparingFieldByFieldRecursively(expectedCreative);
    }

    @Test
    public void creativeIsConverted() {
        Creative creative = new Creative()
                .withId(CREATIVE_ID)
                .withPreviewUrl(THUMBNAIL_URL)
                .withLivePreviewUrl(PREVIEW_URL);
        AdsGetContainer adsGetContainer = getContainerBuilder().withCreative(creative).build();

        SmartAdBuilderAdGet result = convertSmartAdBuilderBanner(adsGetContainer);
        AdBuilderAdGetItem expectedCreative = new AdBuilderAdGetItem()
                .withCreativeId(CREATIVE_ID)
                .withPreviewUrl(PREVIEW_URL)
                .withThumbnailUrl(THUMBNAIL_URL);
        assertThat(result.getCreative()).isEqualToComparingFieldByFieldRecursively(expectedCreative);
    }

    private AdsGetContainer.Builder getContainerBuilder() {
        return new AdsGetContainer.Builder()
                .withAd(new PerformanceBanner())
                .withCampaign(new Campaign());
    }
}
