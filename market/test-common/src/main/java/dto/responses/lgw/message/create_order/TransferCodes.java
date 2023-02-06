package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import dto.responses.lgw.message.transfer_code.Outbound;
import lombok.Data;

@Data
public class TransferCodes {

    @JsonProperty("inbound")
    private Inbound inbound;

    @JsonProperty("outbound")
    private Outbound outbound;
}
