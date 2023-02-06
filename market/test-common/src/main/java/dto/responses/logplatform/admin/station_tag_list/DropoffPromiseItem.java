package dto.responses.logplatform.admin.station_tag_list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DropoffPromiseItem {

    @JsonProperty("area")
    private Area area;

    @JsonProperty("deliveries_limit")
    private int deliveriesLimit;

    @JsonProperty("intervals")
    private List<String> intervals;

    @JsonProperty("delivering_policy")
    private String deliveringPolicy;
}
