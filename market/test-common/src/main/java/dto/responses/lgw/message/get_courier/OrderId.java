package dto.responses.lgw.message.get_courier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderId {

    @JsonProperty("deliveryId")
    private String deliveryId;

    @JsonProperty("partnerId")
    private String partnerId;

    @JsonProperty("yandexId")
    private String yandexId;
}
