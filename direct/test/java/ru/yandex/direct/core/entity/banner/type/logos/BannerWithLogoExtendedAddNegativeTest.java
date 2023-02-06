package ru.yandex.direct.core.entity.banner.type.logos;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerWithLogo;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageSizeInvalid;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndImageType;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImageFormat;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners.clientPerformanceMainBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class BannerWithLogoExtendedAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {
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
                {"too small logo", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.LOGO)
                        .withWidth(1L)
                        .withHeight(1L), imageSizeInvalid()},
                {"too big logo", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.WIDE)
                        .withWidth(1920L)
                        .withHeight(1080L), imageSizeInvalid()},
                {"invalid image type", defaultBannerImageFormat()
                        .withImageType(BannerImageFormat.ImageType.IMAGE_AD)
                        .withWidth(2560L)
                        .withHeight(1600L), inconsistentStateBannerTypeAndImageType()},
        };
    }

    @Test
    @Parameters(method = "formats")
    @TestCaseName("{0}")
    public void test(@SuppressWarnings("unused") String description,
                     BannerImageFormat bannerImageFormat, Defect<Void> defect) {
        String imageHash = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo(), bannerImageFormat)
                .getImageHash();

        PerformanceBannerMain banner = clientPerformanceMainBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash(imageHash);

        ValidationResult<?, Defect> validationResult = prepareAndApplyInvalid(banner);

        Assert.assertThat(validationResult, hasDefectWithDefinition(validationError(
                path(field(BannerWithLogo.LOGO_IMAGE_HASH.name())), defect)));
    }
}
