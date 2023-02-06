package dto.responses.scint.manualcells;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScCell {

    @JsonProperty("id")
    private Long id;
}
