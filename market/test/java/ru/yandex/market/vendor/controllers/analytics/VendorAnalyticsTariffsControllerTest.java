package ru.yandex.market.vendor.controllers.analytics;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.security.Role;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;


@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/analytics/VendorAnalyticsTariffsControllerTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/analytics/VendorAnalyticsTariffsControllerTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class VendorAnalyticsTariffsControllerTest extends AbstractVendorPartnerFunctionalTest {

    private final Clock clock;
    private final WireMockServer csBillingApiMock;

    @Autowired
    public VendorAnalyticsTariffsControllerTest(WireMockServer csBillingApiMock, Clock clock) {
        this.csBillingApiMock = csBillingApiMock;
        this.clock = clock;
    }

    @BeforeEach
    void beforeEach() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Получение тарифов, выбранных вендором")
    void getVendorTariffs() {
        setVendorUserRoles(Collections.singleton(Role.manager_user), 11);

        csBillingApiMock.stubFor(WireMock.get(urlPathEqualTo("/api/v1/service/206/tariffs/search"))
                        .withQueryParam("tariffTypeId", equalTo("20"))
                .willReturn(aResponse().withBody(getStringResource("/retrofit2_response.json"))));

        String actual = FunctionalTestHelper.getWithAuth(baseUrl + "/vendors/1991/analytics/tariffs?uid=11");

        JsonAssert.assertJsonEquals(
                "{\n" +
                        "  \"errors\": [],\n" +
                        "  \"meta\": {\n" +
                        "    \"host\": \"${json-unit.ignore}\",\n" +
                        "    \"health\": \"${json-unit.ignore}\"\n" +
                        "  },\n" +
                        "  \"result\": {  \n" +
                        "   \"item\":[  \n" +
                        "      {  \n" +
                        "         \"id\":20,\n" +
                        "         \"name\":\"10 категорий\",\n" +
                        "         \"details\":{  \n" +
                        "            \"cost\":30000000,\n" +
                        "            \"periodType\":\"month\"\n" +
                        "         }\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}" +
                        "}",
                actual
        );
    }

    @Test
    @DisplayName("Обновление тарифа, выбранного вендором")
    void addVendorTariff() {
        setVendorUserRoles(Collections.singleton(Role.manager_user), 11);

        String body = "" +
                "{ \"id\" : 20 }";

        csBillingApiMock.stubFor(WireMock.get(urlPathEqualTo("/api/v1/service/206/tariffs/search"))
                .withQueryParam("tariffTypeId", equalTo("20"))
                .willReturn(aResponse().withBody(getStringResource("/retrofit2_response.json"))));

        String actual = FunctionalTestHelper.postWithAuth(baseUrl + "/vendors/1991/analytics/tariffs?uid=11", body);
        JsonAssert.assertJsonEquals(
                "{\n" +
                        "  \"errors\": [],\n" +
                        "  \"meta\": {\n" +
                        "    \"host\": \"${json-unit.ignore}\",\n" +
                        "    \"health\": \"${json-unit.ignore}\"\n" +
                        "  },\n" +
                        "  \"result\": {  \n" +
                        "   \"item\":[  \n" +
                        "      {  \n" +
                        "         \"id\":20,\n" +
                        "         \"name\":\"10 категорий\",\n" +
                        "         \"details\":{  \n" +
                        "            \"cost\":30000000,\n" +
                        "            \"periodType\":\"month\"\n" +
                        "         }\n" +
                        "      }\n" +
                        "   ]\n" +
                        "}" +
                        "}",
                actual
        );
    }
}
