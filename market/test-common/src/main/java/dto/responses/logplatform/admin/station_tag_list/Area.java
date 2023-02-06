package dto.responses.logplatform.admin.station_tag_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Area {

    @JsonProperty("station_area_object")
    private StationAreaObject stationAreaObject;

    @JsonProperty("class_name")
    private String className;
}
