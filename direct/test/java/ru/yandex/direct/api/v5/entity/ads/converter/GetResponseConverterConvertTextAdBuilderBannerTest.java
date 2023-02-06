package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdBuilderAdGetItem;
import com.yandex.direct.api.v5.ads.TextAdBuilderAdGet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.creative.model.Creative;

import static org.assertj.core.api.Assertions.assertThat;

@Api5Test
@RunWith(SpringRunner.class)

@ParametersAreNonnullByDefault
public class GetResponseConverterConvertTextAdBuilderBannerTest {

    @Autowired
    public GetResponseConverter converter;

    @Test
    public void hrefIsConverted_not_null() {
        String href = "href";
        var ad = buildTextAdBuilderAd().withHref(href);
        AdsGetContainer adsGetContainer = getContainerBuilder(ad).withCreative(new Creative().withId(1L)).build();

        TextAdBuilderAdGet result = converter.convertTextAdBuilderBanner(adsGetContainer);
        assertThat(result.getHref().getValue()).isEqualTo(href);
    }

    @Test
    public void hrefIsConverted_null() {
        // TODO: нужен ли такой тест?

        AdsGetContainer adsGetContainer = getContainerBuilder().withCreative(new Creative().withId(1L)).build();

        TextAdBuilderAdGet result = converter.convertTextAdBuilderBanner(adsGetContainer);
        assertThat(result.getHref().getValue()).isNullOrEmpty();
    }

    @Test
    public void creativeIsConverted_only_id() {
        Long id = 1L;

        AdsGetContainer adsGetContainer = getContainerBuilder().withCreative(new Creative().withId(id)).build();

        TextAdBuilderAdGet result = converter.convertTextAdBuilderBanner(adsGetContainer);
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

        TextAdBuilderAdGet result = converter.convertTextAdBuilderBanner(adsGetContainer);
        assertThat(result.getCreative())
                .isEqualToComparingFieldByFieldRecursively(
                        new AdBuilderAdGetItem().withCreativeId(id).withPreviewUrl(previewUrl)
                                .withThumbnailUrl(thumbnailUrl));
    }

    //region utils
    private ImageBanner buildTextAdBuilderAd() {
        return new ImageBanner()
                .withId(0L)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NEW)
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true)
                .withCreativeId(1L)
                .withIsMobileImage(false);
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
        return getContainerBuilder(buildTextAdBuilderAd());
    }
    //endregion
}
