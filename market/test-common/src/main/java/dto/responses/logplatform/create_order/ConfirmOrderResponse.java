package dto.responses.logplatform.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ConfirmOrderResponse {
    @JsonProperty("request_id")
    private String requestId;
}
