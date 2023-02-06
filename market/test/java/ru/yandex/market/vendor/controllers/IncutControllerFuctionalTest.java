package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToJsonPattern;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/IncutControllerFuctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/IncutControllerFuctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class IncutControllerFuctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final WireMockServer advIncutMock;
    private final WireMockServer reportMock;
    private final Clock clock;

    @Autowired
    public IncutControllerFuctionalTest(WireMockServer advIncutMock,
                                        WireMockServer reportMock,
                                        Clock clock) {
        this.advIncutMock = advIncutMock;
        this.reportMock = reportMock;
        this.clock = clock;
    }

    @Test
    void testGetExecutionExceptionFromAdvIncut() {

        advIncutMock.stubFor(get("/api/v1/incuts/6541?vendorId=19708&datasourceId=28195&uid=1186962236")
                .willReturn(aResponse().withStatus(400).withBody(getStringResource(
                        "/testGetExecutionExceptionFromAdvIncut/incutResponse.json"))));

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            FunctionalTestHelper.get(
                    baseUrl + "/vendors/19708/modelbids/incuts/6541?uid=1186962236");
        });

        String expected = getStringResource("/testGetExecutionExceptionFromAdvIncut/expected.json");
        String actual = exception.getResponseBodyAsString();

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void testGetSomeExecutionExceptionFromAdvIncut() {

        advIncutMock.stubFor(get("/api/v1/incuts/6541?vendorId=19708&datasourceId=28195&uid=1186962236")
                .willReturn(aResponse().withStatus(400).withBody(getStringResource(
                        "/testGetSomeExecutionExceptionFromAdvIncut/incutResponse.json"))));

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            FunctionalTestHelper.get(
                    baseUrl + "/vendors/19708/modelbids/incuts/6541?uid=1186962236");
        });

        String expected = getStringResource("/testGetSomeExecutionExceptionFromAdvIncut/expected.json");
        String actual = exception.getResponseBodyAsString();

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void testGetIncut() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, Month.DECEMBER,
                        17, 15, 8, 0)));

        advIncutMock.stubFor(get("/api/v1/incuts/6541?vendorId=19708&datasourceId=28195&uid=1186962236")
                .willReturn(aResponse().withBody(getStringResource("/testGetIncut/incutResponse.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=1000&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testGetIncut/models.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=0&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testGetIncut/models.json"))));

        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/19708/modelbids/incuts/6541?uid=1186962236");

        String expected = getStringResource("/testGetIncut/expected.json");

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void testGetIncutWithNotFoundModelsInReport() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, Month.DECEMBER,
                        17, 15, 8, 0)));

        advIncutMock.stubFor(get("/api/v1/incuts/6541?vendorId=19708&datasourceId=28195&uid=1186962236")
                .willReturn(aResponse().withBody(getStringResource("/testGetIncutWithNotFoundModelsInReport/incutResponse.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=1000&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testGetIncutWithNotFoundModelsInReport/models.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=0&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testGetIncutWithNotFoundModelsInReport/models.json"))));

        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/19708/modelbids/incuts/6541?uid=1186962236");

        String expected = getStringResource("/testGetIncutWithNotFoundModelsInReport/expected.json");

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void testGetIncutList() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, Month.DECEMBER,
                        17, 15, 8, 0)));

        advIncutMock.stubFor(WireMock.get("/api/v1/incuts/list?vendorId=19708&datasourceId=28195&uid=1186962236" +
                        "&incutName=incut&page=1&pageSize=2")
                .willReturn(aResponse().withBody(getStringResource("/testGetIncutList/incutResponse.json"))));

        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/19708/modelbids/incuts/list?uid=1186962236&page=1&pageSize=2" +
                        "&incutName=incut");

        String expected = getStringResource("/testGetIncutList/expected.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void createIncutList() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, Month.DECEMBER,
                        17, 15, 8, 0)));

        advIncutMock.stubFor(WireMock.post("/api/v1/incuts?vendorId=19708&datasourceId=28195&uid=1186962236" +
                        "&transition=DRAFT_SELF")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testCreateIncutList/incutRequestBody.json"),
                        true, false))
                .willReturn(aResponse().withBody(getStringResource("/testCreateIncutList/incutResponse.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=0&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testCreateIncutList/models.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=1000&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testCreateIncutList/models.json"))));

        String actual = FunctionalTestHelper.post(
                baseUrl + "/vendors/19708/modelbids/incuts?uid=1186962236&transition=DRAFT_SELF",
                getStringResource("/testCreateIncutList/requestBody.json")
        );

        String expected = getStringResource("/testCreateIncutList/expected.json");

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void testPutIncut() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, Month.DECEMBER,
                        17, 15, 8, 0)));

        advIncutMock.stubFor(WireMock.put("/api/v1/incuts/1?vendorId=19708&datasourceId=28195&uid=1186962236" +
                        "&transition=DRAFT_SELF")
                .withRequestBody(new EqualToJsonPattern(getStringResource("/testPutIncut/requestBody.json"), true,
                        false))
                .willReturn(aResponse().withBody(getStringResource("/testPutIncut/incutResponse.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=0&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testPutIncut/models.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=1000&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testPutIncut/models.json"))));

        String actual = FunctionalTestHelper.put(
                baseUrl + "/vendors/19708/modelbids/incuts/1?uid=1186962236&transition=DRAFT_SELF",
                getStringResource("/testPutIncut/requestBody.json")
        );

        String expected = getStringResource("/testPutIncut/expected.json");

        JsonAssert.assertJsonEquals(expected, actual);
    }

    @Test
    void testPutIncutTransit() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, Month.DECEMBER,
                        17, 15, 8, 0)));

        advIncutMock.stubFor(WireMock.put("/api/v1/incuts/1/transit?transition=DRAFT_SELF&vendorId=19708&datasourceId" +
                        "=28195&uid=1186962236")
                .willReturn(aResponse().withBody(getStringResource("/testPutIncutTransit/incutResponse.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=0&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testPutIncutTransit/models.json"))));

        reportMock.stubFor(get(urlEqualTo("/?place=brand_products&bsformat=2&vendor_id=101&pp=7&show-msku=1&entities" +
                "=product&numdoc=1000&page=1&hyperid=1429703292%2C1448810179"))
                .willReturn(aResponse().withBody(getStringResource("/testPutIncutTransit/models.json"))));

        String actual = FunctionalTestHelper.put(
                baseUrl + "/vendors/19708/modelbids/incuts/1/transit?uid=1186962236&transition=DRAFT_SELF", null);

        String expected = getStringResource("/testPutIncutTransit/expected.json");

        JsonAssert.assertJsonEquals(expected, actual);
    }
}
