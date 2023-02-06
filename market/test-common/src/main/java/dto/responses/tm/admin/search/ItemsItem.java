package dto.responses.tm.admin.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ItemsItem {

    @JsonProperty("values")
    private Values values;

    @JsonProperty("id")
    private Long id;
}
