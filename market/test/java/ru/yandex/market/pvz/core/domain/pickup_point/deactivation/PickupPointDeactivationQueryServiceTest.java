package ru.yandex.market.pvz.core.domain.pickup_point.deactivation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.calendar.override.PickupPointCalendarOverride;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.params.DeactivationParams;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.params.DeactivationReasonParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointDeactivationQueryServiceTest {

    private final TestableClock clock;
    private final PickupPointDeactivationQueryService deactivationQueryService;
    private final PickupPointDeactivationCommandService deactivationCommandService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final DeactivationReasonRepository deactivationReasonRepository;
    private final PickupPointDeactivationLogRepository pickupPointDeactivationLogRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TestPickupPointFactory pickupPointFactory;

    @Test
    void getDeactivations() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(false)
                        .cashAllowed(false)
                        .returnAllowed(false)
                        .prepayAllowed(true)
                        .active(true)
                        .build());

        var firstDeactivation = pickupPointDeactivationLogRepository.findAll().iterator().next();

        var dr1 = deactivationReasonCommandService.createDeactivationReason("Причина 1", "Описание причины 1", true,
                true, null);
        var dr2 = deactivationReasonCommandService.createDeactivationReason("Причина 2", "Описание причины 2", true,
                true, null);

        var date1 = LocalDate.of(2021, 12, 1);
        var date2 = LocalDate.of(2021, 12, 2);

        deactivationCommandService.deactivate(pickupPoint.getPvzMarketId(), dr1.getId(), date1);
        deactivationCommandService.deactivate(pickupPoint.getPvzMarketId(), dr2.getId(), date2);

        createDeactivation(pickupPoint.getPvzMarketId(), dr1.getId(), date1, date1);
        createDeactivation(pickupPoint.getPvzMarketId(), dr2.getId(), date2, date2);

        var actual = deactivationQueryService.getDeactivations(pickupPoint.getPvzMarketId());
        var deactivationReasons = StreamEx.of(actual.getDeactivationReasons())
                .map(DeactivationReasonParams::getId).toList();
        var currentDeactivationDeactivationDates = StreamEx.of(actual.getCurrentDeactivations())
                .map(DeactivationParams::getDeactivationDate).toList();
        var previousDeactivationActivationDates = StreamEx.of(actual.getPreviousDeactivations())
                .map(DeactivationParams::getActivationDate).toList();

        assertThat(actual.isDropOff()).isFalse();
        assertThat(actual.isActivatedForClients()).isTrue();
        assertThat(actual.isActivatedForDropOff()).isTrue();
        assertThat(currentDeactivationDeactivationDates).containsExactlyElementsOf(
                List.of(firstDeactivation.getDeactivationDate(), date1, date2));
        assertThat(previousDeactivationActivationDates).containsExactlyElementsOf(List.of(date2, date1));
        assertThat(deactivationReasons).containsAll(List
                .of(dr1.getId(), dr2.getId()));
    }

    private void createDeactivation(long pvzMarketId, long reasonId,
                                    LocalDate deactivationDate, LocalDate activationDate) {
        var deactivation = PickupPointDeactivationLog.builder()
                .pickupPoint(pickupPointRepository.findByPvzMarketIdOrThrow(pvzMarketId))
                .deactivationReason(deactivationReasonRepository.findByIdOrThrow(reasonId))
                .deactivationDate(deactivationDate);

        if (activationDate != null) {
            pickupPointDeactivationLogRepository.save(deactivation.activationDate(activationDate).build());
        } else {
            pickupPointDeactivationLogRepository.save(deactivation.build());
        }
    }

    @Test
    void getDayOffsAndOverrides() {
        clock.setFixed(Instant.EPOCH, clock.getZone());

        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(false)
                        .cashAllowed(false)
                        .returnAllowed(false)
                        .prepayAllowed(true)
                        .active(true)
                        .build());

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason("Логистическая причина", "",
                true, false, "LOGISTICS_REASON");

        deactivationCommandService.deactivate(pickupPoint.getPvzMarketId(), deactivationReason.getId(),
                LocalDate.now());

        var today = LocalDate.now().minusDays(3);
        var dayOffsAndOverrides = deactivationQueryService.getDayOffsAndOverrides(
                Collections.singletonList(pickupPoint.getId()), today);

        var dayOffs = dayOffsAndOverrides.getDayOffs().get(pickupPoint.getId());
        assertThat(dayOffs.size()).isEqualTo(3);
        assertThat(dayOffs).containsExactlyInAnyOrderElementsOf(List.of(today,
                today.plusDays(1), today.plusDays(2)));

        var overrides = dayOffsAndOverrides.getOverrides().get(pickupPoint.getId());
        var days = new ArrayList<LocalDate>();
        for (int i = 0; i < 30; i++) {
            days.add(LocalDate.now().plusDays(i));
        }
        assertThat(overrides.size()).isEqualTo(30);
        assertThat(StreamEx.of(overrides).map(PickupPointCalendarOverride::getDate).toList()).containsAll(days);
    }
}
