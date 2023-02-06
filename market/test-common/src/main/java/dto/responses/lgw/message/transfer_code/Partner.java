package dto.responses.lgw.message.transfer_code;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Partner {

    @JsonProperty("id")
    private Long id;
}
