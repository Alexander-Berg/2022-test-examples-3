package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResourceBillingFeature {

    @JsonProperty("delivery_n_d_s")
    private int deliveryNDS;

    @JsonProperty("client_order_id")
    private String clientOrderId;

    @JsonProperty("delivery_cost")
    private int deliveryCost;

    @JsonProperty("payment_method")
    private String paymentMethod;
}
