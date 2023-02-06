package dto.responses.logplatform.admin.station_tag_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DropoffReservationsItem {

    @JsonProperty("area")
    private Area area;

    @JsonProperty("deliveries_limit")
    private int deliveriesLimit;

    @JsonProperty("delivering_policy")
    private String deliveringPolicy;

    @JsonProperty("interval")
    private Interval interval;

    @JsonProperty("quota_id")
    private String quotaId;
}
