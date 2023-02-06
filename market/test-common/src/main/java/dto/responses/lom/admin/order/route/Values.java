package dto.responses.lom.admin.order.route;

import com.fasterxml.jackson.annotation.JsonProperty;
import dto.responses.lom.admin.order.Route;
import lombok.Data;

@Data
public class Values {

    @JsonProperty("route")
    private Route route;

    @JsonProperty("order")
    private Order order;
}
