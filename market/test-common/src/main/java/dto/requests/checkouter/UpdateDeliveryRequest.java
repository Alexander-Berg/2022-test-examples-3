package dto.requests.checkouter;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateDeliveryRequest {
    private long deliveryServiceId;
}

