package dto.responses.lavka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CustomerMeta {

    @JsonProperty("country")
    private String country;

    @JsonProperty("metro")
    private String metro;

    @JsonProperty("porch")
    private String porch;

    @JsonProperty("street")
    private String street;

    @JsonProperty("locality")
    private String locality;

    @JsonProperty("intercom")
    private String intercom;

    @JsonProperty("floor")
    private String floor;

    @JsonProperty("region")
    private String region;

    @JsonProperty("house")
    private String house;

    @JsonProperty("room")
    private String room;

    @JsonProperty("settlement")
    private String settlement;
}
