package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@RequiredArgsConstructor
public class ReceiptTable extends Cell {
    String prefix = "STAGE";
    LocationType locationType = LocationType.RECEIPT_TABLE;
    boolean loseId = false;
    @NonNull
    String zone;
    String vghLocPrefix = "STAGEOBM";
}
