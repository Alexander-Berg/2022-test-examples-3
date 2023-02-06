package dto.responses.lgw.message.get_order;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Order {
    private OrderInfo order;

    @Data
    public static class Item {
        private List<Map<String, String>> instances;
    }

    @Data
    public static class OrderInfo {
        private List<Item> items;
    }
}
