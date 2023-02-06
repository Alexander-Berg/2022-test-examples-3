package ru.yandex.direct.core.entity.banner.type.titleextension;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTitleExtension.TITLE_EXTENSION;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxTextLengthWithoutTemplateMarker;
import static ru.yandex.direct.core.entity.banner.type.titleextension.BannerWithTitleExtensionConstants.MAX_LENGTH_TITLE_EXTENSION;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTitleExtensionAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String TITLE_EXTENSION_STR = "long title extension long title extension";

    @Test
    public void addInvalidTitleExtension() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitleExtension(TITLE_EXTENSION_STR);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TITLE_EXTENSION)),
                maxTextLengthWithoutTemplateMarker(MAX_LENGTH_TITLE_EXTENSION))));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
