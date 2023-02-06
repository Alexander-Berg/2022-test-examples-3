package ru.yandex.market.checkout.pushapi.helpers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.pushapi.settings.Settings;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * @author sergeykoles
 * Created on: 26.12.2019
 */
@Component
public class CheckouterMockConfigurer {

    @Autowired
    private WireMockServer checkouterMock;

    @Autowired
    private ObjectMapper annotationObjectMapper;

    public void setShopMetaData(long shopId, ShopMetaData data) {
        try {
            checkouterMock.stubFor(get(urlEqualTo("/shops/" + shopId))
                    .willReturn(
                            ok(annotationObjectMapper.writeValueAsString(data))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ShopMetaData." + data, e);
        }
    }

    public void setDefaultResponse() {
        try {
            checkouterMock.stubFor(get(WireMock.urlMatching("/shops/\\d+")).atPriority(Integer.MAX_VALUE)
                    .willReturn(
                            ok(annotationObjectMapper.writeValueAsString(ShopMetaData.DEFAULT))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize ShopMetaData." + ShopMetaData.DEFAULT, e);
        }
    }

    public void setSettings(long shopId, Settings settings) {
        try {
            checkouterMock.stubFor(get(urlPathEqualTo("/pushapi-settings/" + shopId))
                    .willReturn(ok(annotationObjectMapper.writeValueAsString(settings))
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Settings. " + settings, e);
        }
    }

    public void mockPostSettings(long shopId, Settings settings, Boolean sandbox) {
        try {
            checkouterMock.stubFor(post(urlPathEqualTo("/pushapi-settings/" + shopId +
                    (sandbox == null ? "" : "?sandbox=" + sandbox)))
                    .willReturn(ok(annotationObjectMapper.writeValueAsString(settings))
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Settings. " + settings, e);
        }
    }
}
