package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class PackingTable extends Cell {
    @NonNull
    String zone;
    String prefix = "ATP";
    LocationType locationType = LocationType.PACK;
    boolean loseId = false;
}
