package dto.requests.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Dimensions {

    @JsonProperty("length")
    private Long length;

    @JsonProperty("width")
    private Long width;

    @JsonProperty("weight")
    private Long weight;

    @JsonProperty("height")
    private Long height;
}
