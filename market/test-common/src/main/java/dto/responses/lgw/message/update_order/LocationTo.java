package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LocationTo {

    @JsonProperty("country")
    private String country;

    @JsonProperty("zipCode")
    private String zipCode;

    @JsonProperty("subRegion")
    private String subRegion;

    @JsonProperty("federalDistrict")
    private String federalDistrict;

    @JsonProperty("lng")
    private double lng;

    @JsonProperty("porch")
    private String porch;

    @JsonProperty("locality")
    private String locality;

    @JsonProperty("house")
    private String house;

    @JsonProperty("room")
    private String room;

    @JsonProperty("street")
    private String street;

    @JsonProperty("locationId")
    private int locationId;

    @JsonProperty("intercom")
    private String intercom;

    @JsonProperty("region")
    private String region;

    @JsonProperty("floor")
    private int floor;

    @JsonProperty("lat")
    private double lat;
}
