package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Physical {

    @JsonProperty("length")
    private int length;

    @JsonProperty("width")
    private int width;

    @JsonProperty("weight_tare")
    private int weightTare;

    @JsonProperty("predefined_volume")
    private int predefinedVolume;

    @JsonProperty("weight_gross")
    private int weightGross;

    @JsonProperty("weight_net")
    private int weightNet;

    @JsonProperty("height")
    private int height;
}
