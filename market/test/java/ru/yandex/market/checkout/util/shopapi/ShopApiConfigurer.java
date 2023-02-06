package ru.yandex.market.checkout.util.shopapi;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ContentTypeHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartParameters;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderParameters;
import ru.yandex.market.checkout.pushapi.helpers.PushApiQueryStocksParameters;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@Component
public class ShopApiConfigurer {

    private final WireMockServer shopadminStubMock;
    private final PushApiTestSerializationService pushApiTestSerializationService;

    public ShopApiConfigurer(WireMockServer shopadminStubMock,
                             PushApiTestSerializationService pushApiTestSerializationService) {
        this.shopadminStubMock = shopadminStubMock;
        this.pushApiTestSerializationService = pushApiTestSerializationService;
    }

    public void mockCart(PushApiCartParameters parameters) {
        CartResponse cartResponse = parameters.getShopCartResponse();
        String url;
        if (parameters.isPartnerInterface() || ApiSettings.STUB == parameters.getApiSettings()) {
            url = "/" + parameters.getShopId() + "/cart";
        } else {
            url = "/cart";
        }

        String content;
        String contentType;
        switch (parameters.getDataType()) {
            case XML:
                content = pushApiTestSerializationService.serialize(cartResponse);
                contentType = MediaType.APPLICATION_XML_VALUE;
                break;
            case JSON:
                content = pushApiTestSerializationService.serializeJson(cartResponse);
                contentType = MediaType.APPLICATION_JSON_UTF8_VALUE;
                break;
            default:
                throw new IllegalArgumentException("dataType: " + parameters.getDataType());
        }

        shopadminStubMock.stubFor(post(urlPathEqualTo(url))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withBody(content)
                        .withHeaders(new HttpHeaders(new ContentTypeHeader(contentType)))));
    }

    public void mockOrderResponse(PushApiOrderParameters parameters) {
        boolean partnerInterface = parameters.isPartnerInterface();
        OrderResponse orderResponse = parameters.getOrderResponse();
        String url = (partnerInterface ? "/" + parameters.getShopId() : "") + "/order/accept";
        String content;
        String contentType;
        switch (parameters.getDataType()) {
            case XML:
                content = pushApiTestSerializationService.serialize(orderResponse);
                contentType = MediaType.APPLICATION_XML_VALUE;
                break;
            case JSON:
                content = pushApiTestSerializationService.serializeJson(orderResponse);
                contentType = MediaType.APPLICATION_JSON_UTF8_VALUE;
                break;
            case UNKNOWN:
                content = pushApiTestSerializationService.serializeJson(orderResponse);
                contentType = MediaType.TEXT_HTML_VALUE;
                break;
            default:
                throw new IllegalArgumentException("dataType: " + parameters.getDataType());
        }

        shopadminStubMock.stubFor(post(urlPathEqualTo(url))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withBody(content)
                        .withHeaders(new HttpHeaders(new ContentTypeHeader(contentType)))));
    }

    public void mockStocks(PushApiQueryStocksParameters parameters) {
        boolean partnerInterface = parameters.isPartnerInterface();
        String url = (partnerInterface ? "/" + parameters.getShopId() : "") + "/stocks";

        String content = parameters.callContentSerializer();
        String contentType;
        switch (parameters.getDataType()) {
            case XML:
                contentType = MediaType.APPLICATION_XML_VALUE;
                break;
            case JSON:
                contentType = MediaType.APPLICATION_JSON_UTF8_VALUE;
                break;
            default:
                throw new IllegalArgumentException("DataType is not supported: " + parameters.getDataType());
        }
        shopadminStubMock.stubFor(post(urlPathEqualTo(url))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withBody(content)
                        .withFixedDelay(parameters.getResponseDelay())
                        .withHeaders(new HttpHeaders(new ContentTypeHeader(contentType)))));

    }

    public List<ServeEvent> getServeEvents() {
        return shopadminStubMock.getAllServeEvents();
    }
}
