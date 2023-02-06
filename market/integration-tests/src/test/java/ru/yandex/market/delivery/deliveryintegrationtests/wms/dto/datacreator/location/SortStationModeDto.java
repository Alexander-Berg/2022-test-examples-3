package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SortStationModeDto {

    private final String station;
    private final String mode;
}
