package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RestrictedData {

    @JsonProperty("transferCodes")
    private TransferCodes transferCodes;
}
