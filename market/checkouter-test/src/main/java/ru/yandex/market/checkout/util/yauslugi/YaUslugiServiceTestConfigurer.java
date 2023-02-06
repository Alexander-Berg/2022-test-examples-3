package ru.yandex.market.checkout.util.yauslugi;

import java.util.Optional;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceTimeSlotsResponse;
import ru.yandex.market.checkout.checkouter.yauslugi.rest.YaUslugiClient;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@Component
public class YaUslugiServiceTestConfigurer {

    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private WireMockServer yaUslugiMock;

    public void mockGetTimeslots(YaServiceTimeSlotsResponse timeslotsResponse) {
        mockGetTimeslots(timeslotsResponse, null);
    }

    public void mockGetTimeslots(YaServiceTimeSlotsResponse timeslotsResponse, String yandexReqId) {
        ResponseDefinitionBuilder builder =
                okJson(testSerializationService.serializeYaUslugiObject(timeslotsResponse));
        Optional.ofNullable(yandexReqId)
                .ifPresent(value -> builder.withHeader(YaUslugiClient.YANDEX_REQ_ID, yandexReqId));
        yaUslugiMock.stubFor(post(urlPathEqualTo("/ydo/api/get_cached_slots")).willReturn(builder));
    }

    public WireMockServer getYaUslugiMock() {
        return yaUslugiMock;
    }

    public void verifyZeroInteractions() {
        assertThat(getYaUslugiMock().getAllServeEvents(), hasSize(0));
    }
}
