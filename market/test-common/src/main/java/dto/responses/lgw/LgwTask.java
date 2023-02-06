package dto.responses.lgw;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class LgwTask {

    @JsonProperty("delaySeconds")
    private Integer delaySeconds;

    @JsonProperty("rootId")
    private Integer rootId;

    @JsonProperty("countRetry")
    private Integer countRetry;

    @JsonProperty("created")
    private String created;

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("requestFlow")
    private String requestFlow;

    @JsonProperty("updated")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime updated;

    @JsonProperty("taskId")
    private Integer taskId;

    @JsonProperty("parentId")
    private Integer parentId;

    @JsonProperty("consumer")
    private String consumer;

    @JsonProperty("status")
    private String status;
}
