package ru.yandex.direct.core.entity.banner.type.displayhref;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithDisplayHref;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Path;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref.DISPLAY_HREF;
import static ru.yandex.direct.core.entity.banner.model.BannerWithHrefAndDisplayHref.HREF;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidDisplayHrefUsage;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.restrictedCharsInDisplayHref;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithDisplayHrefUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithDisplayHref> {

    private static final Path PATH = path(field(DISPLAY_HREF));
    private static final String VALID_DISPLAY_HREF = "/display-href";

    @Test
    public void invalidDisplayHref() {
        bannerInfo = createBanner(VALID_DISPLAY_HREF);
        Long bannerId = bannerInfo.getBannerId();

        ModelChanges<TextBanner> changes = new ModelChanges<>(bannerId, TextBanner.class)
                .process("display.href!", DISPLAY_HREF);

        var validationResult = prepareAndApplyInvalid(changes);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(PATH, restrictedCharsInDisplayHref(".!"))
        ));
    }

    @Test
    public void displayHrefIsSetWhenHrefIsNull() {
        bannerInfo = createBanner(VALID_DISPLAY_HREF);
        Long bannerId = bannerInfo.getBannerId();

        ModelChanges<TextBanner> changes = new ModelChanges<>(bannerId, TextBanner.class)
                .process(null, HREF);

        var validationResult = prepareAndApplyInvalid(changes);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(PATH, invalidDisplayHrefUsage())
        ));
    }

    private TextBannerInfo createBanner(String displayHref) {
        return steps.bannerSteps().createBanner(activeTextBanner().withDisplayHref(displayHref));
    }
}
