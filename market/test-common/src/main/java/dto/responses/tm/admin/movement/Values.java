package dto.responses.tm.admin.movement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Values {

    @JsonProperty("idWithPrefix")
    private String idWithPrefix;

    @JsonProperty("created")
    private String created;

    @JsonProperty("plannedIntervalEnd")
    private String plannedIntervalEnd;

    @JsonProperty("externalId")
    private Object externalId;

    @JsonProperty("transport")
    private Transport transport;

    @JsonProperty("partnerType")
    private String partnerType;

    @JsonProperty("partnerId")
    private PartnerId partnerId;

    @JsonProperty("plannedIntervalStart")
    private String plannedIntervalStart;

    @JsonProperty("updated")
    private String updated;

    @JsonProperty("trackerFilter")
    private TrackerFilter trackerFilter;

    @JsonProperty("status")
    private String status;

    @JsonProperty("lgwFilter")
    private LgwFilter lgwFilter;
}
