package dto.responses.lgw.message.get_courier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Car {

    @JsonProperty("number")
    private String number;

}
