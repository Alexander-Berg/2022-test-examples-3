package dto.responses.lgw.message.update_items_instances;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateItemsInstancesResponse {

    @JsonProperty("partner")
    private Partner partner;

    @JsonProperty("orderId")
    private OrderId orderId;

    @JsonProperty("itemsInstances")
    private List<ItemsInstancesItem> itemsInstances;
}
