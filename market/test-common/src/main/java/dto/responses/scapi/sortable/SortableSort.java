package dto.responses.scapi.sortable;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SortableSort {

    @JsonProperty("destination")
    private Destination destination;

    @JsonProperty("parentRequired")
    private boolean parentRequired;
}
