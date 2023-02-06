package ru.yandex.market.checkout.util.balance.checkers;

import com.google.gson.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateProductParams {

    private long partnerId;
    private String name;
    private String productId;
    private Integer serviceFee;

    public static CreateProductParams product(long partnerId, String name, String productId) {
        CreateProductParams params = new CreateProductParams();
        params.partnerId = partnerId;
        params.name = name;
        params.productId = productId;
        return params;
    }

    public static CreateProductParams product(long partnerId, String name, String productId, Integer serviceFee) {
        CreateProductParams params = new CreateProductParams();
        params.partnerId = partnerId;
        params.name = name;
        params.productId = productId;
        params.serviceFee = serviceFee;
        return params;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public Integer getServiceFee() {
        return serviceFee;
    }

    public long getPartnerId() {
        return partnerId;
    }

    void matches(JsonObject json) {
        assertEquals(productId, json.remove("product_id").getAsString());
        assertEquals(name, json.remove("name").getAsString());
        assertEquals(partnerId, json.remove("partner_id").getAsLong());
        if (serviceFee != null) {
            assertEquals(serviceFee.intValue(), json.remove("service_fee").getAsInt());
        }

        assertEquals(0, json.size(), "CreateServiceProduct request contains unexpected elements: " + json);
    }
}
