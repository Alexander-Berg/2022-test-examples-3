package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class BbxdSortingCell extends Cell {
    @NonNull
    String zone;
    String prefix = "ATBXS";
    LocationType locationType = LocationType.BBXD_SORT_BUF;
    boolean loseId = false;
}
