package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class DoorOut extends Cell {
    String prefix = "DOOROUT";
    LocationType locationType = LocationType.DOOR_OUT;
    boolean loseId = false;
    @NonNull
    String zone;
}
