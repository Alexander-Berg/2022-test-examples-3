package dto.requests.lms;

import lombok.AllArgsConstructor;
import lombok.Data;

import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

@Data
@AllArgsConstructor
public class CreateCapacityRequest {
    private CapacityService capacityService;
    private CountingType countingType;
    private DeliveryType deliveryType;
    private Integer locationFrom;
    private Integer capacityRegion;
    private Long partnerId;
    private Integer platformClientId;
    private CapacityType type;
    private Long value;
    private String day;

}
