package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReservationDetails {

    @JsonProperty("reservation_id")
    private String reservationId;

    @JsonProperty("reserve_take_ts")
    private int reserveTakeTs;

    @JsonProperty("input_carriage_id")
    private String inputCarriageId;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("external_order_id")
    private String externalOrderId;

    @JsonProperty("reserve_put_ts")
    private int reservePutTs;

    @JsonProperty("internal_place_id")
    private String internalPlaceId;

    @JsonProperty("output_carriage_id")
    private String outputCarriageId;

    @JsonProperty("node_id")
    private String nodeId;

    @JsonProperty("status")
    private String status;
}
