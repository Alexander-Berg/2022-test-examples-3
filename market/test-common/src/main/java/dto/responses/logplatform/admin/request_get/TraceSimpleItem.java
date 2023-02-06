package dto.responses.logplatform.admin.request_get;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TraceSimpleItem {

    @JsonProperty("sub_statuses")
    private List<SubStatusesItem> subStatuses;

    @JsonProperty("timestamp")
    private int timestamp;

    @JsonProperty("status")
    private String status;
}
