package dto.responses.tm.admin.movement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ItemsItem {

    @JsonProperty("values")
    private Values values;

    @JsonProperty("id")
    private Integer id;
}
