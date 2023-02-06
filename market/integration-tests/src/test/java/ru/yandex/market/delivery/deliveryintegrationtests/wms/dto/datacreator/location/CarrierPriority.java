package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@Builder
public class CarrierPriority {
    private final String carrierCode;
    private final int priority;
}
