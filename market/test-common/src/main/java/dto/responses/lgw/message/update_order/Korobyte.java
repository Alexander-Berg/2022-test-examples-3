package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Korobyte {

    @JsonProperty("weightGross")
    private int weightGross;

    @JsonProperty("width")
    private int width;

    @JsonProperty("length")
    private int length;

    @JsonProperty("height")
    private int height;
}
