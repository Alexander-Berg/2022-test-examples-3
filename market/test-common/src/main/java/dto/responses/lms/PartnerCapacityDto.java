package dto.responses.lms;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import lombok.Data;

import ru.yandex.market.logistics.management.entity.type.CapacityService;
import ru.yandex.market.logistics.management.entity.type.CapacityType;
import ru.yandex.market.logistics.management.entity.type.CountingType;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

@Data
public class PartnerCapacityDto {

    private Long id;

    @NotNull
    private Long partnerId;

    @NotNull
    private Integer locationFrom;

    @NotNull
    private Integer locationTo;

    private DeliveryType deliveryType;

    @NotNull
    private CapacityType type;

    @NotNull
    private CountingType countingType;

    private CapacityService capacityService;

    private Long platformClientId;

    private LocalDate day;

    @NotNull
    private Long value;
}
