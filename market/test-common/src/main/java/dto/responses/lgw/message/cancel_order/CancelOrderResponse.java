package dto.responses.lgw.message.cancel_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CancelOrderResponse {

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("orderId")
    private OrderId orderId;
}
