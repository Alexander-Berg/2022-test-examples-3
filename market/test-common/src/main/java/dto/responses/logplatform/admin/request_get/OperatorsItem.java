package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OperatorsItem {

    @JsonProperty("operator_name")
    private String operatorName;

    @JsonProperty("operator_id")
    private String operatorId;
}
