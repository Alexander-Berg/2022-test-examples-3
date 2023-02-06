package dto.responses.scapi.sortable;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Destination {

    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;
}
