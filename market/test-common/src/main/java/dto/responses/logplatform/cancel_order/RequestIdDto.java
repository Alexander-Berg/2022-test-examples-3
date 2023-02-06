package dto.responses.logplatform.cancel_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RequestIdDto {
    @JsonProperty("request_id")
    private String requestId;
}
