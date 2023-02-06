package dto.responses.tm.admin.movement;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TmAdminMovementResponse {

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("items")
    private List<ItemsItem> items;
}
