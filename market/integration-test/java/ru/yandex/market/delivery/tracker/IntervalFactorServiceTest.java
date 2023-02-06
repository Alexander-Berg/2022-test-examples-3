package ru.yandex.market.delivery.tracker;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceRole;
import ru.yandex.market.delivery.tracker.domain.enums.TrackingPhase;
import ru.yandex.market.delivery.tracker.service.tracking.IntervalFactorService;

public class IntervalFactorServiceTest extends AbstractContextualTest {

    @Autowired
    private IntervalFactorService intervalFactorService;

    @Test
    @DatabaseSetup("/database/states/status_interval_factor/empty.xml")
    void testEmptyTable() {
        int factor = intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 2, TrackingPhase.FIRST);
        Assertions.assertEquals(1, factor);
        int factorSecondPhase =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 2, TrackingPhase.SECOND);
        Assertions.assertEquals(4, factorSecondPhase);

        int factorWithoutCheckpoints =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(1, factorWithoutCheckpoints);
        int factorWithoutCheckpointsSecondPhase =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.SECOND);
        Assertions.assertEquals(4, factorWithoutCheckpointsSecondPhase);

        int factorWithoutDeliveryService =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, 2, TrackingPhase.FIRST);
        Assertions.assertEquals(1, factorWithoutDeliveryService);
        int factorWithoutDeliveryServiceSecondPhase =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, 2, TrackingPhase.SECOND);
        Assertions.assertEquals(4, factorWithoutDeliveryServiceSecondPhase);

        int factorAllNulls =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(1, factorAllNulls);
        int factorAllNullsSecondPhase =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.SECOND);
        Assertions.assertEquals(4, factorAllNullsSecondPhase);
    }

    @Test
    @DatabaseSetup("/database/states/status_interval_factor/only_default_value.xml")
    void testOnlyDefaultValueInTable() {
        int factor = intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 2, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factor);

        int factorWithoutCheckpoints =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factorWithoutCheckpoints);

        int factorWithoutDeliveryService =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, 2, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factorWithoutDeliveryService);

        int factorAllNulls =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factorAllNulls);
    }

    @Test
    @DatabaseSetup("/database/states/status_interval_factor/only_default_status_factors.xml")
    void testOnlyDefaultStatusFactors() {
        int factor130 = intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 130, TrackingPhase.FIRST);
        int factor40 = intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 40, TrackingPhase.FIRST);
        int factor42 = intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 42, TrackingPhase.FIRST);
        Assertions.assertEquals(12, factor130);
        Assertions.assertEquals(4, factor40);
        Assertions.assertEquals(2, factor42);

        int factorWithoutCheckpoints =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factorWithoutCheckpoints);

        int factorWithoutDeliveryService =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, 130, TrackingPhase.FIRST);
        Assertions.assertEquals(12, factorWithoutDeliveryService);

        int factorAllNulls =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factorAllNulls);
    }

    @Test
    @DatabaseSetup("/database/states/status_interval_factor/only_default_ds_factors.xml")
    void testOnlyDefaultDeliveryServiceFactors() {
        int factor10 = intervalFactorService.getFactor(10L, DeliveryServiceRole.UNKNOWN, 1, TrackingPhase.FIRST);
        int factor11 = intervalFactorService.getFactor(11L, DeliveryServiceRole.UNKNOWN, 2, TrackingPhase.FIRST);
        int factor404 = intervalFactorService.getFactor(404L, DeliveryServiceRole.UNKNOWN, 3, TrackingPhase.FIRST);
        Assertions.assertEquals(100, factor10);
        Assertions.assertEquals(111, factor11);
        Assertions.assertEquals(2, factor404);

        int factorWithoutCheckpoints =
            intervalFactorService.getFactor(11L, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(111, factorWithoutCheckpoints);

        int factorWithoutDeliveryService =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, 130, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factorWithoutDeliveryService);

        int factorAllNulls =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factorAllNulls);
    }

    @Test
    @DatabaseSetup("/database/states/status_interval_factor/both_status_ds_factors.xml")
    void testBothFactors() {
        int ds1status10 = intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 10, TrackingPhase.FIRST);
        int ds2status20 = intervalFactorService.getFactor(2L, DeliveryServiceRole.UNKNOWN, 20, TrackingPhase.FIRST);
        int ds3status30 = intervalFactorService.getFactor(3L, DeliveryServiceRole.UNKNOWN, 30, TrackingPhase.FIRST);
        Assertions.assertEquals(111, ds1status10);
        Assertions.assertEquals(222, ds2status20);
        // pair (null delivery_service_id, status) has a higher priority than (delivery_service_id, null status)
        Assertions.assertEquals(300, ds3status30);

        int factorWithoutCheckpoints =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        int factorWithAnotherStatus =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 40, TrackingPhase.FIRST);
        Assertions.assertEquals(10, factorWithoutCheckpoints);
        Assertions.assertEquals(10, factorWithAnotherStatus);

        int factorWithoutDeliveryService =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, 20, TrackingPhase.FIRST);
        int factorWithAnotherDs =
            intervalFactorService.getFactor(4L, DeliveryServiceRole.UNKNOWN, 20, TrackingPhase.FIRST);
        Assertions.assertEquals(200, factorWithoutDeliveryService);
        Assertions.assertEquals(200, factorWithAnotherDs);

        int factorAllNulls =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factorAllNulls);
    }

    @Test
    @DatabaseSetup("/database/states/status_interval_factor/different_phase_factors.xml")
    void testBothKeysFilledWithDifferentPhases() {
        int factorWithoutCheckpoints =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        int factorWithAnotherStatus =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 40, TrackingPhase.FIRST);
        Assertions.assertEquals(10, factorWithoutCheckpoints);
        Assertions.assertEquals(10, factorWithAnotherStatus);
        int factorWithoutCheckpointsSecondPhase =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.SECOND);
        int factorWithAnotherStatusSecondPhase =
            intervalFactorService.getFactor(1L, DeliveryServiceRole.UNKNOWN, 40, TrackingPhase.SECOND);
        Assertions.assertEquals(40, factorWithoutCheckpointsSecondPhase);
        Assertions.assertEquals(40, factorWithAnotherStatusSecondPhase);

        int factorWithoutDeliveryService =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, 30, TrackingPhase.FIRST);
        int factorWithAnotherDs =
            intervalFactorService.getFactor(2L, DeliveryServiceRole.UNKNOWN, 20, TrackingPhase.FIRST);
        Assertions.assertEquals(300, factorWithoutDeliveryService);
        Assertions.assertEquals(200, factorWithAnotherDs);
        int factorWithoutDeliveryServiceSecondPhase =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, 30, TrackingPhase.SECOND);
        int factorWithAnotherDsSecondPhase =
            intervalFactorService.getFactor(2L, DeliveryServiceRole.UNKNOWN, 20, TrackingPhase.SECOND);
        Assertions.assertEquals(3030, factorWithoutDeliveryServiceSecondPhase);
        Assertions.assertEquals(2222, factorWithAnotherDsSecondPhase);

        int factorAllNulls =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.FIRST);
        Assertions.assertEquals(2, factorAllNulls);
        int factorAllNullsSecondPhase =
            intervalFactorService.getFactor(null, DeliveryServiceRole.UNKNOWN, null, TrackingPhase.SECOND);
        Assertions.assertEquals(8, factorAllNullsSecondPhase);
    }
}
