package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.function.Consumer;

import com.yandex.direct.api.v5.ads.AdTypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static ru.yandex.direct.api.v5.entity.ads.converter.TypeAndSubtypesConverter.convertType;

@RunWith(Parameterized.class)
public class TypeConverterTest {

    @Parameter
    public String desc;

    @Parameter(1)
    public Banner banner;

    @Parameter(2)
    public Consumer<Banner> checkExpectations;

    @Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"dynamic", new DynamicBanner(), isCorrectType(AdTypeEnum.DYNAMIC_TEXT_AD, AdGroupType.DYNAMIC)},
                {"image ad", new ImageBanner(), isCorrectType(AdTypeEnum.IMAGE_AD, AdGroupType.BASE)},
                {"image ad for mobile_content adGroup", new ImageBanner(),
                        isCorrectType(AdTypeEnum.IMAGE_AD, AdGroupType.MOBILE_CONTENT)},
                {"mobile content", new MobileAppBanner(), isCorrectType(AdTypeEnum.MOBILE_APP_AD,
                        AdGroupType.MOBILE_CONTENT)},
                {"text", new TextBanner(), isCorrectType(AdTypeEnum.TEXT_AD, AdGroupType.BASE)},
                {"cpc_video", new CpcVideoBanner(), isCorrectType(AdTypeEnum.CPC_VIDEO_AD, AdGroupType.BASE)},
                {"cpc_video for mobile_content adGroup", new CpcVideoBanner(),
                        isCorrectType(AdTypeEnum.CPC_VIDEO_AD, AdGroupType.MOBILE_CONTENT)},
                {"cpm_banner", new CpmBanner(), isCorrectType(AdTypeEnum.CPM_BANNER_AD, AdGroupType.CPM_BANNER)},
                {"cpm_banner", new CpmBanner(), isCorrectType(AdTypeEnum.CPM_VIDEO_AD, AdGroupType.CPM_VIDEO)},
                {"performance", new PerformanceBanner(), isCorrectType(AdTypeEnum.SMART_AD,
                        AdGroupType.PERFORMANCE)},
        };
    }

    private static Consumer<Banner> isCorrectType(AdTypeEnum expectedType, AdGroupType adGroupType) {
        return banner -> assertThat(convertType(banner, adGroupType, null)).isEqualTo(expectedType);
    }

    @Test
    public void test() {
        checkExpectations.accept(banner);
    }
}
