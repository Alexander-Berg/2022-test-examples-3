package dto.responses.trust;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TrustResponse {

    @JsonProperty("status")
    private String status;
}
