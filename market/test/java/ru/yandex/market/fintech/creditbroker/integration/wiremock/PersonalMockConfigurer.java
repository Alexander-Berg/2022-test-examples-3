package ru.yandex.market.fintech.creditbroker.integration.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class PersonalMockConfigurer {

    @Autowired
    private WireMockServer personalMock;

    public void mockV1PhonesRetrieve() {

    }

}
