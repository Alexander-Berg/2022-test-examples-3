package ru.yandex.market.vendor.controllers.analytics;

import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.security.Role;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class AnalyticsTariffsControllerTest extends AbstractVendorPartnerFunctionalTest {

    @Autowired
    private WireMockServer csBillingApiMock;

    @Test
    @DisplayName("Получение всех тарифов услеги Маркет.Аналитика")
    void getTariffs() {
        setVendorUserRoles(Collections.singleton(Role.manager_user), 1);

        csBillingApiMock.stubFor(WireMock.get(urlPathEqualTo("/api/v1/service/206/tariffs/search"))
                .withQueryParam("tariffTypeId", equalTo("20"))
                .willReturn(aResponse().withBody(getStringResource("/getTariffs/retrofit2_response.json"))));

        String analyticsTariffs = FunctionalTestHelper.getWithAuth(baseUrl + "/tariffs/analytics?uid=1");
        JsonAssert.assertJsonEquals(
                getStringResource("/getTariffs/expected.json"),
                analyticsTariffs
        );
    }
}
