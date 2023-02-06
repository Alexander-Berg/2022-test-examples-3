package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.Collection;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.noRightsToAudiencePixel;
import static ru.yandex.direct.core.entity.banner.type.pixels.BannerPixelsConstraints.validPermissionsForYaAudienceProvider;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;

@RunWith(Parameterized.class)
public class BannerPixelConstraintsPermissionsYaAudienceForProviderTest {
    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public String arg;

    @Parameterized.Parameter(2)
    public Set<PixelProvider> accessibleProviders;

    @Parameterized.Parameter(3)
    public Defect expectedDefect;

    @Test
    public void testParametrized() {
        assertThat(validPermissionsForYaAudienceProvider(accessibleProviders).apply(arg), is(expectedDefect));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "в валидации учитываются только пиксели Я.Аудиторий",
                        adfoxPixelUrl(),
                        emptySet(),
                        null,
                },
                {
                        "разрешён пиксель",
                        yaAudiencePixelUrl(),
                        Set.of(PixelProvider.YANDEXAUDIENCE, PixelProvider.MEDIASCOPE),
                        null,
                },
                {
                        "запрещён пиксель",
                        yaAudiencePixelUrl(),
                        Set.of(PixelProvider.MEDIASCOPE),
                        noRightsToAudiencePixel(yaAudiencePixelUrl()),
                },
        });
    }
}
