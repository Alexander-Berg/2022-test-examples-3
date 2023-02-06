package dto.responses.scintmanualcells;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CellsItem {

    @JsonProperty("scNumber")
    private String scNumber;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("empty")
    private boolean empty;
}
