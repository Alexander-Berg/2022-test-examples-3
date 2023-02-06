package dto.responses.logplatform.admin.station_tag_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StationAreaObject {

    @JsonProperty("station_id")
    private String stationId;
}
