package ru.yandex.market.hrms.core.service.sc;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScApiConfigurer {
    private final Stubbing scApiWireMockServer;

    public void mockCreateOrUpdate(String resultJson) {
        scApiWireMockServer.givenThat(
                WireMock.put(WireMock.urlPathEqualTo("/hermes/user"))
                        .willReturn(WireMock.okJson(resultJson))
        );
    }

    public void mockDelete(Long id) {
        scApiWireMockServer.givenThat(
                WireMock.delete(WireMock.urlPathEqualTo("/hermes/user/" + id))
                        .willReturn(WireMock.ok())
        );
    }
}
