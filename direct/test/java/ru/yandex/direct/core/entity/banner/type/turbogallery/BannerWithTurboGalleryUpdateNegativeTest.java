package ru.yandex.direct.core.entity.banner.type.turbogallery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboGalleryHref;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Path;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboGallery.TURBO_GALLERY_HREF;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboGalleryUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTurboGalleryHref> {

    private static final Path PATH = path(field(TURBO_GALLERY_HREF));
    private static final String VALID_HREF = "http://yandex.ru/turbo/text=lpc/42";
    public static final String INVALID_HREF = "href!";

    @Test
    public void updateInvalidTurboGallery() {
        bannerInfo = createBanner(VALID_HREF);
        Long bannerId = bannerInfo.getBannerId();

        ModelChanges<TextBanner> changes = new ModelChanges<>(bannerId, TextBanner.class)
                .process(INVALID_HREF, TURBO_GALLERY_HREF);

        var validationResult = prepareAndApplyInvalid(changes);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(PATH, invalidHref())
        ));
    }

    private TextBannerInfo createBanner(String turboGalleryHref) {
        return steps.bannerSteps().createBanner(activeTextBanner().withTurboGalleryHref(turboGalleryHref));
    }
}
