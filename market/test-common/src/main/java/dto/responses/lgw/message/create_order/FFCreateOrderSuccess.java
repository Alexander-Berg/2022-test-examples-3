package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FFCreateOrderSuccess {

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("trackId")
    private String trackId;

    @JsonProperty("partner")
    private Partner partner;
}
