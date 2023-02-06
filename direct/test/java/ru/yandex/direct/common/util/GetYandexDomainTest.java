package ru.yandex.direct.common.util;

import java.util.Collection;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.common.enums.YandexDomain;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class GetYandexDomainTest {

    @Parameterized.Parameter
    public String host;

    @Parameterized.Parameter(value = 1)
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public Optional<YandexDomain> expectedResult;

    @Parameterized.Parameters(name = "Host: {0}; expectedDomain: {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {null, Optional.empty()},
                {"ya.ru", Optional.empty()},
                {"direct.yandex.ru", Optional.of(YandexDomain.RU)},
                {"direct.yandex.com", Optional.of(YandexDomain.COM)},
                {"direct.yandex.com.tr", Optional.of(YandexDomain.TR)},
                {"13945.beta6.direct.yandex.ru", Optional.of(YandexDomain.RU)},
        });
    }

    @Test
    public void checkGetYandexDomain() {
        Optional<YandexDomain> result = HostUtils.getYandexDomain(host);

        assertThat(result).isEqualTo(expectedResult);
    }
}
