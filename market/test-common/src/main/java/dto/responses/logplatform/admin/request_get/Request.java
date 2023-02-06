package dto.responses.logplatform.admin.request_get;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Request {

    @JsonProperty("decision_deadline")
    private int decisionDeadline;

    @JsonProperty("created_at")
    private int createdAt;

    @JsonProperty("multi_orders_delivering_available")
    private boolean multiOrdersDeliveringAvailable;

    @JsonProperty("priority")
    private int priority;

    @JsonProperty("revision")
    private int revision;

    @JsonProperty("employer_code")
    private String employerCode;

    @JsonProperty("additional_codes")
    private List<Object> additionalCodes;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("request_code")
    private String requestCode;

    @JsonProperty("corp_client_id")
    private String corpClientId;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("return_node_code")
    private String returnNodeCode;

    @JsonProperty("status")
    private String status;
}
