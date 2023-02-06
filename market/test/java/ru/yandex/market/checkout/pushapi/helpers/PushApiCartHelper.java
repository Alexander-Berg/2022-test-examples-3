package ru.yandex.market.checkout.pushapi.helpers;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Assertions;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;
import ru.yandex.market.checkout.util.shopapi.ShopApiConfigurer;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.ACTION_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.API_SETTINGS;
import static ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams.CONTEXT;

@WebTestHelper
public class PushApiCartHelper {

    private final MockMvc mockMvc;
    private final PushApiTestSerializationService testSerializationService;
    private final SettingsService settingsService;
    private final SettingsProvider settingsProvider;
    private final ShopApiConfigurer shopApiConfigurer;

    public PushApiCartHelper(MockMvc mockMvc,
                             PushApiTestSerializationService testSerializationService,
                             SettingsService settingsService,
                             SettingsProvider settingsProvider,
                             ShopApiConfigurer shopApiConfigurer) {
        this.mockMvc = mockMvc;
        this.testSerializationService = testSerializationService;
        this.settingsService = settingsService;
        this.settingsProvider = settingsProvider;
        this.shopApiConfigurer = shopApiConfigurer;
    }

    public CartResponse cart(PushApiCartParameters parameters) throws Exception {
        MvcResult mvcResult = cartForActions(parameters)
                .andExpect(status().isOk())
                .andDo(log())
                .andReturn();
        String response = new String(mvcResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        return testSerializationService.deserialize(response, CartResponse.class);
    }

    public void cartException(PushApiCartParameters parameters, String errorMessage) throws Exception {
        MvcResult mvcResult = cartForActions(parameters)
                .andExpect(status().is4xxClientError())
                .andDo(log())
                .andReturn();


        String response = new String(mvcResult.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        Assertions.assertTrue(response.contains(errorMessage));
    }

    public ResultActions cartForActions(PushApiCartParameters parameters) throws Exception {
        RequestContextHolder.createNewContext();

        setupSettings(parameters);
        shopApiConfigurer.mockCart(parameters);

        return performCart(parameters);
    }

    public void setupSettings(long shopId, boolean sandbox, boolean partnerInterface, DataType dataType) {
        settingsService.updateSettings(
                shopId,
                settingsProvider.buildXmlSettings(partnerInterface, dataType),
                sandbox
        );
    }

    public List<ServeEvent> getServeEvents() {
        return shopApiConfigurer.getServeEvents();
    }

    private void setupSettings(PushApiCartParameters parameters) {
        setupSettings(
                parameters.getShopId(),
                parameters.isSandbox(),
                parameters.isPartnerInterface(),
                parameters.getDataType()
        );
    }

    private ResultActions performCart(PushApiCartParameters parameters) throws Exception {
        MockHttpServletRequestBuilder builder = post("/shops/{shopId}/cart", parameters.getShopId())
                .param("sandbox", String.valueOf(parameters.isSandbox()))
                .param("uid", String.valueOf(parameters.getUid()));

        if (parameters.getContext() != null) {
            builder.param(CONTEXT, parameters.getContext().name());
        }

        if (parameters.getApiSettings() != null) {
            builder.param(API_SETTINGS, parameters.getApiSettings().name());
        }

        if (parameters.getActionId() != null) {
            builder.param(ACTION_ID, parameters.getActionId());
        }

        var result = mockMvc.perform(builder
                        .content(testSerializationService.serialize(parameters.getRequest()))
                        .contentType(MediaType.APPLICATION_XML)
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(result));
    }
}
