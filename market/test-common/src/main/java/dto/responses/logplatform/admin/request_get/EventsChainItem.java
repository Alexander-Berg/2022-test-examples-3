package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EventsChainItem {

    @JsonProperty("node_details")
    private NodeDetails nodeDetails;

    @JsonProperty("reservation_details")
    private ReservationDetails reservationDetails;

    @JsonProperty("type")
    private String type;

    @JsonProperty("action_details")
    private ActionDetails actionDetails;

    @JsonProperty("transfer_details")
    private TransferDetails transferDetails;
}
