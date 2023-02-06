package ru.yandex.direct.core.entity.banner.type.turbolanding;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithHref.HREF;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding.TURBO_LANDING_ID;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredButEmptyHrefOrTurbolandingId;
import static ru.yandex.direct.core.testing.data.TestNewCpcVideoBanners.clientCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithHrefAndTurboLandingAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void cpcVideoBannerWithoutTurboAndHref() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultCpcVideoCreative(adGroupInfo.getClientInfo(), steps.creativeSteps().getNextCreativeId());
        var banner = clientCpcVideoBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref(null)
                .withTurboLandingId(null);
        var vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(HREF)), notNull())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test // менять синхронно с тестом из BannersAddOperationCpmGeoproductTest
    public void cpmGeoproductBothTurboAndHrefAreNull() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmGeoproductAdGroup();
        Long creativeId = steps.creativeSteps()
                .addDefaultHtml5CreativeForGeoproduct(adGroupInfo.getClientInfo()).getCreativeId();

        var banner = clientCpmBanner(creativeId)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref(null)
                .withTurboLandingId(null);
        var vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TURBO_LANDING_ID)), notNull())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test  // менять синхронно с тестом из BannersAddOperationTest
    public void cpmBannerBothTurboAndHrefAreNull() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        Long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(), creativeId);

        var banner = clientCpmBanner(creativeId)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref(null)
                .withTurboLandingId(null);
        var vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(), requiredButEmptyHrefOrTurbolandingId())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

}
