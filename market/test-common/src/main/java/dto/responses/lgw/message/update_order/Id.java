package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Id {

    @JsonProperty("yandexId")
    private String yandexId;

    @JsonProperty("deliveryId")
    private String deliveryId;

    @JsonProperty("partnerId")
    private String partnerId;
}
