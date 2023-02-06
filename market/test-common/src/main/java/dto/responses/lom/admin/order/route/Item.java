package dto.responses.lom.admin.order.route;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Item {

    @JsonProperty("values")
    private Values values;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("title")
    private String title;
}
