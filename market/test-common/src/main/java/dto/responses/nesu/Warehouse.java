package dto.responses.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Warehouse {

    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    private Long id;
}
