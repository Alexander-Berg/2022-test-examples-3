package ru.yandex.direct.core.entity.banner.type.body;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithFixedBody.BODY;
import static ru.yandex.direct.core.validation.defects.RightsDefects.forbiddenToChange;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithFixedBodyUpdateTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Test
    public void updateBody() {
        bannerInfo = steps.performanceBannerSteps().createDefaultPerformanceBanner();

        ModelChanges<PerformanceBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), PerformanceBanner.class)
                        .process("Banner Body", BODY);

        ValidationResult<?, Defect> result = prepareAndApplyInvalid(modelChanges);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(BODY)),
                forbiddenToChange()))));
    }
}
