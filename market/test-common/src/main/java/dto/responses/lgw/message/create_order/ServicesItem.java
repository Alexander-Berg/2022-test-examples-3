package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServicesItem {

    @JsonProperty("code")
    private String code;

    @JsonProperty("isOptional")
    private Boolean isOptional;
}
