package dto.responses.tm.admin.status_history;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ItemsItem {

    @JsonProperty("values")
    private Values values;

    @JsonProperty("id")
    private int id;

}
