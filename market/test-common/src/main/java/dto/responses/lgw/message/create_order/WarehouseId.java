package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WarehouseId {

    @JsonProperty("fulfillmentId")
    private String fulfillmentId;

    @JsonProperty("partnerId")
    private String partnerId;

    @JsonProperty("yandexId")
    private String yandexId;
}
