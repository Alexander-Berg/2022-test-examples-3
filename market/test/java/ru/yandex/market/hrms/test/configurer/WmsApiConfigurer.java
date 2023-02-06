package ru.yandex.market.hrms.test.configurer;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

@Component
@RequiredArgsConstructor
public class WmsApiConfigurer {

    private final Stubbing wmsApiWireMockServer;

    public void mockAuthUsersResponse(String response) {
        wmsApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/auth/users"))
                        .willReturn(WireMock.okJson(response))
        );
    }

    public void mockCreateUser(String requestBody, String response) {
        wmsApiWireMockServer.givenThat(
                WireMock.post(WireMock.urlPathEqualTo("/auth/users"))
                        .withRequestBody(equalToJson(requestBody))
                        .willReturn(WireMock.okJson(response))
        );
    }

    public void mockBeginner(String requestBody) {
        wmsApiWireMockServer.givenThat(
                WireMock.post(WireMock.urlPathEqualTo("/auth/users/beginner"))
                        .withRequestBody(equalToJson(requestBody))
                        .willReturn(WireMock.ok())
        );
    }
}
