package dto.responses.lom.admin.order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import ru.yandex.market.logistics.lom.model.dto.OrderDto;

@Data
public class OrdersResponse {

    @JsonProperty("data")
    private List<OrderDto> data;

    @JsonProperty("size")
    private Integer size;
}
