package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Area {
    String areaKey;
    List<String> zones = new ArrayList<>();
    List<String> sortingStations = new ArrayList<>();
    List<String> cells = new ArrayList<>();
}
