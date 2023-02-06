package dto.responses.lgw.message.create_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleItem {

    @JsonProperty("periods")
    private List<String> periods;

    @JsonProperty("day")
    private String day;
}
