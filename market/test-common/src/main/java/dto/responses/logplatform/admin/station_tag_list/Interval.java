package dto.responses.logplatform.admin.station_tag_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Interval {

    @JsonProperty("hr_interval")
    private String hrInterval;

    @JsonProperty("from")
    private int from;

    @JsonProperty("to")
    private int to;
}
