package dto.responses.logplatform.admin.request_get;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RequestGetResponse {

    @JsonProperty("operators")
    private List<OperatorsItem> operators;

    @JsonProperty("employer")
    private Employer employer;

    @JsonProperty("model")
    private Model model;

    @JsonProperty("stations")
    private List<StationsItem> stations;

    @JsonProperty("external_requests")
    private List<ExternalRequestsItem> externalRequests;
}
