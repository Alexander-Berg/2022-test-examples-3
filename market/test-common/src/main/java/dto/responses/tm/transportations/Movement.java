package dto.responses.tm.transportations;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Movement {

    @JsonProperty("plannedIntervalEnd")
    private String plannedIntervalEnd;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("weight")
    private Integer weight;

    @JsonProperty("maxPallet")
    private Integer maxPallet;

    @JsonProperty("plannedIntervalStart")
    private String plannedIntervalStart;

    @JsonProperty("volume")
    private Object volume;

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("courier")
    private Object courier;

    @JsonProperty("isTrackable")
    private Boolean isTrackable;

    @JsonProperty("registers")
    private List<Object> registers;

    @JsonProperty("changedAt")
    private String changedAt;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("status")
    private String status;
}
