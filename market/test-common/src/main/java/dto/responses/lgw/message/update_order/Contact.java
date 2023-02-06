package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Contact {

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("name")
    private String name;
}
