package ru.yandex.market.delivery.tracker;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceRole;
import ru.yandex.market.delivery.tracker.domain.enums.TrackingPhase;
import ru.yandex.market.delivery.tracker.service.tracking.TrackingDurationService;

import static ru.yandex.market.delivery.tracker.service.tracking.TrackingDurationService.PHASES_DAYS_BY_DEFAULT;
import static ru.yandex.market.delivery.tracker.service.tracking.TrackingDurationService.TRACKING_DAYS_NO_CHECKPOINTS;

class TrackingDurationServiceTest extends AbstractContextualTest {

    @Autowired
    private TrackingDurationService service;

    @Test
    @DatabaseSetup("/database/states/empty_DB.xml")
    void testEmpty() {
        // no checkpoints for track
        Assertions.assertEquals(
            TRACKING_DAYS_NO_CHECKPOINTS,
            service.calcAllPhasesDurationInDays(null, DeliveryServiceRole.UNKNOWN, null)
        );

        // no configs + no default duration in DB (null, null, days)
        Assertions.assertEquals(
            PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.FIRST) + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
            service.calcAllPhasesDurationInDays(1L, DeliveryServiceRole.UNKNOWN, 1)
        );
    }

    @SuppressWarnings("unused")
    @ParameterizedTest(name = "{0}")
    @MethodSource("onlyStatusesKeyFilled")
    @DatabaseSetup("/database/states/tracking_duration/only_statuses_filled.xml")
    void testOnlyOneKeyFilled(
        String name,
        int expectedDays,
        Integer status
    ) {
        Assertions.assertEquals(
            expectedDays,
            service.calcAllPhasesDurationInDays(null, DeliveryServiceRole.UNKNOWN, status)
        );
    }

    @Nonnull
    private static Stream<Arguments> onlyStatusesKeyFilled() {
        return Stream.of(
            Arguments.of(
                "no checkpoints for track",
                TRACKING_DAYS_NO_CHECKPOINTS,
                null
            ),
            Arguments.of(
                "(null, null, 20) applied. Configs only for 42, 105, 130 statuses",
                20 + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
                1
            ),
            Arguments.of(
                "80 days for 130 status",
                80 + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
                130
            ),
            Arguments.of(
                "79 days for 105 status",
                79 + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
                105
            )
        );
    }

    @SuppressWarnings("unused")
    @ParameterizedTest(name = "{0}")
    @MethodSource("onlyStatusesAndDeliveryServiceIdsKeysFilled")
    @DatabaseSetup("/database/states/tracking_duration/ds_statuses_filled.xml")
    void testOnlyBothKeysFilled(
        String name,
        int expectedDays,
        Long deliveryServiceId,
        Integer status
    ) {
        Assertions.assertEquals(
            expectedDays,
            service.calcAllPhasesDurationInDays(deliveryServiceId, DeliveryServiceRole.UNKNOWN, status)
        );
    }

    @Nonnull
    private static Stream<Arguments> onlyStatusesAndDeliveryServiceIdsKeysFilled() {
        return Stream.of(
            Arguments.of(
                "no checkpoints for track, even with config for 145 ds",
                TRACKING_DAYS_NO_CHECKPOINTS,
                145L,
                null
            ),
            Arguments.of(
                "81 days for 145 ds and 130 status",
                81 + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
                145L,
                130
            ),
            Arguments.of(
                "82 days for 172 ds and 130 status",
                82 + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
                172L,
                130
            ),
            Arguments.of(
                "100 days for 172 ds",
                100 + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
                172L,
                129
            ),
            Arguments.of(
                "80 days for 130 status",
                80 + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
                1L,
                130
            ),
            Arguments.of(
                "42 days for 42 ds, not 666. DS has higher priority than STATUS",
                42 + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
                42L,
                42
            ),
            Arguments.of(
                "(null, null, 20) applied. Configs only for 42, 130 statuses and 42, 145, 172 ds",
                20 + PHASES_DAYS_BY_DEFAULT.get(TrackingPhase.SECOND),
                1L,
                1
            )
        );
    }

    @SuppressWarnings("unused")
    @ParameterizedTest(name = "{0}")
    @MethodSource("bothAllKeysFilledWithDifferentPhases")
    @DatabaseSetup("/database/states/tracking_duration/ds_status_and_different_phases_filled.xml")
    void testBothKeysFilledWithDifferentPhases(
        String name,
        int expectedDays,
        Long deliveryServiceId,
        DeliveryServiceRole role,
        Integer status
    ) {
        Assertions.assertEquals(expectedDays, service.calcAllPhasesDurationInDays(deliveryServiceId, role, status));
    }

    @Nonnull
    private static Stream<Arguments> bothAllKeysFilledWithDifferentPhases() {
        return Stream.of(
            Arguments.of(
                "no checkpoints for track, even with config for 145 ds",
                TRACKING_DAYS_NO_CHECKPOINTS,
                145L,
                DeliveryServiceRole.UNKNOWN,
                null
            ),
            Arguments.of(
                "81 days phase 1 for 145 ds and 130 status + 119 days phase 2 for 130 status",
                81 + 119,
                145L,
                DeliveryServiceRole.UNKNOWN,
                130
            ),
            Arguments.of(
                "82 days phase 1 for 172 ds and 130 status + 119 days phase 2 for 172ds",
                82 + 120,
                172L,
                DeliveryServiceRole.FULFILLMENT,
                130
            ),
            Arguments.of(
                "80 days phase 1 for 130 status + 119 days phase 2 for 130 status",
                80 + 119,
                1L,
                DeliveryServiceRole.UNKNOWN,
                130
            ),
            Arguments.of(
                "0 days phase 1 after 120 status from dropship + 0 days as default phase 2 for 120 status",
                0,
                20L,
                DeliveryServiceRole.DROPSHIP,
                120
            ),
            Arguments.of(
                "0 days phase 1 after 120 status from dropship + 0 days as default phase 2 for 120 status",
                0,
                null,
                DeliveryServiceRole.DROPSHIP,
                120
            ),
            Arguments.of(
                "30 as default for TRACKING_DAYS_NO_CHECKPOINTS and role not null",
                30,
                null,
                DeliveryServiceRole.DROPSHIP,
                null
            ),
            Arguments.of(
                "30 as default for TRACKING_DAYS_NO_CHECKPOINTS and role not null",
                30,
                20L,
                DeliveryServiceRole.DROPSHIP,
                null
            ),
            Arguments.of(
                "(null, null, 20) applied for phase 1 + 121 days phase 2 for 222 ds and 40 status",
                20 + 121,
                222L,
                DeliveryServiceRole.UNKNOWN,
                40
            )
        );
    }
}
