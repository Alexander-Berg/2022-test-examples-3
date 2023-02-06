package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ActionDetails {

    @JsonProperty("requested_action_type")
    private String requestedActionType;

    @JsonProperty("planned_instant")
    private PlannedInstant plannedInstant;

    @JsonProperty("action_code")
    private String actionCode;

    @JsonProperty("requested_instant")
    private RequestedInstant requestedInstant;

    @JsonProperty("execution_idx")
    private int executionIdx;

    @JsonProperty("internal_contractor_id")
    private String internalContractorId;

    @JsonProperty("confirmation_info")
    private ConfirmationInfo confirmationInfo;

    @JsonProperty("operator_contractor_id")
    private String operatorContractorId;

    @JsonProperty("requested_instant_hr")
    private String requestedInstantHr;

    @JsonProperty("action_id")
    private int actionId;

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty("action_status")
    private String actionStatus;

    @JsonProperty("status")
    private String status;
}
