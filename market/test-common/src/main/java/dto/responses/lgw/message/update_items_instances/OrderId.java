package dto.responses.lgw.message.update_items_instances;

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
