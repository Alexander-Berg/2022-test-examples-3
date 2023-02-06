package ru.yandex.market.pvz.tms.executor.pickup_point;

import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.ChangeActiveReason;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.ChangeActiveType;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.PickupPointChangeActiveLog;
import ru.yandex.market.pvz.core.domain.pickup_point.change_active.PickupPointChangeActiveLogRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReason;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLog;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLogRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.DEACTIVATION_WITH_REASONS;

@TransactionlessEmbeddedDbTest
@Import({PickupPointChangeActiveExecutor.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointChangeActiveExecutorTest {

    private final TestableClock clock;
    private final PickupPointChangeActiveLogRepository changeActiveLogRepository;

    private final PickupPointChangeActiveExecutor executor;

    private final TestPickupPointFactory pickupPointFactory;
    private final PickupPointDeactivationLogRepository deactivationLogRepository;
    private final DeactivationReasonRepository deactivationReasonRepository;
    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final PickupPointRepository pickupPointRepository;

    @ParameterizedTest
    @EnumSource(ChangeActiveReason.class)
    void testCorrectChangeActiveType(ChangeActiveReason changeActiveReason) {
        var pickupPoint = createPickupPoint(true);
        changePickupPointActive(pickupPoint, changeActiveReason, ChangeActiveType.DEACTIVATE, false);

        var pickupPoint2 = createPickupPoint(false);
        changePickupPointActive(pickupPoint2, changeActiveReason, ChangeActiveType.ACTIVATE, false);

        executor.doRealJob(null);

        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint.getId()).getActive()).isFalse();
        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint2.getId()).getActive()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(ChangeActiveReason.class)
    void testWrongChangeActiveType(ChangeActiveReason changeActiveReason) {
        var pickupPoint = createPickupPoint(true);
        changePickupPointActive(pickupPoint, changeActiveReason, ChangeActiveType.ACTIVATE, false);

        var pickupPoint2 = createPickupPoint(false);
        changePickupPointActive(pickupPoint2, changeActiveReason, ChangeActiveType.DEACTIVATE, false);

        executor.doRealJob(null);

        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint.getId()).getActive()).isTrue();
        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint2.getId()).getActive()).isFalse();
    }

    @Test
    void testDeactivate() {
        configurationGlobalCommandService.setValue(DEACTIVATION_WITH_REASONS, true);
        clock.setFixed(Instant.EPOCH, clock.getZone());
        var reason1 = createReason("Причина 1", "Описание причины 1");
        var reason2 = createReason("Причина 2", "Описание причины 2");
        var pickupPoint1 = createPickupPoint(true);
        var pickupPoint2 = createPickupPoint(true);
        createDeactivation(pickupPoint1.getPvzMarketId(), reason1.getId(), LocalDate.now(clock));
        createDeactivation(pickupPoint1.getPvzMarketId(), reason2.getId(), LocalDate.now(clock));
        createDeactivation(pickupPoint2.getPvzMarketId(), reason1.getId(), LocalDate.now(clock));

        executor.doRealJob(null);

        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint1.getId()).getActive()).isFalse();
        assertThat(pickupPointRepository.findByIdOrThrow(pickupPoint2.getId()).getActive()).isFalse();

        var deactivations = deactivationLogRepository.findAll();
        assertThat(deactivations.size()).isEqualTo(5);

        for (var deactivation : deactivations) {
            assertThat(deactivation.getDeactivationDate()).isEqualTo(LocalDate.now(clock));
            assertThat(deactivation.getDeactivationAppliedAt()).isNotNull();
        }
    }

    private PickupPoint createPickupPoint(boolean active) {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        return pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(active)
                        .build());
    }

    private void changePickupPointActive(
            PickupPoint pickupPoint, ChangeActiveReason reason, ChangeActiveType changeActiveType, boolean outdated
    ) {
        changeActiveLogRepository.save(
                PickupPointChangeActiveLog.builder()
                        .pickupPoint(pickupPoint)
                        .estimatedDate(LocalDate.now(clock))
                        .reason(reason)
                        .type(changeActiveType)
                        .outdated(outdated)
                        .build()
        );
    }

    private DeactivationReason createReason(String reason, String details) {
        return deactivationReasonRepository.save(DeactivationReason.builder()
                .reason(reason)
                .details(details)
                .canBeCancelled(true)
                .fullDeactivation(true)
                .build());
    }

    private void createDeactivation(long pvzMarketId, long reasonId,
                                    LocalDate deactivationDate) {
        deactivationLogRepository.save(PickupPointDeactivationLog.builder()
                .pickupPoint(pickupPointRepository.findByPvzMarketIdOrThrow(pvzMarketId))
                .deactivationReason(deactivationReasonRepository.findByIdOrThrow(reasonId))
                .deactivationDate(deactivationDate).build());
    }
}
