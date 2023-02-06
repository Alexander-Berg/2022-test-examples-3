package dto.responses.scint.manualcells;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScRoute {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("cells")
    private List<ScCell> cells;
}
