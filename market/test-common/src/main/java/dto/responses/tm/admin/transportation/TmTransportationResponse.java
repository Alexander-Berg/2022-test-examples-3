package dto.responses.tm.admin.transportation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TmTransportationResponse {

    @JsonProperty("item")
    private Item item;

}
