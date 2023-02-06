package ru.yandex.direct.core.entity.banner.type.turbolandingparams;

import java.util.Collection;

import javax.annotation.Nullable;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLandingParams;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingParams;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithTurboLandingParamsUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTurboLanding> {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public String oldTurboLandingParams;

    @Parameterized.Parameter(2)
    public String newTurboLandingParams;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "null -> abc",
                        null,
                        "abc"
                },
                {
                        "abc -> null",
                        "abc",
                        null
                },
                {
                        "abc -> 0",
                        "abc",
                        "0"
                },
                {
                        "'' -> abc",
                        "",
                        "abc"
                },
                {
                        "'' -> null",
                        "",
                        null
                },
                {
                        "null -> ''",
                        null,
                        ""
                },
                {
                        "abc -> ''",
                        "abc",
                        ""
                }
        });
    }

    @Test
    public void test() {
        bannerInfo = createBanner(oldTurboLandingParams);

        ModelChanges<TextBanner> modelChanges =
                new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                        .process(newTurboLandingParams, BannerWithTurboLandingParams.TURBO_LANDING_HREF_PARAMS);
        prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(bannerInfo.getBannerId(), TextBanner.class);
        assertThat(actualBanner.getTurboLandingHrefParams(), equalTo(newTurboLandingParams));
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
