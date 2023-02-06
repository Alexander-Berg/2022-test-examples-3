package dto.responses.lgw.message.get_courier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Partner {

    @JsonProperty("id")
    private int id;
}
