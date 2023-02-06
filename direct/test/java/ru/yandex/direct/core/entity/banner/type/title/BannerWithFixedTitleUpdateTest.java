package ru.yandex.direct.core.entity.banner.type.title;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithFixedTitle.TITLE;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithFixedTitleUpdateTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Test
    public void updateTitle() {
        bannerInfo = steps.dynamicBannerSteps().createDefaultDynamicBanner();

        ModelChanges<DynamicBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), DynamicBanner.class)
                        .process("Banner Title", TITLE);

        ValidationResult<?, Defect> result = prepareAndApplyInvalid(modelChanges);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(TITLE)),
                forbiddenToChange()))));
    }

}
