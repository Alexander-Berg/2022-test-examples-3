package ru.yandex.direct.core.entity.banner.type.title;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithFixedTitle;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.context.i18n.LocaleContextHolder.setLocale;
import static ru.yandex.direct.core.entity.banner.model.BannerWithFixedTitle.TITLE;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultMcBannerImageFormat;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestNewDynamicBanners.clientDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestNewMcBanners.clientMcBanner;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceBanners.clientPerformanceBanner;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithFixedTitleAddTest extends BannerAdGroupInfoAddOperationTestBase {

    @Before
    public void before() {
        setLocale(Locale.forLanguageTag("ru"));
    }

    @Test
    public void dynamicBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup();
        DynamicBanner clientBanner = clientDynamicBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long bannerId = prepareAndApplyValid(clientBanner);
        BannerWithFixedTitle banner = getBanner(bannerId, BannerWithFixedTitle.class);
        assertThat(banner.getTitle()).isEqualTo("{Динамический заголовок}");
    }

    @Test
    public void performanceBanner() {
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(
                defaultPerformanceCreative(null, null),
                adGroupInfo.getClientInfo());
        PerformanceBanner clientBanner = clientPerformanceBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long bannerId = prepareAndApplyValid(clientBanner);
        BannerWithFixedTitle banner = getBanner(bannerId, BannerWithFixedTitle.class);
        assertThat(banner.getTitle()).isEqualTo("{Перфоманс заголовок}");
    }

    @Test
    public void mcBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup();
        String imageHash = steps.bannerSteps()
                .createBannerImageFormat(adGroupInfo.getClientInfo(), defaultMcBannerImageFormat(null))
                .getImageHash();
        McBanner clientBanner = clientMcBanner(imageHash)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long bannerId = prepareAndApplyValid(clientBanner);
        BannerWithFixedTitle banner = getBanner(bannerId, BannerWithFixedTitle.class);
        assertThat(banner.getTitle()).isEqualTo("picture banner title");
    }

    @Test
    public void clientBannerWithTitle() {
        adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup();
        DynamicBanner clientBanner = clientDynamicBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitle("Banner Title");

        ValidationResult<?, Defect> result = prepareAndApplyInvalid(clientBanner);
        assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(
                path(field(TITLE)),
                isNull()))));
    }
}
