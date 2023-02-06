package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OperatorEvent {

    @JsonProperty("is_cancelation_for_repeat")
    private boolean isCancelationForRepeat;

    @JsonProperty("operator_initiator")
    private String operatorInitiator;

    @JsonProperty("returned_already")
    private boolean returnedAlready;

    @JsonProperty("event_instant")
    private int eventInstant;

    @JsonProperty("external_place_id")
    private String externalPlaceId;

    @JsonProperty("responsibility")
    private String responsibility;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("external_order_id")
    private String externalOrderId;

    @JsonProperty("operator_comment")
    private String operatorComment;

    @JsonProperty("class_name")
    private String className;

    @JsonProperty("operator_event_type")
    private String operatorEventType;

    @JsonProperty("event_status")
    private String eventStatus;

    @JsonProperty("place_current_info")
    private PlaceCurrentInfo placeCurrentInfo;
}
