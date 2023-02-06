package ru.yandex.market.checkout.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.StocksRequest;
import ru.yandex.market.checkout.pushapi.client.entity.StocksResponse;
import ru.yandex.market.checkout.util.serialize.CartResponseJsonSerializer;
import ru.yandex.market.checkout.util.serialize.OrderResponseJsonSerializer;
import ru.yandex.market.checkout.util.serialize.StocksResponseJsonSerializer;

@Component
public class PushApiTestSerializationService {

    private final AbstractHttpMessageConverter inClassMappingJsonMessageConverter;
    private final AbstractHttpMessageConverter shopApiClassMappingJsonMessageConverter;

    public PushApiTestSerializationService(AbstractHttpMessageConverter inClassMappingXmlMessageConverter,
                                           AbstractHttpMessageConverter shopApiClassMappingJsonMessageConverter) {
        this.inClassMappingJsonMessageConverter = inClassMappingXmlMessageConverter;
        this.shopApiClassMappingJsonMessageConverter = shopApiClassMappingJsonMessageConverter;
    }

    public <T> String serialize(T cart) {
        return doSerialize(cart, inClassMappingJsonMessageConverter);
    }

    public <T> String serializeOut(T cartResponse) {
        return doSerialize(cartResponse, shopApiClassMappingJsonMessageConverter);
    }

    public String serializeJson(CartResponse cart) {
        return CartResponseJsonSerializer.serializeJson(cart);
    }

    public String serializeJson(OrderResponse orderResponse) {
        return OrderResponseJsonSerializer.serializeJson(orderResponse);
    }

    public String serializeJson(StocksResponse stocksResponse) {
        return StocksResponseJsonSerializer.serializeJson(stocksResponse);
    }

    public String serializeJson(StocksRequest stocksRequest) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("warehouseId", stocksRequest.getWarehouseId());

        if (CollectionUtils.isNotEmpty(stocksRequest.getSkus())) {
            JSONArray skuArray = new JSONArray();
            for (String sku : stocksRequest.getSkus()) {
                skuArray.put(sku);
            }
            jsonObject.put("skus", skuArray);
        }
        return jsonObject.toString();
    }

    public <T> T deserialize(String response, Class<T> cartResponseClass) {
        try {
            MockHttpInputMessage mockHttpInputMessage =
                    new MockHttpInputMessage(response.getBytes(StandardCharsets.UTF_8));
            return (T) inClassMappingJsonMessageConverter.read(cartResponseClass, mockHttpInputMessage);
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    private <T> String doSerialize(T cart, AbstractHttpMessageConverter converter) {
        try {
            MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
            converter.write(cart, MediaType.APPLICATION_XML, mockHttpOutputMessage);
            return mockHttpOutputMessage.getBodyAsString();
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }
}
