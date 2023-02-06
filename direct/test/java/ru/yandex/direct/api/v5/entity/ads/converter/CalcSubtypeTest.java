package ru.yandex.direct.api.v5.entity.ads.converter;

import com.yandex.direct.api.v5.ads.AdSubtypeEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.TypeAndSubtypesConverter.calcSubtype;

@RunWith(Parameterized.class)
public class CalcSubtypeTest {

    @Parameterized.Parameter
    public BannerWithSystemFields ad;

    @Parameterized.Parameter(1)
    public AdSubtypeEnum expectedSubtype;

    @Parameterized.Parameters(name = "{1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {new ImageBanner().withCreativeId(1L).withIsMobileImage(true),
                        AdSubtypeEnum.MOBILE_APP_AD_BUILDER_AD},
                {new ImageBanner().withCreativeId(1L).withIsMobileImage(false), AdSubtypeEnum.TEXT_AD_BUILDER_AD},
                {new ImageBanner().withImageHash("2").withIsMobileImage(true), AdSubtypeEnum.MOBILE_APP_IMAGE_AD},
                {new ImageBanner().withImageHash("2").withIsMobileImage(false),
                        AdSubtypeEnum.TEXT_IMAGE_AD},
                {new TextBanner(), AdSubtypeEnum.NONE},
                {new CpcVideoBanner().withIsMobileVideo(true), AdSubtypeEnum.MOBILE_APP_CPC_VIDEO_AD_BUILDER_AD},
                {new CpcVideoBanner().withIsMobileVideo(false), AdSubtypeEnum.NONE},
        };
    }

    @Test
    public void test() {
        assertThat(calcSubtype(ad)).isEqualTo(expectedSubtype);
    }
}
