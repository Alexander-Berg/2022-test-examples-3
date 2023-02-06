package dto.responses.logplatform.admin.station_tag_list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PickupVolumeTag {

    @JsonProperty("weight_by_day")
    private List<Integer> weightByDay;

    @JsonProperty("volume_by_day")
    private List<Long> volumeByDay;
}
