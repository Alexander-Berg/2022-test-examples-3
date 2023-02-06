package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SorterExit {

    String sorterExitKey;
    @Builder.Default
    String prefix = "SR-";
    String zone;
    boolean isAlternateExit;
    boolean isErrorExit;
}
