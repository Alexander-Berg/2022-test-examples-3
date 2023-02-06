package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class PickingCell extends Cell {
    @NonNull
    String zone;
    String prefix = "ATO";
    LocationType locationType = LocationType.PICK;
    boolean loseId;

    public PickingCell(String zone) {
        this.zone = zone;
        this.loseId = true;
    }

    public PickingCell(String zone, boolean loseId) {
        this.zone = zone;
        this.loseId = loseId;
    }
}
