package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Korobyte {

    @JsonProperty("weightGross")
    private Integer weightGross;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("length")
    private Integer length;

    @JsonProperty("height")
    private Integer height;
}
