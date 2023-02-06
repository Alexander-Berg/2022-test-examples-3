package dto.responses.scintmanualcells;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GetCellsResponse {

    @JsonProperty("cells")
    private List<CellsItem> cells;

    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    private int id;
}
