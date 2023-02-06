package dto.requests.mock;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetOrderInstancesData {

    String yandexId;
    String ffTrackCode;
    Long supplierId;

    List<GetOrderInstancesItem> items;

    @Value
    @Builder
    public static class GetOrderInstancesItem {
        String shopSku;
        BigDecimal price;
        String uit;
        String cis;
    }
}
