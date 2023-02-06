package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class AnomalyConsolidationCell extends Cell {
    @NonNull
    String zone;
    String prefix = "ATANC";
    LocationType locationType = LocationType.ANO_CONSOLIDATION;
    boolean loseId = false;
}

