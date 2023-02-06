package ru.yandex.market.hrms.test.configurer;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LenelApiConfigurer {

    private final Stubbing lenelApiWireMockServer;

    public void mockCreateCardholderSuccess(String resultJson) {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/AddCardholder"))
                        .willReturn(WireMock.okJson(resultJson))
        );
    }

    public void mockCreateCardholderForbidden() {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/AddCardholder"))
                        .willReturn(WireMock.forbidden())
        );
    }

    public void mockUpdateCardholderSuccess(String resultJson) {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/UpdateCardholder"))
                        .willReturn(WireMock.okJson(resultJson))
        );
    }

    public void mockUpdateCardholderForbidden() {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/UpdateCardholder"))
                        .willReturn(WireMock.forbidden())
        );
    }

    public void mockRemoveCardholderSuccess(String resultJson) {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/DeleteCardholder"))
                        .willReturn(WireMock.okJson(resultJson))
        );
    }

    public void mockRemoveCardholderForbidden() {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/DeleteCardholder"))
                        .willReturn(WireMock.forbidden())
        );
    }

    public void mockAddBadgeToCardholderSuccess(String resultJson) {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/AddBadge"))
                        .willReturn(WireMock.okJson(resultJson))
        );
    }

    public void mockAddBadgeToCardholderForbidden() {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/AddBadge"))
                        .willReturn(WireMock.forbidden())
        );
    }

    public void mockRemoveBadgeSuccess(String resultJson) {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/DeleteBadge"))
                        .willReturn(WireMock.okJson(resultJson))
        );
    }

    public void mockRemoveBadgeForbidden() {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/DeleteBadge"))
                        .willReturn(WireMock.forbidden())
        );
    }

    public void mockGetBadgeByIdSuccess(String resultJson) {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/GetBadgeById"))
                        .willReturn(WireMock.okJson(resultJson))
        );
    }

    public void mockGetBadgeByIdForbidden() {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/GetBadgeById"))
                        .willReturn(WireMock.forbidden())
        );
    }

    public void mockGetBadgeByKeySuccess(String resultJson) {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/GetBadgeByKey"))
                        .willReturn(WireMock.okJson(resultJson))
        );
    }

    public void mockGetBadgeByKeyForbidden() {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/GetBadgeByKey"))
                        .willReturn(WireMock.forbidden())
        );
    }

    public void mockGetLoggedEventsSuccess(String currentScenario, String nextScenario, String resultJson) {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/GetLoggedEvents"))
                        .inScenario(currentScenario)
                        .willSetStateTo(nextScenario)
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(WireMock.okJson(resultJson))
        );
    }

    public void mockGetLoggedEventsForbidden() {
        lenelApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/GetLoggedEvents"))
                        .willReturn(WireMock.forbidden())
        );
    }
}
