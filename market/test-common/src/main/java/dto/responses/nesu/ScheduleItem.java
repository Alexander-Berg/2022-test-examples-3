package dto.responses.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleItem {

    @JsonProperty("timeFrom")
    private String timeFrom;

    @JsonProperty("timeTo")
    private String timeTo;

    @JsonProperty("id")
    private Integer id;
}
