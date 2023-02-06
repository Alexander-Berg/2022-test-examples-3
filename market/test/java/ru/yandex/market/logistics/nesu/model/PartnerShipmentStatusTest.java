package ru.yandex.market.logistics.nesu.model;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;
import ru.yandex.market.logistics.nesu.facade.comparator.PartnerShipmentStatusPriorityExtractor;

@DisplayName("Проверка статусов отгрузок")
public class PartnerShipmentStatusTest extends AbstractTest {

    private static final int DEFAULT_STATUS_PRIORITY = 1000;

    @Test
    @DisplayName("У каждого не скрытого статуса установлен приоритет сортировки")
    void eachVisibleStatusHasSortingPriority() {
        var comparator = new PartnerShipmentStatusPriorityExtractor();
        Assertions.assertTrue(StreamEx.of(PartnerShipmentStatus.values())
            .filter(status -> !status.isHidden())
            .mapToInt(comparator)
            .noneMatch(priority -> priority == DEFAULT_STATUS_PRIORITY)
        );
    }
}
