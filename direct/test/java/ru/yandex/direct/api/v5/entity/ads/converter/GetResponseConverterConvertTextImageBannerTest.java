package ru.yandex.direct.api.v5.entity.ads.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.TextImageAdGet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.NewStatusImageModerate;

import static org.assertj.core.api.Assertions.assertThat;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class GetResponseConverterConvertTextImageBannerTest {
    @Autowired
    public GetResponseConverter converter;

    @Test
    public void hrefIsConverted_not_null() {
        String href = "href";
        ImageBanner ad = buildTextImageAd().withHref(href);
        TextImageAdGet result = converter.convertTextImageBanner(ad);
        assertThat(result.getHref().getValue()).isEqualTo(href);
    }

    @Test
    public void hrefIsConverted_null() {
        // TODO: нужен ли такой тест?

        ImageBanner ad = buildTextImageAd();
        TextImageAdGet result = converter.convertTextImageBanner(ad);
        assertThat(result.getHref().getValue()).isNullOrEmpty();
    }

    @Test
    public void adImageHashIsConverted_not_null() {
        String hash = "adImageHash";
        ImageBanner ad = buildTextImageAd().withImageHash(hash);
        TextImageAdGet result = converter.convertTextImageBanner(ad);
        assertThat(result.getAdImageHash()).isEqualTo(hash);
    }

    //region utils
    private ImageBanner buildTextImageAd() {
        return new ImageBanner()
                .withId(0L)
                .withIsMobileImage(false)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NEW)
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true)
                .withImageStatusModerate(NewStatusImageModerate.NEW);
    }
    //endregion
}
