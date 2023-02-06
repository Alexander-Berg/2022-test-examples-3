package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PartnerCodesItem {

    @JsonProperty("partnerCodeValue")
    private String partnerCodeValue;

    @JsonProperty("partnerId")
    private String partnerId;
}
