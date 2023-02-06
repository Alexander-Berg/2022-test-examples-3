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
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.noRightsToPixel;
import static ru.yandex.direct.core.entity.banner.type.pixels.BannerPixelsConstraints.validPermissionsForProvider;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.mailRuTop100PixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;

@RunWith(Parameterized.class)
public class BannerPixelConstraintsPermissionsForProviderTest {
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
        assertThat(validPermissionsForProvider(accessibleProviders).apply(arg), is(expectedDefect));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "пиксель Я.Аудиторий не учитывается в валидации",
                        yaAudiencePixelUrl(),
                        emptySet(),
                        null,
                },
                {
                        "разрешён пиксель",
                        adfoxPixelUrl(),
                        Set.of(PixelProvider.ADFOX, PixelProvider.MEDIASCOPE),
                        null,
                },
                {
                        "запрещён пиксель",
                        adfoxPixelUrl(),
                        Set.of(PixelProvider.MEDIASCOPE),
                        noRightsToPixel(adfoxPixelUrl(), Set.of(PixelProvider.MEDIASCOPE)),
                },
                {
                        "разрешён пиксель",
                        mailRuTop100PixelUrl(),
                        Set.of(PixelProvider.MAIL_RU_TOP_100),
                        null,
                },
        });
    }
}
