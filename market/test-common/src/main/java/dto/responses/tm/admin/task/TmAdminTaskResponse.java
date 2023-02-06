package dto.responses.tm.admin.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TmAdminTaskResponse {

    @JsonProperty("item")
    private Item item;

}
