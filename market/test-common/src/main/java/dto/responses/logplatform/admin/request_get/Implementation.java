package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Implementation {

    @JsonProperty("coord")
    private Coord coord;

    @JsonProperty("details")
    private Details details;

    @JsonProperty("class_name")
    private String className;

    @JsonProperty("station_id")
    private String stationId;
}
