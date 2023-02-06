package ru.yandex.market.checkout.pushapi.web;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartParameters;
import ru.yandex.market.checkout.pushapi.providers.PushApiCartProvider;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CartWrongTokenTest extends AbstractWebTestBase {

    private static final long DEFAULT_SHOP_ID = 2234562L;
    private static final boolean SANDBOX = false;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PushApiTestSerializationService testSerializationService;
    @Autowired
    private WireMockServer shopadminStubMock;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SettingsProvider settingsProvider;

    @Test
    public void shouldSendWrongTokenOnCartWrongToken() throws Exception {
        Cart cart = PushApiCartProvider.buildCartRequest();
        CartResponse cartResponse = PushApiCartParameters.mapCartRequestToResponse(cart, DataType.JSON);

        mockPostSettings(DEFAULT_SHOP_ID, settingsProvider.buildXmlSettings(), SANDBOX);

        shopadminStubMock.stubFor(
                WireMock.post(urlPathEqualTo("/cart"))
                        .willReturn(aResponse().withStatus(403)));

        RequestContextHolder.createNewContext();
        var result = mockMvc.perform(post("/shops/{shopId}/cart/wrong-token", DEFAULT_SHOP_ID)
                        .content(testSerializationService.serialize(cart))
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn();

        List<ServeEvent> serveEvents = shopadminStubMock.getAllServeEvents();
        assertThat(serveEvents, hasSize(1));
        ServeEvent serveEvent = Iterables.getOnlyElement(serveEvents);
        MultiValueMap<String, String> params = UriComponentsBuilder
                .fromUriString(serveEvent.getRequest().getUrl())
                .build()
                .getQueryParams();

        String token = params.getFirst("auth-token");
        Assertions.assertNotEquals(SettingsProvider.DEFAULT_TOKEN, token);
    }

    @Test
    public void shouldFailIfShopReturnsNotForbidden() throws Exception {
        Cart cart = PushApiCartProvider.buildCartRequest();
        CartResponse cartResponse = PushApiCartParameters.mapCartRequestToResponse(cart, DataType.JSON);

        mockPostSettings(DEFAULT_SHOP_ID, settingsProvider.buildXmlSettings(), SANDBOX);

        shopadminStubMock.stubFor(
                WireMock.post(urlPathEqualTo("/cart"))
                        .willReturn(aResponse()
                                .withHeaders(new HttpHeaders(new ContentTypeHeader(MediaType.APPLICATION_JSON_UTF8_VALUE)))
                                .withBody(testSerializationService.serializeJson(cartResponse))));

        RequestContextHolder.createNewContext();
        var result = mockMvc.perform(post("/shops/{shopId}/cart/wrong-token", DEFAULT_SHOP_ID)
                        .content(testSerializationService.serialize(cart))
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isUnprocessableEntity());
    }

}
