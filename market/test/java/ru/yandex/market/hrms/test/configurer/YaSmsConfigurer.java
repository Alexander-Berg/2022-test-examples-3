package ru.yandex.market.hrms.test.configurer;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class YaSmsConfigurer {

    private final Stubbing yaSmsApiWireMockServer;

    public void mockSendSmsSuccess(String resultXml) {
        yaSmsApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/sendsms"))
                        .willReturn(WireMock.okTextXml(resultXml))
        );
    }

    public void mockSendSmsForbidden() {
        yaSmsApiWireMockServer.givenThat(
                WireMock.get(WireMock.urlPathEqualTo("/sendsms"))
                        .willReturn(WireMock.forbidden())
        );
    }
}
