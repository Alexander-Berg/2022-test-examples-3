package dto.responses.lgw.message.update_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScheduleItem {

    @JsonProperty("periods")
    private List<String> periods;

    @JsonProperty("day")
    private int day;
}
