package ru.yandex.direct.core.entity.banner.type.turbogallery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboGallery.TURBO_GALLERY_HREF;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboGalleryAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    public static final String INVALID_TURBO_GALLERY_HREF = "yandex.ru";

    @Test
    public void addInvalidTurboGallery() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();

        TextBanner banner = fullTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTurboGalleryHref(INVALID_TURBO_GALLERY_HREF);
        var vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(
                path(field(TURBO_GALLERY_HREF)), invalidHref())
        ));
    }

}
