package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdTypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetRequestConverter.convertTypes;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetRequestConverterConvertTypesTest {

    @Parameterized.Parameter
    public List<AdTypeEnum> types;

    @Parameterized.Parameter(1)
    public List<BannersBannerType> expectedTypes;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {emptyList(), emptyList()},
                {singletonList(AdTypeEnum.DYNAMIC_TEXT_AD), singletonList(BannersBannerType.dynamic)},
                {singletonList(AdTypeEnum.IMAGE_AD), singletonList(BannersBannerType.image_ad)},
                {singletonList(AdTypeEnum.MOBILE_APP_AD), List.of(BannersBannerType.mobile_content)},
                {singletonList(AdTypeEnum.TEXT_AD), singletonList(BannersBannerType.text)},
                {singletonList(AdTypeEnum.CPM_BANNER_AD), singletonList(BannersBannerType.cpm_banner)},
                {singletonList(AdTypeEnum.CPC_VIDEO_AD), singletonList(BannersBannerType.cpc_video)},
                {singletonList(AdTypeEnum.SMART_AD), asList(BannersBannerType.performance,
                        BannersBannerType.performance_main)},
                {singletonList(AdTypeEnum.CONTENT_PROMOTION_VIDEO_AD),
                        singletonList(BannersBannerType.content_promotion)},
                {singletonList(AdTypeEnum.CONTENT_PROMOTION_COLLECTION_AD),
                        singletonList(BannersBannerType.content_promotion)},
                {singletonList(AdTypeEnum.CONTENT_PROMOTION_SERVICE_AD),
                        singletonList(BannersBannerType.content_promotion)},
                {singletonList(AdTypeEnum.CONTENT_PROMOTION_EDA_AD),
                        singletonList(BannersBannerType.content_promotion)},
                {asList(AdTypeEnum.values()),
                        asList(BannersBannerType.dynamic, BannersBannerType.image_ad,
                                BannersBannerType.mobile_content, BannersBannerType.text,
                                BannersBannerType.cpm_banner, BannersBannerType.cpc_video,
                                BannersBannerType.performance, BannersBannerType.performance_main,
                                BannersBannerType.content_promotion)},
                // direct enumeration 'cause API supports no all types of banners
        };
    }

    @Test
    public void test() {
        assertThat(convertTypes(types)).containsExactlyInAnyOrder(expectedTypes.toArray(new BannersBannerType[0]));
    }

}
