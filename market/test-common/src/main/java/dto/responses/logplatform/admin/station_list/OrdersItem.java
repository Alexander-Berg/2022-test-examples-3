package dto.responses.logplatform.admin.station_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OrdersItem {

    @JsonProperty("start_instant")
    private String startInstant;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("external_order_id")
    private String externalOrderId;

    @JsonProperty("request_id")
    private String requestId;
}
