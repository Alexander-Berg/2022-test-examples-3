package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class WithdrawalReplBuffer extends Cell {
    @NonNull
    String zone;
    String prefix = "REPWD";
    LocationType locationType = LocationType.REPLENISHMENT_BUF_WITHDRAWAL;
    boolean loseId = false;

}
