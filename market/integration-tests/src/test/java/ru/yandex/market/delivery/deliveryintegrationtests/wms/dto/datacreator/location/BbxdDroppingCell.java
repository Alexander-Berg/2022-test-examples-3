package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class BbxdDroppingCell extends Cell {
    @NonNull
    String zone;
    String prefix = "ATBXDRP";
    LocationType locationType = LocationType.DROP;
    boolean loseId = false;
}
