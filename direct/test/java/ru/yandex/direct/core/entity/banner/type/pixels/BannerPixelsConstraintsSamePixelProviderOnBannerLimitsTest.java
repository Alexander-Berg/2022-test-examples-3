package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.equalPixelProvidersLimitExceeded;
import static ru.yandex.direct.core.entity.banner.type.pixels.BannerPixelsConstraints.maxNumberOfSamePixelProviders;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl2;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl2;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl2;

@RunWith(Parameterized.class)
public class BannerPixelsConstraintsSamePixelProviderOnBannerLimitsTest {
    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public List<String> arg;

    @Parameterized.Parameter(2)
    public Set<String> accessibleProviderNames;

    @Parameterized.Parameter(3)
    public Defect expectedDefect;

    @Test
    public void testParametrized() {
        assertThat(maxNumberOfSamePixelProviders(accessibleProviderNames).apply(arg), is(expectedDefect));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // positive
                {
                        "два Adfox пикселя, но прав на adfox нет",
                        List.of(adfoxPixelUrl(), adfoxPixelUrl2()),
                        emptySet(),
                        null,
                },
                // negative
                {
                        "два Adfox пикселя",
                        List.of(adfoxPixelUrl(), adfoxPixelUrl2()),
                        StreamEx.of(PixelProvider.values()).map(it -> it.getProviderName()).toSet(),
                        equalPixelProvidersLimitExceeded(List.of("Adfox")),
                },
                {
                        "несколько превышений",
                        List.of(yaAudiencePixelUrl2(), adriverPixelUrl(), adfoxPixelUrl(),
                                adfoxPixelUrl2(), adriverPixelUrl2()),
                        StreamEx.of(PixelProvider.values()).map(it -> it.getProviderName()).toSet(),
                        equalPixelProvidersLimitExceeded(List.of("Adriver", "Adfox")),
                },
        });
    }
}
