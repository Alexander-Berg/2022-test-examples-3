package dto.responses.tm.admin.status_history;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Values {

    @JsonProperty("newStatus")
    private String newStatus;

    @JsonProperty("changedAt")
    private String changedAt;
}
