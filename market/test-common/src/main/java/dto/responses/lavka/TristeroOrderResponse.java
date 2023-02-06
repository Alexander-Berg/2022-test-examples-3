package dto.responses.lavka;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TristeroOrderResponse {

    @JsonProperty("uid")
    private String uid;

    @JsonProperty("customer_address")
    private String customerAddress;

    @JsonProperty("customer_meta")
    private CustomerMeta customerMeta;

    @JsonProperty("delivery_date")
    private String deliveryDate;

    @JsonProperty("vendor")
    private String vendor;

    @JsonProperty("depot_id")
    private String depotId;

    @JsonProperty("ref_order")
    private String refOrder;

    @JsonProperty("state")
    private String state;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("items")
    private List<LavkaItem> items;

    @JsonProperty("token")
    private String token;

    @JsonProperty("timeslot")
    private TimeSlot timeslot;
}
