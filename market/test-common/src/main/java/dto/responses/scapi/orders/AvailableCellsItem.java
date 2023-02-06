package dto.responses.scapi.orders;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AvailableCellsItem {

    @JsonProperty("number")
    private String number;

    @JsonProperty("subType")
    private String subType;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("status")
    private String status;
}
