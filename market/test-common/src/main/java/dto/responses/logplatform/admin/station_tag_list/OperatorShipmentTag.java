package dto.responses.logplatform.admin.station_tag_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OperatorShipmentTag {

    @JsonProperty("volume")
    private Long volume;

    @JsonProperty("weight")
    private int weight;

    @JsonProperty("shipment_id")
    private String shipmentId;
}
