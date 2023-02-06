package dto.responses.logplatform.order_history;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderStatusEvent {
    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("status")
    private String status;
}
