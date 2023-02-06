package dto.responses.lgw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LgwTaskItem {

    @JsonProperty("values")
    private LgwTask values;

    @JsonProperty("id")
    private Long id;
}
