package ru.yandex.market.api.partner.controllers.region;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;

@DbUnitDataSet(before = "regions.before.csv")
class RegionControllerTest extends FunctionalTest {

    /**
     * Разные знаки припенания знак тире (—),  короткое тире (–) и дефис (-).
     */
    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("Республика Северная Осетия – Алания"),
                Arguments.of("Республика Северная Осетия — Алания"),
                Arguments.of("Республика Северная Осетия - Алания")
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void testOkSupplierDropshipJson(String searchUrl) {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                regionUrl(searchUrl),
                HttpMethod.GET, Format.JSON, String.class);

        String expected = "{\"regions\":[" +
                "{\"id\":11021,\"name\":\"Республика Северная Осетия — Алания\",\"type\":\"REPUBLIC\"," +
                "\"parent\":{\"id\":102444,\"name\":\"Северо-Кавказский федеральный округ\",\"type\":\"COUNTRY_DISTRICT\"," +
                "\"parent\":{\"id\":225,\"name\":\"Россия\",\"type\":\"COUNTRY\"}}}]}";
        JsonTestUtil.assertEquals(expected, response.getBody());
    }

    private String regionUrl(String name) {
        return String.format("%s/regions?name=%s", urlBasePrefix, name);
    }
}
