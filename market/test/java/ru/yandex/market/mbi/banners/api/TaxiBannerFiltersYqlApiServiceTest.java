package ru.yandex.market.mbi.banners.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.banners.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Функциональные тесты на {@link TaxiBannerFiltersYqlApiService}
 */
class TaxiBannerFiltersYqlApiServiceTest extends FunctionalTest {

    private static final String PATH_TO_EXPECTED_SQL = "ru/yandex/market/mbi/banners/api/" +
            "TaxiBannerFiltersYqlApiServiceTestSql/";

    @Test
    @DisplayName("Без фильтров")
    void testWithoutFilters() throws Exception {
        String requestName = "TaxiBannerFiltersYqlApiServiceTest.without.request.json";
        String expectedFileName = "TaxiBannerFiltersYqlApiServiceTest.without.response.json";
        String pathToQuery = PATH_TO_EXPECTED_SQL + "TaxiBannerFiltersYqlApiServiceTest.withoutFilters.sql";
        ResponseEntity<String> response = postGenerateFilterYql(requestName);
        assertResponse(response, expectedFileName, pathToQuery);
    }

    @Test
    @DisplayName("Все фильтры")
    void testAllFilters() throws Exception {
        String requestName = "TaxiBannerFiltersYqlApiServiceTest.all.request.json";
        String expectedFileName = "TaxiBannerFiltersYqlApiServiceTest.all.response.json";
        String pathToQuery = PATH_TO_EXPECTED_SQL + "TaxiBannerFiltersYqlApiServiceTest.all.filters.sql";
        ResponseEntity<String> response = postGenerateFilterYql(requestName);
        assertResponse(response, expectedFileName, pathToQuery);
    }

    @Test
    @DisplayName("Фильтр тип партнера")
    void testSupplierTypesFilters() throws Exception {
        String requestName = "TaxiBannerFiltersYqlApiServiceTest.supplierTypes.request.json";
        String expectedFileName = "TaxiBannerFiltersYqlApiServiceTest.supplierTypes.response.json";
        String pathToQuery = PATH_TO_EXPECTED_SQL + "TaxiBannerFiltersYqlApiServiceTest.filter.partner.type.sql";
        ResponseEntity<String> response = postGenerateFilterYql(requestName);
        assertResponse(response, expectedFileName, pathToQuery);
    }


    @Test
    @DisplayName("Фильтр живость")
    void testAlivenessFilter() throws Exception {
        String requestName = "TaxiBannerFiltersYqlApiServiceTest.aliveness.request.json";
        String expectedFileName = "TaxiBannerFiltersYqlApiServiceTest.aliveness.response.json";
        String pathToQuery = PATH_TO_EXPECTED_SQL + "TaxiBannerFiltersYqlApiServiceTest.aliveness.sql";
        ResponseEntity<String> response = postGenerateFilterYql(requestName);
        assertResponse(response, expectedFileName, pathToQuery);
    }

    @Test
    @DisplayName("Фильтр живость, не живые")
    void testAlivenessUnavailableFilter() throws Exception {
        String requestName = "TaxiBannerFiltersYqlApiServiceTest.aliveness.false.request.json";
        String expectedFileName = "TaxiBannerFiltersYqlApiServiceTest.aliveness.false.response.json";
        String pathToQuery = PATH_TO_EXPECTED_SQL + "TaxiBannerFiltersYqlApiServiceTest.aliveness.not.alive.sql";
        ResponseEntity<String> response = postGenerateFilterYql(requestName);
        assertResponse(response, expectedFileName, pathToQuery);
    }

    @Test
    @DisplayName("Фильтр Добавить бизнесы к id партнёров")
    void testUnionPartnerBusinessesFilters() throws Exception {
        String requestName = "TaxiBannerFiltersYqlApiServiceTest.unionPartnerBusinesses.request.json";
        String expectedFileName = "TaxiBannerFiltersYqlApiServiceTest.unionPartnerBusinesses.response.json";
        String pathToQuery = PATH_TO_EXPECTED_SQL + "TaxiBannerFiltersYqlApiServiceTest.add.bussiness.ids.sql";
        ResponseEntity<String> response = postGenerateFilterYql(requestName);
        assertResponse(response, expectedFileName, pathToQuery);
    }

    @Test
    @DisplayName("Фильтр noexpress")
    void testNoExpressFilter() throws Exception {
        String requestName = "TaxiBannerFiltersYqlApiServiceTest.noexpress.request.json";
        String expectedFileName = "TaxiBannerFiltersYqlApiServiceTest.noexpress.response.json";
        String pathToQuery = PATH_TO_EXPECTED_SQL + "TaxiBannerFiltersYqlApiServiceTest.filter.no.express.sql";
        ResponseEntity<String> response = postGenerateFilterYql(requestName);
        assertResponse(response, expectedFileName, pathToQuery);
    }

    @Test
    @DisplayName("Фильтр express")
    void testExpressFilter() throws Exception {
        String requestName = "TaxiBannerFiltersYqlApiServiceTest.express.request.json";
        String expectedFileName = "TaxiBannerFiltersYqlApiServiceTest.express.response.json";
        String pathToQuery = PATH_TO_EXPECTED_SQL + "TaxiBannerFiltersYqlApiServiceTest.filter.express.sql";
        ResponseEntity<String> response = postGenerateFilterYql(requestName);
        assertResponse(response, expectedFileName, pathToQuery);
    }

    @Test
    @DisplayName("Фильтр id введенные руками")
    void testManualIds() throws Exception {
        String requestName = "TaxiBannerFiltersYqlApiServiceTest.manual.ids.request.json";
        String expectedFileName = "TaxiBannerFiltersYqlApiServiceTest.manual.ids.response.json";
        String pathToQuery = PATH_TO_EXPECTED_SQL + "TaxiBannerFiltersYqlApiServiceTest.filter.manual.ids.sql";
        ResponseEntity<String> response = postGenerateFilterYql(requestName);
        assertResponse(response, expectedFileName, pathToQuery);
    }

    private ResponseEntity<String> postGenerateFilterYql(String requestFile) {
        String request = getString(getClass(), requestFile);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        return FunctionalTestHelper.post(baseUrl + "/taxiBannerFilterYql",
                new HttpEntity<>(request, headers));

    }

    private void assertResponse(
            ResponseEntity<String> response,
            String expectedFile,
            String pathToQuery
    ) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertValidHumanReadableSqlQueries(response, pathToQuery);
        jsonAssertFile(expectedFile, response.getBody());
    }

    private void assertValidHumanReadableSqlQueries(
            ResponseEntity<String> response, String pathToQuery
    ) throws Exception {
        String expected = readExpectedQuery(pathToQuery);
        String got = extractQueryFromResponse(response);
        Assertions.assertEquals(expected, got);
    }

    private String readExpectedQuery(String pathToQuery) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream is = classLoader.getResourceAsStream(pathToQuery);
        if (is == null) {
            throw new IOException("Input stream is null");
        }
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String extractQueryFromResponse(ResponseEntity<String> response) {
        var nameAndValue = response.getBody().split(":");  // отделили название поля yql от занчения;
        var result = nameAndValue[1];  // взяли значение поля yql
        result = result.replace("}", "");
        result = result.substring(1, result.length() - 1);  // убрали внутрение кавычки
        return StringEscapeUtils.unescapeJava(result);
    }

    private void jsonAssertFile(String expectedFile, String actual) {
        jsonAssert(getString(getClass(), expectedFile), actual);
    }

    private void jsonAssert(String expected, String actual) {
        try {
            JSONAssert.assertEquals(
                    expected,
                    actual,
                    false
            );
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
    }
}
