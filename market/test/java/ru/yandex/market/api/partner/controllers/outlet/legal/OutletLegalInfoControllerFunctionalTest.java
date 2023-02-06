package ru.yandex.market.api.partner.controllers.outlet.legal;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.xml.sax.SAXException;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.mbi.util.MbiAsserts;

import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;

/**
 * Тестирование взаимодействия с HTTP интерфейсом контроллера поставок {@link OutletLegalInfoController}.
 *
 * @author stani on 08.08.18.
 */
class OutletLegalInfoControllerFunctionalTest extends FunctionalTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static Stream<Arguments> typesArgs() {
        return Stream.of(
                Arguments.of(OrganizationType.OAO),
                Arguments.of(OrganizationType.ZAO),
                Arguments.of(OrganizationType.OTHER),
                Arguments.of(OrganizationType.AO)
        );
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testOkPutLegalInfoJson() throws IOException {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(legalUrl(10774L, 101L),
                HttpMethod.PUT, Format.JSON, IOUtils.toString(
                        this.getClass().getResourceAsStream("put-outlet-legal-info.json"), Charset.defaultCharset()));
        JsonTestUtil.assertEquals("{\"status\":\"OK\"}", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testOkPutLegalInfoXml() throws IOException {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(legalUrl(10774L, 101L),
                HttpMethod.PUT, Format.XML, IOUtils.toString(
                        this.getClass().getResourceAsStream("put-outlet-legal-info.xml"), Charset.defaultCharset()));
        MbiAsserts.assertXmlEquals("<response><status>OK</status></response>", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testOkPutInsertLegalInfoJson() throws IOException {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(legalUrl(10774L, 103L),
                HttpMethod.PUT, Format.JSON, IOUtils.toString(
                        this.getClass().getResourceAsStream("put-outlet-legal-info.json"), Charset.defaultCharset()));
        JsonTestUtil.assertEquals("{\"status\":\"OK\"}", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testInvalidPutLegalInfoJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(legalUrl(10774L, 101L),
                        HttpMethod.PUT, Format.JSON, IOUtils.toString(
                                this.getClass().getResourceAsStream("put-outlet-legal-info-invalid.json"),
                                Charset.defaultCharset()))
        );
        MatcherAssert.assertThat(httpClientErrorException, hasErrorCode(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testPutLegalInfoForOutletNotFoundJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(legalUrl(10774L, 404L),
                        HttpMethod.PUT, Format.JSON, IOUtils.toString(
                                this.getClass().getResourceAsStream("put-outlet-legal-info.json"), Charset.defaultCharset()))
        );
        MatcherAssert.assertThat(httpClientErrorException, hasErrorCode(HttpStatus.NOT_FOUND));
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testInvalidRegNumPutLegalInfoJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(legalUrl(10774L, 101L),
                        HttpMethod.PUT, Format.JSON, IOUtils.toString(
                                this.getClass().getResourceAsStream("put-outlet-legal-info-ivalid-regnum.json"), Charset.defaultCharset()))
        );
        MatcherAssert.assertThat(httpClientErrorException, hasErrorCode(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testOkGetLegalInfoJson() throws IOException {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(legalUrl(10774L, 101L),
                HttpMethod.GET, Format.JSON);
        JsonTestUtil.assertEquals(IOUtils.toString(
                this.getClass().getResourceAsStream("get-legal-info-response.json"), Charset.defaultCharset()),
                response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testOkGetLegalInfoXml() throws IOException {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(legalUrl(10774L, 101L),
                HttpMethod.GET, Format.XML);
        MbiAsserts.assertXmlEquals(IOUtils.toString(
                this.getClass().getResourceAsStream("get-legal-info-response.xml"), Charset.defaultCharset()),
                response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testGetLegalInfoForOutletNotFoundJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        legalUrl(10774L, 404L), HttpMethod.GET, Format.JSON)
        );
        MatcherAssert.assertThat(httpClientErrorException, hasErrorCode(HttpStatus.NOT_FOUND));
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv", after = "outletLegalDelete.after.csv")
    void testOkDeleteLegalInfoJson() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(legalUrl(10774L, 101L),
                HttpMethod.DELETE, Format.JSON);
        JsonTestUtil.assertEquals("{\"status\":\"OK\"}", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv", after = "outletLegalDelete.after.csv")
    void testOkDeleteLegalInfoXml() {
        ResponseEntity<String> response = FunctionalTestHelper.makeRequest(legalUrl(10774L, 101L),
                HttpMethod.DELETE, Format.XML);
        MbiAsserts.assertXmlEquals("<response><status>OK</status></response>", response.getBody());
    }

    @Test
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testDeleteLegalInfoForOutletNotFoundJson() {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(
                        legalUrl(10774L, 404L), HttpMethod.DELETE, Format.JSON)
        );
        MatcherAssert.assertThat(httpClientErrorException, hasErrorCode(HttpStatus.NOT_FOUND));
    }

    @ParameterizedTest()
    @MethodSource("typesArgs")
    @DbUnitDataSet(before = "outletLegal.before.csv")
    void testTypes(OrganizationType type) throws SAXException, IOException, XpathException {
        String url = legalUrl(10774L, 101L);
        String body = StringTestUtil.getString(this.getClass(),
                String.format("outlet-legal-info.%s.xml", type.name().toLowerCase()));
        FunctionalTestHelper.makeRequest(url, HttpMethod.PUT, Format.XML, body, String.class);
        String response = Objects.requireNonNull(
                FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML, String.class).getBody());
        XMLAssert.assertXpathsEqual("/legal-info", body, "/response/result/legal-info", response);
        Integer actualType = jdbcTemplate.queryForObject("select organization_type " +
                        "  from shops_web.outlet_legal_info " +
                        "  where outlet_id = 101",
                Integer.class);
        Assertions.assertEquals(type, HasId.getById(OrganizationType.class, actualType));
    }

    private String legalUrl(long campaignId, long outletId) {
        return String.format("%s/campaigns/%d/outlets/%d/legal", urlBasePrefix, campaignId, outletId);
    }
}
