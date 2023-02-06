package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class DAMAGE extends Cell {
    String prefix = "DAMAGE";
    LocationType locationType = LocationType.OTHER;
    String locationFlag = "DAMAGE";
    String locationStatus = "HOLD";
    boolean loseId = false;
    @NonNull
    String zone;
}
