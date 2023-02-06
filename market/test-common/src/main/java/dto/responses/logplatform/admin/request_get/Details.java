package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Details {

    @JsonProperty("country")
    private String country;

    @JsonProperty("federal_district")
    private String federalDistrict;

    @JsonProperty("street")
    private String street;

    @JsonProperty("porch")
    private String porch;

    @JsonProperty("locality")
    private String locality;

    @JsonProperty("intercom")
    private String intercom;

    @JsonProperty("floor")
    private String floor;

    @JsonProperty("sub_region")
    private String subRegion;

    @JsonProperty("region")
    private String region;

    @JsonProperty("house")
    private String house;

    @JsonProperty("room")
    private String room;

    @JsonProperty("geo_id")
    private int geoId;
}
