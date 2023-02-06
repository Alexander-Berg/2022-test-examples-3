package dto.responses.lgw.message.cancel_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Partner {

    @JsonProperty("id")
    private int id;
}
