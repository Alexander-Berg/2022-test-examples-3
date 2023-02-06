package ru.yandex.direct.core.entity.banner.type.turbogallery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboGallery.TURBO_GALLERY_HREF;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboGalleryAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    private static final String VALID_TURBO_GALLERY_HREF = "https://yandex.ru/turbo?42";

    @Test
    public void addValidTurboGallery() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();

        TextBanner banner = fullTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTurboGalleryHref(VALID_TURBO_GALLERY_HREF);
        Long id = prepareAndApplyValid(banner);

        assertThat(getBanner(id), allOf(
                hasProperty(TURBO_GALLERY_HREF.name(), equalTo(VALID_TURBO_GALLERY_HREF))
        ));
    }
}
