package ru.yandex.market.checkout.util.serialize;

import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;

public final class OrderResponseJsonSerializer {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    private OrderResponseJsonSerializer() {
    }

    public static String serializeJson(OrderResponse orderResponse) {
        try {
            JSONObject order = new JSONObject();
            order.put("id", orderResponse.getId());
            order.put("accepted", orderResponse.isAccepted());
            if (orderResponse.getReason() != null) {
                order.put("reason", orderResponse.getReason().name());
            }
            if (orderResponse.getShipmentDate() != null) {
                order.put("shipmentDate", DATE_FORMAT.format(orderResponse.getShipmentDate()));
            }

            JSONObject result = new JSONObject();
            result.put("order", order);
            return result.toString();
        } catch (JSONException jsonEx) {
            throw new RuntimeException(jsonEx);
        }
    }
}
