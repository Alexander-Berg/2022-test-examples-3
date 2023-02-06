package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class ConveyorLocation extends Cell {
    @NonNull
    String zone;
    String prefix = "PB";
    @NonNull
    LocationType locationType;
    boolean loseId = true;
    String transporterLoc;
}
