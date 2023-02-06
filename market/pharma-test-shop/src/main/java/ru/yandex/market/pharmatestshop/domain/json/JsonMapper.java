package ru.yandex.market.pharmatestshop.domain.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.pharmatestshop.domain.order.status.OrderStatus;
import ru.yandex.market.pharmatestshop.domain.order.status.OrderSubstatus;

public class JsonMapper {
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonNode getRootElement(String jsonData) throws IllegalArgumentException {
        InputStream stream = new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
        try {
            return mapper.readTree(stream);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Не валидный json-body");
        }
    }

    public long getIdFromJsonData(String orderStatusRequest) throws IllegalArgumentException {
        JsonNode rootNode = getRootElement(orderStatusRequest);
        return rootNode.path("order").path("id").asLong();
    }

    public OrderStatus getStatusFromJsonData(String orderStatusRequest) throws IllegalArgumentException {
        JsonNode rootNode = getRootElement(orderStatusRequest);
        return OrderStatus.valueOf(rootNode.path("order").path("status").asText());
    }


    public OrderSubstatus getSubStatusFromJsonData(String orderJsonString) {
        JsonNode rootNode = getRootElement(orderJsonString);
        //Обработка пустого субстатуса
        String substatusString=rootNode.path("order").path("substatus").asText().equals("")?"UNKNOWN":rootNode.path("order").path("substatus").asText();
        return OrderSubstatus.valueOf(substatusString);

    }

    public String getDeliveryTypeFromJsonData(String orderJsonString) {
        JsonNode rootNode = getRootElement(orderJsonString);
        return rootNode.path("order").path("delivery").path("type").asText();

    }

    public String setStatusAndSubstatusToJsonData(String orderJsonString, OrderStatus orderStatus,
                                                  OrderSubstatus orderSubstatus) {

        try {
            JSONObject jsonObject = new JSONObject(orderJsonString);
            JSONObject order = (JSONObject) jsonObject.get("order");
            order.put("status", orderStatus);
            order.put("substatus", orderSubstatus);

            return jsonObject.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;

    }


}
