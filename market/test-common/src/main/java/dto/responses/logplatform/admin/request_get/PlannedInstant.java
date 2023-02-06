package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PlannedInstant {

    @JsonProperty("planned_instant")
    private PlannedInstant plannedInstant;

    @JsonProperty("wait_duration")
    private int waitDuration;

    @JsonProperty("planned_location")
    private PlannedLocation plannedLocation;

    @JsonProperty("mileage")
    private int mileage;

    @JsonProperty("min")
    private int min;

    @JsonProperty("max")
    private int max;
}
