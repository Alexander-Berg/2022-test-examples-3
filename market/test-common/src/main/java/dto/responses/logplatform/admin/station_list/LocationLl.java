package dto.responses.logplatform.admin.station_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LocationLl {

    @JsonProperty("lon")
    private double lon;

    @JsonProperty("lat")
    private double lat;
}
