package dto.responses.tm.admin.transportation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;

@Data
public class Values {

    @JsonProperty("movingPartnerId")
    private MovingPartnerId movingPartnerId;

    @JsonProperty("movementSource")
    private String movementSource;

    @JsonProperty("plannedLaunchTime")
    private String plannedLaunchTime;

    @JsonProperty("startrekTicket")
    private StartrekTicket startrekTicket;

    @JsonProperty("inboundLegal")
    private InboundLegal inboundLegal;

    @JsonProperty("created")
    private String created;

    @JsonProperty("targetPartnerId")
    private TargetPartnerId targetPartnerId;

    @JsonProperty("movingLegal")
    private MovingLegal movingLegal;

    @JsonProperty("targetLogisticsPointId")
    private TargetLogisticsPointId targetLogisticsPointId;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("inboundPartnerId")
    private InboundPartnerId inboundPartnerId;

    @JsonProperty("outboundLegal")
    private OutboundLegal outboundLegal;

    @JsonProperty("adminTransportationStatus")
    private TransportationStatus adminTransportationStatus;

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

    @JsonProperty("validationErrors")
    private String validationErrors;

    @JsonProperty("inboundLogisticPointId")
    private InboundLogisticPointId inboundLogisticPointId;

    @JsonProperty("updated")
    private String updated;

    @JsonProperty("regular")
    private boolean regular;
}
