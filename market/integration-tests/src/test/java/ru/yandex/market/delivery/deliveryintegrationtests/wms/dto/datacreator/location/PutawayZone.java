package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class PutawayZone {
    @NonNull
    String areaKey;
    String prefix = "ATZ";
    int maxAssignment = 100;
    @Builder.Default
    PutawayZoneType type = PutawayZoneType.UNDEFINED;
    @Nullable
    String building;
}
