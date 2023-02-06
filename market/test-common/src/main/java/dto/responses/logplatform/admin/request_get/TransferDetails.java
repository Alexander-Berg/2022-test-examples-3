package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransferDetails {

    @JsonProperty("node_from_id")
    private String nodeFromId;

    @JsonProperty("input_carriage_id")
    private String inputCarriageId;

    @JsonProperty("batching_allowed")
    private boolean batchingAllowed;

    @JsonProperty("return_node_id")
    private String returnNodeId;

    @JsonProperty("action_from_id")
    private int actionFromId;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("new_logistic_contract")
    private boolean newLogisticContract;

    @JsonProperty("is_allow_to_be_second_in_batching")
    private boolean isAllowToBeSecondInBatching;

    @JsonProperty("action_to_id")
    private int actionToId;

    @JsonProperty("generation_tag_id")
    private String generationTagId;

    @JsonProperty("transfer_id")
    private String transferId;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("is_allowed_to_be_in_taxi_batch")
    private boolean isAllowedToBeInTaxiBatch;

    @JsonProperty("is_rover_allowed")
    private boolean isRoverAllowed;

    @JsonProperty("external_order_id")
    private String externalOrderId;

    @JsonProperty("node_to_id")
    private String nodeToId;

    @JsonProperty("waybill_planner_task_id")
    private String waybillPlannerTaskId;

    @JsonProperty("internal_place_id")
    private String internalPlaceId;

    @JsonProperty("output_carriage_id")
    private String outputCarriageId;

    @JsonProperty("semi_live_batching_allowed")
    private boolean semiLiveBatchingAllowed;

    @JsonProperty("live_batching_allowed")
    private boolean liveBatchingAllowed;

    @JsonProperty("status")
    private String status;
}
