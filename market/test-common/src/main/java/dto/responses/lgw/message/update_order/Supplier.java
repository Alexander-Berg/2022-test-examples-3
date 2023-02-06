package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Supplier {

    @JsonProperty("inn")
    private String inn;
}
