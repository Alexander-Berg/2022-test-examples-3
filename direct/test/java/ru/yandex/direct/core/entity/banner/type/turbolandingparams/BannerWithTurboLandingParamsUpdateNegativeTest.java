package ru.yandex.direct.core.entity.banner.type.turbolandingparams;

import javax.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingParams;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboLandingParams.TURBO_LANDING_HREF_PARAMS;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingParamsUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTurboLanding> {

    @Test
    public void containsInvalidEscapeSeq() {
        bannerInfo = createBanner("abc");

        String newTurboLandingParams = "%zz";
        ModelChanges<TextBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                        .process(newTurboLandingParams, TURBO_LANDING_HREF_PARAMS);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TURBO_LANDING_HREF_PARAMS)),
                invalidValue())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    private TextBannerInfo createBanner(@Nullable String turboLandingParams) {
        return steps.bannerSteps().createActiveTextBanner(
                activeTextBanner()
                        .withTurboLandingParams(turboLandingParams == null ?
                                null :
                                new OldBannerTurboLandingParams().withHrefParams(turboLandingParams)
                        ));
    }

}
