package dto.responses.tm.admin.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;

@Data
public class Values {

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("registerId")
    private RegisterId registerId;

    @JsonProperty("created")
    private String created;

    @JsonProperty("logisticPointFromId")
    private LogisticPointFromId logisticPointFromId;

    @JsonProperty("deniedRegisterId")
    private DeniedRegisterId deniedRegisterId;

    @JsonProperty("validationErrors")
    private String validationErrors;

    @JsonProperty("logisticPointToId")
    private LogisticPointToId logisticPointToId;

    @JsonProperty("updated")
    private String updated;

    @JsonProperty("status")
    private String status;

    @JsonProperty("adminTransportationStatus")
    private TransportationStatus adminTransportationStatus;
}
