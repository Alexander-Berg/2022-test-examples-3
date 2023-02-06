package ru.yandex.direct.core.entity.banner.type.turbolanding;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding.TURBO_LANDING_ID;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.turboPageNotFound;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingUpdateNegativeTest
        extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTurboLanding> {

    @Test
    public void invalidTurboLandingForTextBanner() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner();

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(),
                TextBanner.class)
                .process(-1L, TURBO_LANDING_ID);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TURBO_LANDING_ID)), turboPageNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
