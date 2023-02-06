package dto.responses.scapi.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Warehouse {

    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    private int id;
}
