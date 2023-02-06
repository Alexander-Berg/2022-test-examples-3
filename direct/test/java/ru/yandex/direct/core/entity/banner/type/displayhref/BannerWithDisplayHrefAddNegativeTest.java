package ru.yandex.direct.core.entity.banner.type.displayhref;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref.DISPLAY_HREF;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.duplicateSpecCharsInDisplayHref;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidDisplayHrefUsage;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithDisplayHrefAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {
    private static final Path PATH = path(field(DISPLAY_HREF));

    @Test
    public void invalidDisplayHref() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        TextBanner banner = fullTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withDisplayHref("//display--href");

        ValidationResult<?, Defect> validationResult = prepareAndApplyInvalid(banner);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(PATH, duplicateSpecCharsInDisplayHref())));
    }

    @Test
    public void displayHrefIsSetWhenHrefIsNull() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        TextBanner banner = fullTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref(null)
                .withDisplayHref("/display-href");

        ValidationResult<?, Defect> validationResult = prepareAndApplyInvalid(banner);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(PATH, invalidDisplayHrefUsage())));
    }

}
