package dto.responses.nesu;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DeliveryOptionsItem {

    @JsonProperty("delivery")
    private Delivery delivery;

    @JsonProperty("cost")
    private Cost cost;

    @JsonProperty("tariffName")
    private String tariffName;

    @JsonProperty("pickupPointIds")
    private List<Long> pickupPointIds;

    @JsonProperty("disabledReasons")
    private Object disabledReasons;

    @JsonProperty("tariffId")
    private Long tariffId;

    @JsonProperty("services")
    private List<ServicesItem> services;

    @JsonProperty("shipments")
    private List<ShipmentsItem> shipments;

    @JsonProperty("tags")
    private List<String> tags;
}
