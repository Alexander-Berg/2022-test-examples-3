package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Information {

    @JsonProperty("internal_status")
    private String internalStatus;

    @JsonProperty("description")
    private String description;
}
