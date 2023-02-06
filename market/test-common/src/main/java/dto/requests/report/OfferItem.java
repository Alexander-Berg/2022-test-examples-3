package dto.requests.report;

import java.util.List;

import dto.Item;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OfferItem {

    private Long shopId;
    private List<Item> items;

    public Integer getWarehouseId() {
        return items.stream()
            .map(Item::getWarehouseId)
            .findAny()
            .orElseThrow(() -> new AssertionError("Нет warehouseId у итемов с shopId" + shopId));
    }
}
