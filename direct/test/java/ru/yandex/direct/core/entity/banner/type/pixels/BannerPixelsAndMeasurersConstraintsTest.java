package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidPixelWithMeasurer;
import static ru.yandex.direct.core.entity.banner.type.pixels.BannerPixelsAndMeasurersConstraints.checkPixelWithMeasurer;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.tnsPixelUrl;

@RunWith(Parameterized.class)
public class BannerPixelsAndMeasurersConstraintsTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "MEDIASCOPE совместим с ads.adfox.ru",
                        adfoxPixelUrl(),
                        null,
                },
                {
                        "строки null в этом констрейне не проверяются",
                        null,
                        null,
                },
                {
                        "формат строки в этом констрейне не проверяется",
                        "sdkfjsjfghsdf",
                        null,
                },
                {
                        "MEDIASCOPE не совместим с tns-counter",
                        tnsPixelUrl(),
                        invalidPixelWithMeasurer(tnsPixelUrl(), BannerMeasurerSystem.MEDIASCOPE.name()),
                },
        });
    }

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String pixel;

    @Parameterized.Parameter(2)
    public Defect expectedDefect;

    @Test
    public void test() {
        List<BannerMeasurer> bannerMeasurers = List.of(new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.MEDIASCOPE)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false));
        assertThat(checkPixelWithMeasurer(bannerMeasurers,
                Set.of(PixelProvider.ADFOX, PixelProvider.MEDIASCOPE, PixelProvider.TNS))
                .apply(pixel), is(expectedDefect));
    }
}
