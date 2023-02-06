package dto.responses.tm.admin.register_unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ItemsItem {

    @JsonProperty("values")
    private Values values;

    @JsonProperty("id")
    private Integer id;
}
