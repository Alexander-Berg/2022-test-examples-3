package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import lombok.Data;

import javax.annotation.ParametersAreNonnullByDefault;

@Data
@ParametersAreNonnullByDefault
public class InboundTable {
    private final String stageCell;
    private final String obmCell;
    private final String doorCell;
    private final String damageCall;
}
