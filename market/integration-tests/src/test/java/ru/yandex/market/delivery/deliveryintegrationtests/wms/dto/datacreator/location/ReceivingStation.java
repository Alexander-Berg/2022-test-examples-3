package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class ReceivingStation {
    String loc;
    boolean enabled;
    String defaultContainerSettings;
    boolean nestingEnabled;
}
