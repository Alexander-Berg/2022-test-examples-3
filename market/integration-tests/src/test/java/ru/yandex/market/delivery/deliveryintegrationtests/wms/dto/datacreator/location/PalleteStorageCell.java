package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class PalleteStorageCell extends Cell {
    @NonNull
    String zone;
    String prefix = "ATPS";
    LocationType locationType = LocationType.OTHER;
    boolean loseId = false;
}
