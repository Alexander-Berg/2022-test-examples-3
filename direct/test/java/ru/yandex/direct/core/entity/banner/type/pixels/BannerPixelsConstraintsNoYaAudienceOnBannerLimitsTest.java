package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.pixelNumberYaAudienceLimitExceeded;
import static ru.yandex.direct.core.entity.banner.type.pixels.BannerPixelsConstraints.maxNumberOfYaAudiencePixels;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl2;

public class BannerPixelsConstraintsNoYaAudienceOnBannerLimitsTest extends BannerPixelsConstraintsBaseTest {
    public BannerPixelsConstraintsNoYaAudienceOnBannerLimitsTest() {
        super(maxNumberOfYaAudiencePixels());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // positive
                {
                        "Один пиксель Я.Аудиторий на баннере",
                        ImmutableList.of(yaAudiencePixelUrl2()),
                        null,
                },
                // negative
                {
                        "Превышение лимита числа пикселей Я.Аудиторий на баннере",
                        ImmutableList.of(yaAudiencePixelUrl(), yaAudiencePixelUrl2()),
                        pixelNumberYaAudienceLimitExceeded(),
                },
        });
    }
}
