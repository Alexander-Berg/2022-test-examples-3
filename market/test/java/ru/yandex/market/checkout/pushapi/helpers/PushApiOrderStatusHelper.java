package ru.yandex.market.checkout.pushapi.helpers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.pushapi.client.entity.PushApiOrder;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@WebTestHelper
public class PushApiOrderStatusHelper {

    private final MockMvc mockMvc;
    private final WireMockServer shopadminStubMock;
    private final PushApiTestSerializationService testSerializationService;
    private final SettingsProvider settingsProvider;
    private final SettingsService settingsService;

    public PushApiOrderStatusHelper(MockMvc mockMvc,
                                    WireMockServer shopadminStubMock,
                                    PushApiTestSerializationService testSerializationService,
                                    SettingsProvider settingsProvider,
                                    SettingsService settingsService) {
        this.mockMvc = mockMvc;
        this.shopadminStubMock = shopadminStubMock;
        this.testSerializationService = testSerializationService;
        this.settingsProvider = settingsProvider;
        this.settingsService = settingsService;
    }

    public ResultActions orderStatusForActions(PushApiOrderStatusParameters parameters) throws Exception {
        Settings settings = settingsProvider.buildXmlSettings(
                parameters.isPartnerInterface(), parameters.getDataType()
        );

        settingsService.updateSettings(parameters.getShopId(), settings, parameters.isSandbox());

        RequestContextHolder.createNewContext();
        shopadminStubMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/order/status"))
                .willReturn(aResponse().withStatus(200)));

        var result = mockMvc.perform(post("/shops/{shopId}/order/status", parameters.getShopId())
                        .content(testSerializationService.serialize(new PushApiOrder(parameters.getOrderChange())))
                        .contentType(MediaType.APPLICATION_XML)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(result));
    }

    public List<ServeEvent> getServeEvents() {
        return shopadminStubMock.getAllServeEvents();
    }
}
