package ru.yandex.market.checkout.pushapi.application;

import java.nio.charset.StandardCharsets;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.pushapi.config.web.WebMvcConfig;
import ru.yandex.market.checkout.pushapi.helpers.CheckouterMockConfigurer;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartParameters;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderParameters;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderStatusParameters;
import ru.yandex.market.checkout.pushapi.helpers.PushApiQueryStocksParameters;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;
import ru.yandex.market.common.test.guava.ForgetfulSuppliersInitializer;
import ru.yandex.market.metrics.micrometer.PrometheusConfiguration;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

// TODO: Заменить на ContextHierarcy когда сможем поднимать services-контекст отдельно
@ContextConfiguration(
        classes = {AbstractWebTestBase.WebConfiguration.class},
        initializers = {ForgetfulSuppliersInitializer.class})
@WebAppConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class AbstractWebTestBase extends AbstractServicesTestBase {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    protected MockMvc mockMvc;

    @Autowired
    protected SettingsProvider settingsProvider;
    @Autowired
    protected CheckouterMockConfigurer checkouterMockConfigurer;
    @SpyBean(name = "settingsService")
    protected SettingsService settingsService;
    @Autowired
    protected WireMockServer shopadminStubMock;
    @Autowired
    protected PushApiTestSerializationService testSerializationService;

    protected void mockPostSettings(long shopId, Settings settings, Boolean sandbox) {
        checkouterMockConfigurer.mockPostSettings(shopId, settings, sandbox);
        Mockito.doReturn(settings).when(settingsService).getSettings(shopId, Boolean.TRUE.equals(sandbox));
        Mockito.doNothing().when(settingsService).updateSettings(Mockito.any(Long.class), Mockito.any(Settings.class)
                , Mockito.any(Boolean.class));
        checkouterMockConfigurer.setSettings(shopId, settings);
    }

    protected void mockPostSettings(long shopId, Settings settings) {
        mockPostSettings(shopId, settings, null);
    }

    protected void mockSettingsForDifferentParameters(PushApiCartParameters parameters) {
        var settings = settingsProvider.buildXmlSettings(parameters.isPartnerInterface(), parameters.getDataType());

        mockPostSettings(parameters.getShopId(), settings, parameters.isSandbox());
    }

    protected void mockSettingsForDifferentParameters(PushApiOrderParameters parameters) {
        var settings =
                settingsProvider.buildXmlSettings(parameters.isPartnerInterface(), parameters.getDataType()).toBuilder()
                        .features(parameters.getFeatures())
                        .build();
        mockPostSettings(parameters.getShopId(), settings, parameters.isSandbox());
    }

    protected void mockSettingsForDifferentParameters(PushApiOrderStatusParameters parameters) {
        var settings = settingsProvider.buildXmlSettings(
                parameters.isPartnerInterface(), parameters.getDataType()
        );
        mockPostSettings(parameters.getShopId(), settings, parameters.isSandbox());
    }

    protected void mockSettingsForDifferentParameters(PushApiQueryStocksParameters parameters) {
        var settings = settingsProvider.buildXmlSettings(
                parameters.isPartnerInterface(), parameters.getDataType()
        );
        mockPostSettings(parameters.getShopId(), settings, parameters.isSandbox());
    }

    protected void mockErrorShopAdmin() {
        shopadminStubMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/error/shop-admin"))
                .willReturn(aResponse().withStatus(422)));
    }

    protected ResultActions orderStatusForActions(PushApiOrderStatusParameters parameters) throws Exception {
        Settings settings = settingsProvider.buildXmlSettings(
                parameters.isPartnerInterface(), parameters.getDataType()
        );
        mockPostSettings(parameters.getShopId(), settings, parameters.isSandbox());
        settingsService.updateSettings(parameters.getShopId(), settings, parameters.isSandbox());

        RequestContextHolder.createNewContext();
        shopadminStubMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/order/status"))
                .willReturn(aResponse().withStatus(200)));

        var result = mockMvc.perform(post("/shops/{shopId}/order/status", parameters.getShopId())
                        .content(testSerializationService.serialize(parameters.getOrderChange()))
                        .contentType(MediaType.APPLICATION_XML)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                )
                .andExpect(request().asyncStarted())
                .andReturn();
        return mockMvc.perform(asyncDispatch(result));
    }

    @BeforeEach
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Configuration
    @Import({WebMvcConfig.class, PrometheusConfiguration.class})
    @ConditionalOnWebApplication
    @ComponentScan(
            basePackages = {"ru.yandex.market.checkout.pushapi.helpers", "ru.yandex.market.checkout.pushapi.serde"},
            includeFilters = @ComponentScan.Filter(WebTestHelper.class)
    )
    public static class WebConfiguration {

    }
}
