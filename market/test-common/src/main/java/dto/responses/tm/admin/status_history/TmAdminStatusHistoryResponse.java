package dto.responses.tm.admin.status_history;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TmAdminStatusHistoryResponse {

    @JsonProperty("totalCount")
    private int totalCount;

    @JsonProperty("items")
    private List<ItemsItem> items;

}
