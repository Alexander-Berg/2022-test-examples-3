package dto.responses.lgw.message.update_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Warehouse {

    @JsonProperty("schedule")
    private List<ScheduleItem> schedule;

    @JsonProperty("address")
    private Address address;

    @JsonProperty("instruction")
    private String instruction;

    @JsonProperty("contact")
    private Contact contact;

    @JsonProperty("phones")
    private List<PhonesItem> phones;

    @JsonProperty("id")
    private Id id;
}
