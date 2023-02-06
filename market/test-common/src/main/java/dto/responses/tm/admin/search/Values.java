package dto.responses.tm.admin.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Values {

    @JsonProperty("movingPartnerId")
    private MovingPartnerId movingPartnerId;

    @JsonProperty("plannedLaunchTime")
    private String plannedLaunchTime;

    @JsonProperty("movementSource")
    private String movementSource;

    @JsonProperty("created")
    private String created;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("inboundPartnerId")
    private InboundPartnerId inboundPartnerId;

    @JsonProperty("planned")
    private String planned;

    @JsonProperty("adminTransportationStatus")
    private String adminTransportationStatus;

    @JsonProperty("outboundLogisticPointId")
    private OutboundLogisticPointId outboundLogisticPointId;

    @JsonProperty("movementType")
    private String movementType;

    @JsonProperty("outboundPartnerId")
    private OutboundPartnerId outboundPartnerId;

    @JsonProperty("adminTransportationScheme")
    private String adminTransportationScheme;

    @JsonProperty("adminTransportationType")
    private String adminTransportationType;

    @JsonProperty("type")
    private String type;

    @JsonProperty("status")
    private String status;

    @JsonProperty("inboundLogisticPointId")
    private InboundLogisticPointId inboundLogisticPointId;

    @JsonProperty("regular")
    private Boolean regular;
}
