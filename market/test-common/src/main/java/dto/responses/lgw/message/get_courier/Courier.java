package dto.responses.lgw.message.get_courier;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Courier {

    @JsonProperty("persons")
    private List<PersonsItem> persons;

    @JsonProperty("car")
    private Car car;
}
