package dto.responses.lgw.message.get_order;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class GetOrderRequest {
    private OrderId orderId;
    private Partner partner;

    @Data
    @Accessors(chain = true)
    public static class OrderId {
        private String yandexId;
    }

    @Data
    @Accessors(chain = true)
    public static class Partner {
        private Long id;
    }
}
