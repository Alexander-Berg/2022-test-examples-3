package dto.responses.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Delivery {

    @JsonProperty("calculatedDeliveryDateMax")
    private String calculatedDeliveryDateMax;

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("calculatedDeliveryDateMin")
    private String calculatedDeliveryDateMin;

    @JsonProperty("courierSchedule")
    private Object courierSchedule;

    @JsonProperty("type")
    private String type;
}
