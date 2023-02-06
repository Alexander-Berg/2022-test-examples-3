package dto.responses.logplatform.admin.station_tag_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TagTableFieldsItem {

    @JsonProperty("name")
    private String name;

    @JsonProperty("title")
    private String title;

    @JsonProperty("order")
    private String order;
}
