package ru.yandex.market.hrms.test.configurer;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WarehouseApiConfigurer {

    private final WireMockServer warehouseApiWireMockServer;

    public void mockFindStaffLogin(String wmsLogin, String staffLogin) {
        warehouseApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/api/StaffLogin"))
                        .withQueryParam("login", WireMock.equalTo(wmsLogin))
                        .willReturn(WireMock.okJson("{\"staffLogin\": \"" + staffLogin + "\"}"))
        );
    }

    public void reset() {
        warehouseApiWireMockServer.resetAll();
    }

    public void mockNotFoundStaffLogin(String wmsLogin) {
        warehouseApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/api/StaffLogin"))
                        .withQueryParam("login", WireMock.equalTo(wmsLogin))
                        .willReturn(WireMock.notFound().withBody("Not Found"))
        );
    }

    public void verifyAllRequestsCount(int count) {
        warehouseApiWireMockServer.verify(count, RequestPatternBuilder.allRequests());
    }
}
