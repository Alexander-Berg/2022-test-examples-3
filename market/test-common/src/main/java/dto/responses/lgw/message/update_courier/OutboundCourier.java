package dto.responses.lgw.message.update_courier;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OutboundCourier {

    @JsonProperty("persons")
    private List<PersonsItem> persons;

    @JsonProperty("car")
    private Car car;
}
