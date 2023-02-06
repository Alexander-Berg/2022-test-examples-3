package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import java.util.List;

@Value
@RequiredArgsConstructor
public class ZoneConfig {
    String zone;
    boolean enabled;
    int maxCongestionPercent;
    List<String> types;
}
