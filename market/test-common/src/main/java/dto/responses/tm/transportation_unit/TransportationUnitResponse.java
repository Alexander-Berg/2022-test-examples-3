package dto.responses.tm.transportation_unit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TransportationUnitResponse {

    @JsonProperty("transportationId")
    private Integer transportationId;

    @JsonProperty("plannedIntervalEnd")
    private String plannedIntervalEnd;

    @JsonProperty("actualDateTime")
    private Object actualDateTime;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("plannedIntervalStart")
    private String plannedIntervalStart;

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("requestId")
    private Long requestId;

    @JsonProperty("registers")
    private List<RegistersItem> registers;

    @JsonProperty("changedAt")
    private String changedAt;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("logisticPointId")
    private Long logisticPointId;

    @JsonProperty("status")
    private String status;
}
