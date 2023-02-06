package ru.yandex.market.api.partner.controllers.outlet.license;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiAsserts;

/**
 * Тесты для {@link OutletLicenseController}.
 *
 * @author serenitas
 */
@DbUnitDataSet(before = "OutletLicenseControllerFunctionalTest.before.csv")
class OutletLicenseControllerFunctionalTest extends FunctionalTest {

    private static final String COMMON_LICENSE_URL = "/campaigns/10774/outlets/licenses";


    //тесты на получение
    @Test
    @DisplayName("Тест на получение лицензий в JSON по id аутлетов")
    void testGetByOutletsJson() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                buildUrl("outletIds", Arrays.asList(1L, 2L, 3L, 4L)), HttpMethod.GET, Format.JSON);
        JsonTestUtil.assertEquals(response, getClass(), "getLicensesResponse.json");
    }

    @Test
    @DisplayName("Тест на получение лицензий в XML по id аутлетов")
    void testGetByOutletsXml() throws IOException {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                buildUrl("outletIds", Arrays.asList(1L, 2L, 3L, 4L)), HttpMethod.GET, Format.XML);
        final String expected = IOUtils.toString(getClass().getResourceAsStream("getLicensesResponse.xml"), StandardCharsets.UTF_8);
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    @DisplayName("Тест на получение лицензий в JSON по id лицензий")
    void testGetByLicensesJson() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                buildUrl("ids", Arrays.asList(11L, 12L, 13L, 14L)), HttpMethod.GET, Format.JSON);
        JsonTestUtil.assertEquals(response, getClass(), "getLicensesResponse.json");
    }

    @Test
    @DisplayName("Тест на получение лицензий в XML по id лицензий")
    void testGetByLicensesXml() throws IOException {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                buildUrl("ids", Arrays.asList(11L, 12L, 13L, 14L)), HttpMethod.GET, Format.XML);
        final String expected = IOUtils.toString(getClass().getResourceAsStream("getLicensesResponse.xml"), StandardCharsets.UTF_8);
        MbiAsserts.assertXmlEquals(expected, response.getBody());
    }

    @Test
    @DisplayName("Тест на получение пустого списка лицензий для аутлетов без лицензий")
    void testGetLicensesIfNoLicensesExist() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                buildUrl("outletIds", Collections.singletonList(7L)), HttpMethod.GET, Format.XML);
        MbiAsserts.assertXmlEquals("<response><status>OK</status><result><licenses></licenses></result></response>", response.getBody());
    }

    @Test
    @DisplayName("Тест на ошибку в случае, если не передан ни список id аутлетов, ни список id лицензий")
    void testNoOutletsRequested() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        String.format("%s%s?", urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.GET, Format.JSON));
        MatcherAssert.assertThat(httpClientErrorException, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertEquals("{\"status\":\"ERROR\",\"errors\":[{\"code\":\"MISSING_PARAM\",\"message\":\"Required one of next parameters: outletIds, ids\"}]}",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на ошибку в случае, если передан и список id аутлетов, и список id лицензий")
    void testRequestedDifferentIds() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        String.format("%s%s?ids=%s&outletIds=%s", urlBasePrefix, COMMON_LICENSE_URL, "11,12", "11,12"), HttpMethod.GET, Format.JSON));
        MatcherAssert.assertThat(httpClientErrorException, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        JsonTestUtil.assertEquals("{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"Required one of next parameters: outletIds, ids\"}]}",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на невозможность получить лицензии для чужого аутлета")
    void testGetLicensesForNotOwnOutlet() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        buildUrl("outletIds", Collections.singletonList(10L)), HttpMethod.GET, Format.JSON));
        MatcherAssert.assertThat(httpClientErrorException, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND));
        JsonTestUtil.assertEquals("{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"Outlets not found: 10\"}]}",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на невозможность получить лицензии для несуществующего аутлета")
    void testGetLicensesIfNoOutletsExist() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        buildUrl("outletIds", Collections.singletonList(54321L)), HttpMethod.GET, Format.JSON));
        MatcherAssert.assertThat(httpClientErrorException, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND));
        JsonTestUtil.assertEquals("{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"Outlets not found: 54321\"}]}",
                httpClientErrorException.getResponseBodyAsString());
    }


    //тесты на создание/обновление
    @Test
    @DisplayName("Тест на создание лицензий")
    @DbUnitDataSet(after = "OutletLicenseControllerFunctionalTest.create.after.csv")
    void testCreateLicenses() {
        final String request =
                "<licenses>" +
                        "<license outlet-id=\"7\" number=\"newserialnumber1\" license-type=\"ALCOHOL\" date-of-issue=\"2018-01-07T17:00:00+03:00\" date-of-expiry=\"2099-01-07T17:00:00+03:00\"/>" +
                        "<license outlet-id=\"8\" number=\"newserialnumber2\" license-type=\"ALCOHOL\" date-of-issue=\"2018-02-07T17:00:00+03:00\" date-of-expiry=\"2099-02-07T17:00:00+03:00\"/>" +
                        "</licenses>";
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(String.format("%s%s",
                urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, request);
        MbiAsserts.assertXmlEquals("<response><status>OK</status></response>", response.getBody());
    }

    @Test
    @DisplayName("Тест на обновление лицензий")
    @DbUnitDataSet(after = "OutletLicenseControllerFunctionalTest.update.after.csv")
    void testUpdateLicenses() {
        final String request =
                "<licenses>" +
                        "<license id=\"11\" outlet-id=\"1\" number=\"newserialnumber1\" license-type=\"ALCOHOL\" date-of-issue=\"2018-01-07T17:00:00+03:00\" date-of-expiry=\"2099-01-07T17:00:00+03:00\"/>" +
                        "<license id=\"12\" outlet-id=\"2\" number=\"newserialnumber2\" license-type=\"ALCOHOL\" date-of-issue=\"2018-02-07T17:00:00+03:00\" date-of-expiry=\"2099-02-07T17:00:00+03:00\"/>" +
                        "</licenses>";
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(String.format("%s%s",
                urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, request);
        MbiAsserts.assertXmlEquals("<response><status>OK</status></response>", response.getBody());
    }

    @Test
    @DisplayName("Тест на создание и обновление лицензий в одном запросе")
    @DbUnitDataSet(after = "OutletLicenseControllerFunctionalTest.create.and.update.after.csv")
    void testCreateAndUpdateLicenses() {
        final String request =
                "<licenses>" +
                        "<license id=\"11\" outlet-id=\"1\" number=\"newserialnumber1\" license-type=\"ALCOHOL\" date-of-issue=\"2018-01-07T17:00:00+03:00\" date-of-expiry=\"2099-01-07T17:00:00+03:00\"/>" +
                        "<license outlet-id=\"7\" number=\"newserialnumber2\" license-type=\"ALCOHOL\" date-of-issue=\"2018-02-07T17:00:00+03:00\" date-of-expiry=\"2099-02-07T17:00:00+03:00\"/>" +
                        "</licenses>";
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(String.format("%s%s",
                urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, request);
        MbiAsserts.assertXmlEquals("<response><status>OK</status></response>", response.getBody());
    }

    @Test
    @DisplayName("Тест на невозможность создать лицензии для чужого аутлета")
    void testCreateLicensesForNotOwnOutlet() {
        final String request =
                "<licenses>" +
                        "<license outlet-id=\"7\" number=\"newserialnumber1\" license-type=\"ALCOHOL\" date-of-issue=\"2018-01-07T17:00:00+03:00\" date-of-expiry=\"2099-01-07T17:00:00+03:00\"/>" +
                        "<license outlet-id=\"11\" number=\"newserialnumber2\" license-type=\"ALCOHOL\" date-of-issue=\"2018-02-07T17:00:00+03:00\" date-of-expiry=\"2099-02-07T17:00:00+03:00\"/>" +
                        "</licenses>";
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(String.format("%s%s",
                        urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, request));
        MbiAsserts.assertXmlEquals("<response><status>ERROR</status><errors><error code=\"NOT_FOUND\" message=\"Outlets not found: 11\"/></errors></response>",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на невозможность обновить лицензии для чужого аутлета")
    void testUpdateLicensesForNotOwnOutlet() {
        final String request =
                "<licenses>" +
                        "<license outlet-id=\"7\" number=\"newserialnumber1\" license-type=\"ALCOHOL\" date-of-issue=\"2018-01-07T17:00:00+03:00\" date-of-expiry=\"2099-01-07T17:00:00+03:00\"/>" +
                        "<license id=\"110\" outlet-id=\"10\" number=\"newserialnumber2\" license-type=\"ALCOHOL\" date-of-issue=\"2018-02-07T17:00:00+03:00\" date-of-expiry=\"2099-02-07T17:00:00+03:00\"/>" +
                        "</licenses>";
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(String.format("%s%s",
                        urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, request));
        MbiAsserts.assertXmlEquals("<response><status>ERROR</status><errors><error code=\"NOT_FOUND\" message=\"Outlets not found: 10\"/></errors></response>",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на невозможность добавить еще одну лицензию того же типа")
    void testCheckOfOneLicenseForType() {
        final String request =
                "<licenses>" +
                        "<license outlet-id=\"1\" number=\"newserialnumber1\" license-type=\"ALCOHOL\" date-of-issue=\"2018-01-07T17:00:00+03:00\" date-of-expiry=\"2099-01-07T17:00:00+03:00\"/>" +
                        "<license outlet-id=\"8\" number=\"newserialnumber2\" license-type=\"ALCOHOL\" date-of-issue=\"2018-02-07T17:00:00+03:00\" date-of-expiry=\"2099-02-07T17:00:00+03:00\"/>" +
                        "</licenses>";
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(String.format("%s%s",
                        urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, request));
        MbiAsserts.assertXmlEquals("<response><status>ERROR</status><errors><error code=\"ITEM_DUPLICATE\" message=\"Found more than one license of type ALCOHOL for outlet with id 1\"/></errors></response>",
                httpClientErrorException.getResponseBodyAsString());
    }

    //тесты на валидацию
    @Test
    @DisplayName("Тест на валидацию полей: нет лицензий")
    @DbUnitDataSet(after = "OutletLicenseControllerFunctionalTest.validate.after.csv")
    void testValidateLicenseFieldsNoLicenses() {
        final String requestWithoutLicenses = "<licenses></licenses>";
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(String.format("%s%s",
                        urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, requestWithoutLicenses));
        MbiAsserts.assertXmlEquals("<response><status>ERROR</status><errors><error code=\"MISSING_PARAM\" message=\"Licenses should not be empty\"/></errors></response>",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на валидацию полей: номер лицензии null")
    @DbUnitDataSet(after = "OutletLicenseControllerFunctionalTest.validate.after.csv")
    void testValidateLicenseFieldsNullNumber() {
        final String requestWithNullNumber =
                "<licenses>" +
                        "<license outlet-id=\"7\" license-type=\"ALCOHOL\" date-of-issue=\"2018-01-07T17:00:00+03:00\" date-of-expiry=\"2099-01-07T17:00:00+03:00\"/>" +
                        "<license outlet-id=\"8\" number=\"newserialnumber2\" license-type=\"ALCOHOL\" date-of-issue=\"2018-02-07T17:00:00+03:00\" date-of-expiry=\"2099-02-07T17:00:00+03:00\"/>" +
                        "</licenses>";
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(String.format("%s%s",
                        urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, requestWithNullNumber));
        MbiAsserts.assertXmlEquals("<response><status>ERROR</status><errors><error code=\"CONSTRAINT_VIOLATION\" message=\"License number cannot be blank\"/></errors></response>",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на валидацию полей: пустой номер лицензии")
    @DbUnitDataSet(after = "OutletLicenseControllerFunctionalTest.validate.after.csv")
    void testValidateLicenseFieldsEmptyNumber() {
        final String requestWithEmptyNumber =
                "<licenses>" +
                        "<license outlet-id=\"7\" number=\"newserialnumber1\" license-type=\"ALCOHOL\" date-of-issue=\"2018-01-07T17:00:00+03:00\" date-of-expiry=\"2099-01-07T17:00:00+03:00\"/>" +
                        "<license outlet-id=\"8\" number=\"\" license-type=\"ALCOHOL\" date-of-issue=\"2018-02-07T17:00:00+03:00\" date-of-expiry=\"2099-02-07T17:00:00+03:00\"/>" +
                        "</licenses>";
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(String.format("%s%s",
                        urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, requestWithEmptyNumber));
        MbiAsserts.assertXmlEquals("<response><status>ERROR</status><errors><error code=\"CONSTRAINT_VIOLATION\" message=\"License number cannot be blank\"/></errors></response>",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на валидацию полей: дата выдачи позже даты окончания срока действия")
    @DbUnitDataSet(after = "OutletLicenseControllerFunctionalTest.validate.after.csv")
    void testValidateLicenseFieldsInvalidDates() {
        final String requestWithInvalidDates =
                "<licenses>" +
                        "<license outlet-id=\"7\" number=\"newserialnumber1\" license-type=\"ALCOHOL\" date-of-issue=\"2019-01-07T17:00:00+03:00\" date-of-expiry=\"2018-01-07T17:00:00+03:00\"/>" +
                        "<license outlet-id=\"8\" number=\"newserialnumber2\" license-type=\"ALCOHOL\" date-of-issue=\"2018-02-07T17:00:00+03:00\" date-of-expiry=\"2099-02-07T17:00:00+03:00\"/>" +
                        "</licenses>";
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(String.format("%s%s",
                        urlBasePrefix, COMMON_LICENSE_URL), HttpMethod.POST, Format.XML, requestWithInvalidDates));
        MbiAsserts.assertXmlEquals("<response><status>ERROR</status><errors><error code=\"CONSTRAINT_VIOLATION\" message=\"Date of issue should be before date of expiry\"/></errors></response>",
                httpClientErrorException.getResponseBodyAsString());
    }


    //тесты на удаление
    @Test
    @DisplayName("Тест на удаление лицензии точки в JSON")
    @DbUnitDataSet(after = "OutletLicenseControllerFunctionalTest.delete.after.csv")
    void testDeleteJson() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                buildUrl("ids", Arrays.asList(11L, 12L)), HttpMethod.DELETE, Format.JSON);
        JsonTestUtil.assertEquals("{\"status\":\"OK\"}", Objects.requireNonNull(response.getBody()));
    }

    @Test
    @DisplayName("Тест на удаление лицензии точки в XML")
    @DbUnitDataSet(after = "OutletLicenseControllerFunctionalTest.delete.after.csv")
    void testDeleteXml() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                buildUrl("ids", Arrays.asList(11L, 12L)), HttpMethod.DELETE, Format.XML);
        MbiAsserts.assertXmlEquals("<response><status>OK</status></response>", response.getBody());
    }

    @Test
    @DisplayName("Тест на невозможность удалить лицензии, которых нет")
    void testDeleteNonExistingLicenses() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(buildUrl("ids", Collections.singletonList(128L)), HttpMethod.DELETE, Format.JSON));
        MatcherAssert.assertThat(httpClientErrorException, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND));
        JsonTestUtil.assertEquals("{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"Licenses not found\"}]}",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на невозможность удалить лицензии, если не существует хотя бы одна из списка")
    void testDeleteLicensesWithNonExisting() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(buildUrl("ids", Arrays.asList(15L, 16L, 369L, 14L)), HttpMethod.DELETE, Format.JSON));
        MatcherAssert.assertThat(httpClientErrorException, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND));
        JsonTestUtil.assertEquals("{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"Licenses with ids 369 not found\"}]}",
                httpClientErrorException.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Тест на невозможность удалить лицензии, если нет доступа к какой-либо из точек продаж этих лицензий")
    void testDeleteLicensesWithoutAccess() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(buildUrl("ids", Arrays.asList(15L, 16L, 110L)), HttpMethod.DELETE, Format.JSON));
        MatcherAssert.assertThat(httpClientErrorException, HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND));
        JsonTestUtil.assertEquals("{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"Licenses with ids 110 not found\"}]}",
                httpClientErrorException.getResponseBodyAsString());
    }

    private String buildUrl(String param, Collection<Long> ids) {
        return String.format("%s%s?%s=%s",
                urlBasePrefix, COMMON_LICENSE_URL, param, StringUtils.collectionToCommaDelimitedString(ids));
    }
}
