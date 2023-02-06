package dto.requests.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class To {

    @JsonProperty("location")
    private String location;
}
