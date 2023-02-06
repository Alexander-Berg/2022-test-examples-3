package ru.yandex.direct.core.entity.banner.type.creative;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeType;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentCreativeTypeToBannerType;
import static ru.yandex.direct.core.entity.banner.type.creative.BannerWithCreativeConstraints.isCreativeTypeCorrespondTo;
import static ru.yandex.direct.core.entity.banner.type.creative.BannerWithCreativeUtils.getAllowedCreativeTypesByClass;

@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class BannerWithCreativeConstraintsIsCreativeTypeCorrespondToTest {

    @Test
    @junitparams.Parameters(method = "positiveCases")
    public void hasValidType_positive(Class<? extends Banner> bannerClass, CreativeType creativeType) {
        Creative creative = new Creative().withId(1L).withType(creativeType);
        var allowedTypes = getAllowedCreativeTypesByClass(bannerClass, null);
        Constraint<Long, Defect> constraint =
                isCreativeTypeCorrespondTo(allowedTypes, singletonMap(creative.getId(), creative));

        assertThat(constraint.apply(creative.getId()), nullValue());
    }

    @Test
    @junitparams.Parameters(method = "negativeCases")
    public void hasValidType_negative(Class<? extends Banner> bannerClass, CreativeType creativeType) {
        Creative creative = new Creative().withId(1L).withType(creativeType);
        var allowedTypes = getAllowedCreativeTypesByClass(bannerClass, null);
        Constraint<Long, Defect> constraint =
                isCreativeTypeCorrespondTo(allowedTypes, singletonMap(creative.getId(), creative));

        assertThat(constraint.apply(creative.getId()), is(inconsistentCreativeTypeToBannerType()));
    }

    Iterable<Object[]> positiveCases() {
        return asList(new Object[][]{
                {CpcVideoBanner.class, CreativeType.CPC_VIDEO_CREATIVE},

                {CpmBanner.class, CreativeType.CPM_VIDEO_CREATIVE},
                {CpmBanner.class, CreativeType.BANNERSTORAGE},
                {CpmBanner.class, CreativeType.HTML5_CREATIVE},
                {CpmBanner.class, CreativeType.CANVAS},

                {TextBanner.class, CreativeType.VIDEO_ADDITION_CREATIVE},
                {ImageBanner.class, CreativeType.CANVAS},
                {ImageBanner.class, CreativeType.HTML5_CREATIVE},
                {PerformanceBanner.class, CreativeType.PERFORMANCE},
                {MobileAppBanner.class, CreativeType.VIDEO_ADDITION_CREATIVE},
                {CpmOutdoorBanner.class, CreativeType.CPM_OUTDOOR_CREATIVE},
                {CpmOutdoorBanner.class, CreativeType.BANNERSTORAGE},
                {CpmIndoorBanner.class, CreativeType.CPM_INDOOR_CREATIVE},
                {CpmIndoorBanner.class, CreativeType.BANNERSTORAGE},
        });
    }

    Iterable<Object[]> negativeCases() {
        return asList(new Object[][]{
                {CpcVideoBanner.class, CreativeType.VIDEO_ADDITION_CREATIVE},
                {CpcVideoBanner.class, CreativeType.CPM_VIDEO_CREATIVE},
                {CpcVideoBanner.class, CreativeType.CPM_OUTDOOR_CREATIVE},

                {CpmBanner.class, CreativeType.VIDEO_ADDITION_CREATIVE},
                {CpmBanner.class, CreativeType.CPC_VIDEO_CREATIVE},
                {CpmBanner.class, CreativeType.CPM_OUTDOOR_CREATIVE},

                {CpmOutdoorBanner.class, CreativeType.VIDEO_ADDITION_CREATIVE},
                {CpmOutdoorBanner.class, CreativeType.CPC_VIDEO_CREATIVE},
                {CpmOutdoorBanner.class, CreativeType.CPM_VIDEO_CREATIVE},

                {CpmIndoorBanner.class, CreativeType.VIDEO_ADDITION_CREATIVE},
                {CpmIndoorBanner.class, CreativeType.CPC_VIDEO_CREATIVE},
                {CpmIndoorBanner.class, CreativeType.CPM_VIDEO_CREATIVE},
                {CpmIndoorBanner.class, CreativeType.CPM_OUTDOOR_CREATIVE},

                {TextBanner.class, CreativeType.CPC_VIDEO_CREATIVE},
                {TextBanner.class, CreativeType.CPM_VIDEO_CREATIVE},

                {MobileAppBanner.class, CreativeType.CANVAS},
                {MobileAppBanner.class, CreativeType.HTML5_CREATIVE},

                {PerformanceBanner.class, CreativeType.VIDEO_ADDITION_CREATIVE},
                {ImageBanner.class, CreativeType.CPM_AUDIO_CREATIVE},
                {ImageBanner.class, CreativeType.CPM_AUDIO_CREATIVE},

                {CpcVideoBanner.class, null},
                {CpmBanner.class, null},
                {TextBanner.class, null},
        });
    }
}
