package dto.responses.checkouter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReturnResponse {

    @JsonProperty("id")
    Long id;
}
