package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.MobileAppImageAdGet;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.NewStatusImageModerate;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetResponseConverter.convertMobileAppImageBanner;

@ParametersAreNonnullByDefault
public class GetResponseConverterConvertMobileAppImageBannerTest {

    @Test
    public void trackingUrlIsConverted_not_null() {
        String href = "href";
        var ad = buildMobileAppImageAd().withHref(href);
        MobileAppImageAdGet result = convertMobileAppImageBanner(ad);
        assertThat(result.getTrackingUrl().getValue()).isEqualTo(href);
    }

    @Test
    public void trackingUrlIsConverted_null() {
        var ad = buildMobileAppImageAd();
        MobileAppImageAdGet result = convertMobileAppImageBanner(ad);
        assertThat(result.getTrackingUrl().isNil()).isTrue();
    }

    @Test
    public void adImageHashIsConverted_not_null() {
        String hash = "adImageHash";
        var ad = buildMobileAppImageAd().withImageHash(hash);
        MobileAppImageAdGet result = convertMobileAppImageBanner(ad);
        assertThat(result.getAdImageHash()).isEqualTo(hash);
    }

    @Test
    public void adImageHashIsConverted_null() {
        // TODO: нужен ли такой тест?

        var ad = buildMobileAppImageAd().withImageHash(null);
        MobileAppImageAdGet result = convertMobileAppImageBanner(ad);
        assertThat(result.getAdImageHash()).isNullOrEmpty();
    }

    //region utils
    private ImageBanner buildMobileAppImageAd() {
        return new ImageBanner()
                .withId(0L)
                .withIsMobileImage(true)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NEW)
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true)
                .withImageHash("2")
                .withImageStatusModerate(NewStatusImageModerate.NEW);
    }
    //endregion
}
