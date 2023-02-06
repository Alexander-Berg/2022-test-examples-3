package ru.yandex.direct.core.entity.banner.type.turbolandingparams;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboLandingParams.TURBO_LANDING_HREF_PARAMS;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingParamsAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void turboLandingParamsContainsInvalidEscapeSeq_Error() {
        String invalidHrefParams = "%zz";
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        var banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTurboLandingHrefParams(invalidHrefParams);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TURBO_LANDING_HREF_PARAMS)),
                invalidValue())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

}
