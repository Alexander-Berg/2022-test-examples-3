package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateOrderSuccess {

    @JsonProperty("orderId")
    private OrderId orderId;

    @JsonProperty("trackId")
    private TrackId trackId;

    @JsonProperty("partner")
    private Partner partner;
}
