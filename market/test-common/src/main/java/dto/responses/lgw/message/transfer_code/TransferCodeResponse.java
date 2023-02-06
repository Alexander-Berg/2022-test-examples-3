package dto.responses.lgw.message.transfer_code;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransferCodeResponse {

    @JsonProperty("codes")
    private Codes codes;

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("orderId")
    private OrderId orderId;
}
