package dto.responses.lms;

import java.util.List;

import lombok.Data;

import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;

@Data
public class LogisticSegmentServiceDto {
    private long id;
    private ActivityStatus status;
    private ServiceCodeName code;
    private List<CargoTypeDto> restrictedCargoTypes;
}
