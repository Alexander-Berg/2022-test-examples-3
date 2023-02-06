package dto.responses.logplatform.create_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateOrderResponse {
    @JsonProperty("offers")
    private List<OfferIdDto> offers;
}
