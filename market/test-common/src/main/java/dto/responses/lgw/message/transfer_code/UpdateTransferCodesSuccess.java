package dto.responses.lgw.message.transfer_code;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateTransferCodesSuccess {

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("orderId")
    private OrderId orderId;
}
