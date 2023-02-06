package ru.yandex.market.ff4shops.api;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FfAsserts;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

/**
 * Тесты для {@link CommonErrorController}
 */
class HttpExceptionFunctionalTest extends FunctionalTest {

    private static final ImmutableSet<String> IGNORED_FIELDS = ImmutableSet.of("host", "timestamp");

    @Test
    void testNotFoundInXmlFormat() {
        String wrongPartnerUrl = UriComponentsBuilder
                .fromUriString("http://localhost")
                .port(randomServerPort)
                .path("/wrongPath")
                .toUriString();

        String result = FunctionalTestHelper.getForEntity(wrongPartnerUrl, FunctionalTestHelper.xmlHeaders()).getBody();

        //language=xml
        String expected = "" +
                "<root>\n"
                + "    <uniq/>\n"
                + "    <hash/>\n"
                + "    <requestState>\n"
                + "        <isError>true</isError>\n"
                + "        <errorCodes>\n"
                + "            <errorCode>\n"
                + "                <code>9400</code>\n"
                + "                <message>Not Found: /wrongPath</message>\n"
                + "                <description/>\n"
                + "            </errorCode>\n"
                + "        </errorCodes>\n"
                + "    </requestState>\n"
                + "    <response type=\"\"/>\n"
                + "</root>";

        FfAsserts.assertXmlEquals(expected, result);
    }

    @Test
    void testNotFoundInJsonFormat() {
        String wrongPartnerUrl = UriComponentsBuilder
                .fromUriString("http://localhost")
                .port(randomServerPort)
                .path("/partner")
                .path("/wrongPath")
                .toUriString();

        String result =
                FunctionalTestHelper.getForEntity(wrongPartnerUrl, FunctionalTestHelper.jsonHeaders()).getBody();

        //language=json
        String expected = "" +
                "{\n"
                + "  \"application\": \"ff4shops\",\n"
                + "  \"host\": \"host\",\n"
                + "  \"timestamp\": 1569934005425,\n"
                + "  \"result\": null,\n"
                + "  \"errors\": [\n"
                + "    {\n"
                + "      \"subCode\": \"NOT_FOUND\",\n"
                + "      \"message\": \"Not Found: /partner/wrongPath\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        FfAsserts.assertJsonEquals(expected, result, IGNORED_FIELDS);
    }

    @Test
    void testMediaNotSupported() {
        String partnerUrl = FF4ShopsUrlBuilder.getStocksDebugStatusUrl(randomServerPort, 100500);
        String result = FunctionalTestHelper.getForEntity(partnerUrl, FunctionalTestHelper.xmlHeaders()).getBody();

        //language=json
        String expected = "" +
                "{\n"
                + "  \"application\": \"ff4shops\",\n"
                + "  \"host\": \"host\",\n"
                + "  \"timestamp\": 1570106120117,\n"
                + "  \"result\": null,\n"
                + "  \"errors\": [\n"
                + "    {\n"
                + "      \"subCode\": \"UNSUPPORTED_MEDIA_TYPE\",\n"
                + "      \"message\": \"Media type for /partner/* methods must be application/json\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";

        FfAsserts.assertJsonEquals(expected, result, IGNORED_FIELDS);
    }

    @Test
    void testMediaNotSupportedXml() {
        String xmlUrl = FF4ShopsUrlBuilder.getReferenceUrl(randomServerPort, 11);
        String result = FunctionalTestHelper.postForEntity(xmlUrl, "", FunctionalTestHelper.jsonHeaders()).getBody();

        //language=xml
        String expected = "" +
                "<root>\n"
                + "    <uniq/>\n"
                + "    <hash/>\n"
                + "    <requestState>\n"
                + "        <isError>true</isError>\n"
                + "        <errorCodes>\n"
                + "            <errorCode>\n"
                + "                <code>9999</code>\n"
                + "                <message>Unsupported Media Type</message>\n"
                + "                <description/>\n"
                + "            </errorCode>\n"
                + "        </errorCodes>\n"
                + "    </requestState>\n"
                + "    <response/>\n"
                + "</root>\n";

        FfAsserts.assertXmlEquals(expected, result);
    }
}
