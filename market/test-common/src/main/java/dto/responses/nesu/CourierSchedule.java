package dto.responses.nesu;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CourierSchedule {

    @JsonProperty("schedule")
    private List<ScheduleItem> schedule;

    @JsonProperty("locationId")
    private Integer locationId;
}
