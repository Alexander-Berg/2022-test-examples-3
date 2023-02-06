package dto.responses.lom.admin.order.route;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RouteResponse {

    @JsonProperty("item")
    private Item item;

}
