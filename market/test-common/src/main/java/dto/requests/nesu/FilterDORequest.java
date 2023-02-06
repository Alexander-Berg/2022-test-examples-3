package dto.requests.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class FilterDORequest {

    @JsonProperty("from")
    private From from;

    @JsonProperty("to")
    private To to;

    @JsonProperty("dimensions")
    private Dimensions dimensions;
}
