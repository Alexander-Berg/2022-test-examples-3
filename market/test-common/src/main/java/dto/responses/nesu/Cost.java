package dto.responses.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Cost {

    @JsonProperty("deliveryForCustomer")
    private Integer deliveryForCustomer;

    @JsonProperty("delivery")
    private Double delivery;

    @JsonProperty("total")
    private Object total;

    @JsonProperty("deliveryForSender")
    private Integer deliveryForSender;

    @JsonProperty("assessedValue")
    private Object assessedValue;
}
