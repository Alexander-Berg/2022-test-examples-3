package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RequestedInstant {

    @JsonProperty("min")
    private int min;

    @JsonProperty("max")
    private int max;

    @JsonProperty("policy")
    private String policy;
}
