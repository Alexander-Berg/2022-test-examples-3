package dto.responses.tm.admin.register_unit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RegisterUnitResponse {

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("items")
    private List<ItemsItem> items;
}
