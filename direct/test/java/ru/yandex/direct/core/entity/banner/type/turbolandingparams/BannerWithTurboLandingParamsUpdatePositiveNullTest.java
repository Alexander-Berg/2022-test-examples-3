package ru.yandex.direct.core.entity.banner.type.turbolandingparams;

import javax.annotation.Nullable;

import jdk.jfr.Description;
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingParamsUpdatePositiveNullTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTurboLanding> {

    @Test
    @Description("turboLandingParams был null и остался null")
    public void updateNull() {
        bannerInfo = createBanner(null);

        String newTitle = "newTitle 2324";
        ModelChanges<TextBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                        .process(newTitle, TextBanner.TITLE);
        prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(bannerInfo.getBannerId(), TextBanner.class);
        assertThat(actualBanner.getTurboLandingHrefParams(), equalTo(null));
        assertThat(actualBanner.getTitle(), equalTo(newTitle));
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
