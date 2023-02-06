package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FullEventsItem {

    @JsonProperty("operator_event")
    private OperatorEvent operatorEvent;

    @JsonProperty("event_id")
    private int eventId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("timestamp")
    private int timestamp;
}
