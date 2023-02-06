package dto.responses.logplatform.admin.request_get;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExternalRequestsItem {

    @JsonProperty("contractor_id")
    private String contractorId;

    @JsonProperty("transfer_ids")
    private List<Object> transferIds;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("reservation_ids")
    private List<String> reservationIds;

    @JsonProperty("full_events")
    private List<Object> fullEvents;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("tracking_id")
    private String trackingId;

    @JsonProperty("events")
    private List<Map<String, String>> events;
}
