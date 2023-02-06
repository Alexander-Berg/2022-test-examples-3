package dto.responses.logplatform.admin.station_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RestrictionsItem {

    @JsonProperty("time_to")
    private int timeTo;

    @JsonProperty("time_from")
    private int timeFrom;

    @JsonProperty("day")
    private int day;
}
