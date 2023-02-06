package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class BbxdShippingCell extends Cell {
    @NonNull
    String zone;
    String prefix = "ATSCBX";
    LocationType locationType = LocationType.SHIP_BBXD;
    boolean loseId = true;
}
