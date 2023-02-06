package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidPixelFormat;
import static ru.yandex.direct.core.entity.banner.type.pixels.BannerPixelsConstraints.validPixelFormat;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adlooxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.invalidUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.mailRuTop100PixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.sizmecPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;

@RunWith(Parameterized.class)
public class BannerPixelConstraintsFormatTest {
    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String arg;

    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    @Test
    public void testParametrized() {
        assertThat(validPixelFormat().apply(arg), is(expectedDefect));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // positive
                {
                        "пиксель аудита Я.Аудиторий",
                        yaAudiencePixelUrl(),
                        null,
                },
                {
                        "пиксель аудита Mail.ru top-100",
                        mailRuTop100PixelUrl(),
                        null,
                },
                {
                        "пиксель adloox",
                        adlooxPixelUrl(),
                        null,
                },
                {
                        "пиксель sizmec",
                        sizmecPixelUrl(),
                        null,
                },
                // negative
                {
                        "невалидный пиксель аудита",
                        invalidUrl(),
                        invalidPixelFormat(),
                },
                {
                        "some unknown url",
                        "some unknown url",
                        invalidPixelFormat(),
                },
        });
    }
}
