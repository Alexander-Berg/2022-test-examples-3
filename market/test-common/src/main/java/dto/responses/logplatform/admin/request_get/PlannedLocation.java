package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PlannedLocation {

    @JsonProperty("class_name")
    private String className;
}
