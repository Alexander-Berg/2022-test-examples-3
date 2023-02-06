package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class TurnoverReplBuffer extends Cell {
    @NonNull
    String zone;
    String prefix = "ATPB";
    LocationType locationType = LocationType.REPLENISHMENT_BUF;
    boolean loseId = false;
}
