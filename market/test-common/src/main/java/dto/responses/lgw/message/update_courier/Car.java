package dto.responses.lgw.message.update_courier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Car {

    @JsonProperty("number")
    private String number;
}
