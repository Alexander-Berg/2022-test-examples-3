package dto.responses.idxapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IdxBucket {

    @JsonProperty("bucket_id (report)")
    private Integer bucketIdReport;

    @JsonProperty("bucket_id (delivery calc)")
    private Integer bucketIdDeliveryCalc;

    @JsonProperty("delivery_service_id")
    private Integer deliveryServiceId;
}
