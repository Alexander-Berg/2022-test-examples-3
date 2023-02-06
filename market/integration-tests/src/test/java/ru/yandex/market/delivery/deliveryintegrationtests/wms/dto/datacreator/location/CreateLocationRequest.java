package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.Builder;
import lombok.Data;

import ru.yandex.market.wms.common.model.enums.LocStatus;
import ru.yandex.market.wms.common.model.enums.LocationFlag;
import ru.yandex.market.wms.common.model.enums.LocationType;


@Data
@Builder
public class CreateLocationRequest {
    private final String prefix;
    private final String code;
    private final LocationType locationType;
    private final LocationFlag locationFlag;
    private final LocStatus locationStatus;
    private final boolean loseId;
    private final String zone;
    private final String vghLoc;
    private final String vghLocPrefix;
}
