package dto.responses.lgw.message.update_courier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderId {

    @JsonProperty("fulfillmentId")
    private String fulfillmentId;

    @JsonProperty("partnerId")
    private String partnerId;

    @JsonProperty("yandexId")
    private String yandexId;
}
