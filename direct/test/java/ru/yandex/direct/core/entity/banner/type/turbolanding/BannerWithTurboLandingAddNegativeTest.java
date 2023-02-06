package ru.yandex.direct.core.entity.banner.type.turbolanding;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId.AD_GROUP_ID;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding.TURBO_LANDING_ID;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adGroupNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.turboPageNotFound;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void invalidTurboLandingIdForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTurboLandingId(-1L);
        var vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TURBO_LANDING_ID)), turboPageNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidTurboLandingIdWithInvalidAdGroupIdForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        var banner = clientTextBanner()
                .withAdGroupId(-1L)
                .withTurboLandingId(-1L);
        var vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(AD_GROUP_ID)),
                adGroupNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
