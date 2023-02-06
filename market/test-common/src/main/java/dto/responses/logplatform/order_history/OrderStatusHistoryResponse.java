package dto.responses.logplatform.order_history;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrderStatusHistoryResponse {
    @JsonProperty("state_history")
    List<OrderStatusEvent> stateHistory;
}
