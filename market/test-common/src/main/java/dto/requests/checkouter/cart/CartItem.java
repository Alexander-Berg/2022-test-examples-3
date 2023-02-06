package dto.requests.checkouter.cart;

import java.util.List;

import dto.Item;
import dto.requests.checkouter.DeliveryDtoRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartItem {
    private Long shopId;
    private List<Item> items;
    private DeliveryDtoRequest delivery;
    private String notes;
}
