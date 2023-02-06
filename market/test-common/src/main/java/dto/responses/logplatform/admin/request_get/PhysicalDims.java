package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PhysicalDims {

    @JsonProperty("weight_tare")
    private int weightTare;

    @JsonProperty("predefined_volume")
    private int predefinedVolume;

    @JsonProperty("d_x")
    private int dX;

    @JsonProperty("weight_gross")
    private int weightGross;

    @JsonProperty("d_z")
    private int dZ;

    @JsonProperty("weight_net")
    private int weightNet;

    @JsonProperty("d_y")
    private int dY;
}
