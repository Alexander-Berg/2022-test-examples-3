package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
@Builder
public class SortingStation extends Cell {
    String station;
    @Builder.Default
    String prefix = "ATSO";
    String zone;
    @Builder.Default
    int prefixSortLocationNumber = 2;
    @Builder.Default
    List<String> sortLocations = Collections.emptyList();
    @Builder.Default
    LocationType locationType = LocationType.SORT;
}
