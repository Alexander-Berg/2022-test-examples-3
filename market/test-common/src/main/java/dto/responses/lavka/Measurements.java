package dto.responses.lavka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Measurements {

    @JsonProperty("width")
    private int width;

    @JsonProperty("length")
    private int length;

    @JsonProperty("weight")
    private int weight;

    @JsonProperty("height")
    private int height;
}
