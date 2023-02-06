package dto.responses.logplatform.admin.station_list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TimetableInput {

    @JsonProperty("restrictions")
    private List<RestrictionsItem> restrictions;

    @JsonProperty("time_zone")
    private int timeZone;
}
