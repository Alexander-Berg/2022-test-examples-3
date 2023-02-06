package ru.yandex.market.logistics.management.facade;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.SettingsMethod;
import ru.yandex.market.logistics.management.queue.producer.PickupPointSyncProducer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@DatabaseSetup(
    value = "/data/repository/settings/partner_with_settings_and_methods.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class PartnerFacadeTest extends AbstractContextualTest {

    @Autowired
    private PickupPointSyncProducer pickupPointSyncProducer;

    @Autowired
    private PartnerFacade partnerFacade;

    @Test
    void syncPickupPoints_callsPickupPointSyncProducer() {
        partnerFacade.syncPickupPoints(1L, false);

        Mockito.verify(pickupPointSyncProducer).produceTask(any(SettingsMethod.class), eq(false));
    }

    @Test
    void syncPickupPoints_throwsWhenNoMethodFound() {
        Assertions.assertThrows(
            RuntimeException.class,
            () -> partnerFacade.syncPickupPoints(3L, false),
            "Cannot sync pickup points for partner 3: no getReferencePickupPoints method found"
        );

        Mockito.verifyZeroInteractions(pickupPointSyncProducer);
    }

    @Test
    void syncPickupPoints_throwsWhenMethodIsNotActive() {
        Assertions.assertThrows(
            RuntimeException.class,
            () -> partnerFacade.syncPickupPoints(2L, false),
            "Cannot sync: method ID 7 for partner ID 2 is not active"
        );

        Mockito.verifyZeroInteractions(pickupPointSyncProducer);
    }

    @Test
    void partitionedSearchDoesNotThrow() {
        List<Long> longIdList = IntStream.range(0, Short.MAX_VALUE)
            .mapToObj(Long::valueOf)
            .collect(Collectors.toList());
        assertDoesNotThrow(() -> partnerFacade.getPartnerCargoTypes(longIdList));
    }
}
