package dto.responses.lgw;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class TasksResponse {

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("items")
    private List<LgwTaskItem> items;
}
