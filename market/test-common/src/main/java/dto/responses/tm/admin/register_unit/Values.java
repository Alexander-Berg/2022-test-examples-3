package dto.responses.tm.admin.register_unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Values {

    @JsonProperty("unitType")
    private String unitType;

    @JsonProperty("parentUnits")
    private String parentUnits;

    @JsonProperty("name")
    private String name;

    @JsonProperty("count")
    private String count;

    @JsonProperty("vendorId")
    private String vendorId;

    @JsonProperty("weight")
    private String weight;

    @JsonProperty("ssku")
    private String ssku;

    @JsonProperty("realVendorId")
    private String realVendorId;

    @JsonProperty("barcode")
    private String barcode;

    @JsonProperty("dimensions")
    private String dimensions;
}
