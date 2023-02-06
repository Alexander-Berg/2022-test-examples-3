package ru.yandex.direct.core.entity.banner.type.logos;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithLogo;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.feature.FeatureName;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImageFormat;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners.clientPerformanceMainBanner;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class BannerWithLogoExtendedAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();

        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.SMART_NO_CREATIVES, true);
    }

    public static Object[] formats() {
        return new Object[][]{
                {"square logo image", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.LOGO)
                        .withWidth(30L)
                        .withHeight(30L)},
                {"non-square logo image", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.LOGO)
                        .withWidth(30L)
                        .withHeight(15L)},
                {"ultra-wide logo image", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.LOGO)
                        .withWidth(800L)
                        .withHeight(40L)},
                {"square small image", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.SMALL)
                        .withWidth(300L)
                        .withHeight(300L)},
                {"non-square small image", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.SMALL)
                        .withWidth(450L)
                        .withHeight(300L)},
                {"square regular image", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.REGULAR)
                        .withWidth(450L)
                        .withHeight(450L)},
                {"non-square regular image", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.REGULAR)
                        .withWidth(600L)
                        .withHeight(450L)},
        };
    }

    @Test
    @Parameters(method = "formats")
    @TestCaseName("{0}")
    public void test(@SuppressWarnings("unused") String description, BannerImageFormat bannerImageFormat) {
        String imageHash = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo(), bannerImageFormat)
                .getImageHash();

        PerformanceBannerMain banner = clientPerformanceMainBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash(imageHash);

        Long id = prepareAndApplyValid(banner);

        BannerWithLogo actualBanner = getBanner(id);
        assertThat(actualBanner.getLogoImageHash()).isEqualTo(imageHash);
        assertThat(actualBanner.getLogoStatusModerate()).isEqualTo(BannerLogoStatusModerate.READY);
    }
}
