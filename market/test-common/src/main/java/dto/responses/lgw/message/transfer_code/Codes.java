package dto.responses.lgw.message.transfer_code;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Codes {

    @JsonProperty("inbound")
    private Inbound inbound;

    @JsonProperty("outbound")
    private Outbound outbound;
}
