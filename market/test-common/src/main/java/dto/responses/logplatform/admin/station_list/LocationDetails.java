package dto.responses.logplatform.admin.station_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LocationDetails {

    @JsonProperty("country")
    private String country;

    @JsonProperty("federal_district")
    private String federalDistrict;

    @JsonProperty("street")
    private String street;

    @JsonProperty("locality")
    private String locality;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("region")
    private String region;

    @JsonProperty("sub_region")
    private String subRegion;

    @JsonProperty("house")
    private String house;

    @JsonProperty("geo_id")
    private int geoId;

    @JsonProperty("settlement")
    private String settlement;
}
