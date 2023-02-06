package dto.responses.lgw.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Values {

    @JsonProperty("delaySeconds")
    private Integer delaySeconds;

    @JsonProperty("lastSubReqNumber")
    private Integer lastSubReqNumber;

    @JsonProperty("rootId")
    private Integer rootId;

    @JsonProperty("countRetry")
    private Integer countRetry;

    @JsonProperty("created")
    private String created;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("searchQueryReference")
    private SearchQueryReference searchQueryReference;

    @JsonProperty("requestFlow")
    private String requestFlow;

    @JsonProperty("message")
    private Message message;

    @JsonProperty("updated")
    private String updated;

    @JsonProperty("taskId")
    private Integer taskId;

    @JsonProperty("status")
    private String status;
}
