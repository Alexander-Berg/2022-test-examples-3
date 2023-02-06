package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class VGH extends Cell {
    String prefix = "STAGEOBM";
    LocationType locationType = LocationType.VGH;
    boolean loseId = false;
    @NonNull
    String zone;
}
