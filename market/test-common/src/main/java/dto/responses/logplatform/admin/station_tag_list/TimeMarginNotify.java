package dto.responses.logplatform.admin.station_tag_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TimeMarginNotify {

    @JsonProperty("format_h_r")
    private String formatHR;

    @JsonProperty("time_of_day")
    private String timeOfDay;

    @JsonProperty("days")
    private int days;

    @JsonProperty("direct_margin")
    private int directMargin;

    @JsonProperty("policy")
    private String policy;
}
