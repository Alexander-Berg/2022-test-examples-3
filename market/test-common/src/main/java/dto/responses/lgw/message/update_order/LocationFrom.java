package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LocationFrom {

    @JsonProperty("country")
    private String country;

    @JsonProperty("zipCode")
    private String zipCode;

    @JsonProperty("lng")
    private double lng;

    @JsonProperty("housing")
    private String housing;

    @JsonProperty("locality")
    private String locality;

    @JsonProperty("house")
    private String house;

    @JsonProperty("building")
    private String building;

    @JsonProperty("room")
    private String room;

    @JsonProperty("settlement")
    private String settlement;

    @JsonProperty("street")
    private String street;

    @JsonProperty("locationId")
    private int locationId;

    @JsonProperty("region")
    private String region;

    @JsonProperty("lat")
    private double lat;
}
