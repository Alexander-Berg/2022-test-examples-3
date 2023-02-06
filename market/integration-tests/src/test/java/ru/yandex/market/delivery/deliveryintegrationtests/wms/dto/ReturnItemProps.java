package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReturnItemProps {
    boolean cisRequired;
    boolean clickNoCisButtonAfterCisEntering;
    TypeOfDamaged typeOfDamaged;
}
