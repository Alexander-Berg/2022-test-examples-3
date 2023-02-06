package dto.responses.lavka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LavkaItem {

    @JsonProperty("id")
    private String id;

    @JsonProperty("state")
    private String state;

    @JsonProperty("wms_id")
    private String wmsId;

    @JsonProperty("barcode")
    private String barcode;

    @JsonProperty("measurements")
    private Measurements measurements;

    @JsonProperty("state_meta")
    private StateMeta stateMeta;
}
