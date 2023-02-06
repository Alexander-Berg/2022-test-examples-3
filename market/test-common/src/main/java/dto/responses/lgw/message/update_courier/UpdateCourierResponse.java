package dto.responses.lgw.message.update_courier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateCourierResponse {

    @JsonProperty("codes")
    private Codes codes;

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("outboundCourier")
    private OutboundCourier outboundCourier;

    @JsonProperty("orderId")
    private OrderId orderId;
}
