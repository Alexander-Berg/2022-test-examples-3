package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateOrderResponse {

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("order")
    private Order order;
}
