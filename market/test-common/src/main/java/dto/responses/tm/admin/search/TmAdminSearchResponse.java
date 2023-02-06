package dto.responses.tm.admin.search;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TmAdminSearchResponse {

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("items")
    private List<ItemsItem> items;
}
