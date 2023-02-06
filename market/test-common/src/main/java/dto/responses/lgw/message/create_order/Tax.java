package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tax {

    @JsonProperty("type")
    private String type;

    @JsonProperty("value")
    private Integer value;
}
