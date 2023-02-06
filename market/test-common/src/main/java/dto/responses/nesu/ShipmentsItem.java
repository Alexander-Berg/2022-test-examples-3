package dto.responses.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShipmentsItem {

    @JsonProperty("date")
    private String date;

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("settingsDefault")
    private Boolean settingsDefault;

    @JsonProperty("type")
    private String type;

    @JsonProperty("warehouse")
    private Object warehouse;

    @JsonProperty("alreadyExists")
    private Object alreadyExists;
}
