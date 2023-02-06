package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrderResponse {

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("order")
    private Order order;

    @JsonProperty("restrictedData")
    private RestrictedData restrictedData;
}
