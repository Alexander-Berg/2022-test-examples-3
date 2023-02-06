package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SubStatusesItem {

    @JsonProperty("external_info")
    private ExternalInfo externalInfo;

    @JsonProperty("timestamp")
    private int timestamp;

    @JsonProperty("status")
    private String status;
}
