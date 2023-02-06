package ru.yandex.market.pvz.core.domain.pickup_point.deactivation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointDeactivationCommandServiceTest {

    private static final String REASON = "Причина";
    private static final String DETAILS = "Описание причины";
    private static final String LOGISTICS_REASON = "UNPROFITABLE";

    private final TestableClock clock;
    private final PickupPointDeactivationCommandService deactivationCommandService;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final PickupPointDeactivationLogRepository pickupPointDeactivationLogRepository;
    private final PickupPointRepository pickupPointRepository;
    private final TestPickupPointFactory pickupPointFactory;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.EPOCH, clock.getZone());
    }

    @Test
    void deactivate() {
        var pickupPoint = createPickupPoint(false, true, true, true, true, null);
        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, true,
                true, null);
        deactivationCommandService.deactivate(pickupPoint.getPvzMarketId(), deactivationReason.getId(),
                LocalDate.now(clock));

        var deactivation = pickupPointDeactivationLogRepository
                .findFirstByPickupPointIdAndDeactivationReasonIdAndActivationDateIsNull(pickupPoint.getId(),
                        deactivationReason.getId()).get();

        assertThat(deactivation.getDeactivationDate()).isEqualTo(LocalDate.now(clock));
        assertThat(deactivation.getDeactivationAppliedAt()).isNotNull();

        checkFullDeactivatedPickupPoint(pickupPoint);
    }

    @Test
    void deactivateSamePickupPointWithSameReason() {
        var pickupPoint = createPickupPoint(false, false, false, true, true, null);
        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, true,
                true, null);

        var pvzMarketId = pickupPoint.getPvzMarketId();
        var reasonId = deactivationReason.getId();
        deactivationCommandService.deactivate(pvzMarketId, reasonId, LocalDate.now(clock));

        assertThrows(DeactivationAlreadyExistsException.class, () ->
                deactivationCommandService.deactivate(pvzMarketId, reasonId, LocalDate.now(clock)));
    }

    @Test
    void cancelDeactivation() {
        var pickupPoint = createPickupPoint(false, true, true, true, true, PickupPointBrandingType.FULL);
        cancelFirstDeactivation(pickupPoint.getPvzMarketId());
        var deactivation = pickupPointDeactivationLogRepository.findAll().iterator().next();

        assertThat(deactivation.getDeactivationDate()).isEqualTo(LocalDate.now(clock));
        assertThat(deactivation.getDeactivationAppliedAt()).isNotNull();
        assertThat(deactivation.getActivationDate()).isEqualTo(LocalDate.now(clock));
        assertThat(deactivation.getActivationAppliedAt()).isNotNull();

        var actual = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(actual.getActive()).isTrue();
        assertThat(actual.getReturnAllowed()).isTrue();
        assertThat(actual.getCardAllowed()).isFalse();
        assertThat(actual.getCashAllowed()).isTrue();
        assertThat(actual.getPrepayAllowed()).isTrue();
    }

    @Test
    void disableDropOff() {
        var pickupPoint = createPickupPoint(false, false, true, true, true, null);
        deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, true, true,
                LOGISTICS_REASON);

        deactivationCommandService.disableDropOff(pickupPoint.getLmsId(), LocalDate.now(clock), LOGISTICS_REASON);

        var deactivations = pickupPointDeactivationLogRepository.findAll();
        assertThat(deactivations.size()).isEqualTo(2);

        var deactivation = deactivations.iterator().next();
        assertThat(deactivation.getDeactivationDate()).isEqualTo(LocalDate.now(clock));
        assertThat(deactivation.getDeactivationAppliedAt()).isNotNull();

        var actual = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(actual).isEqualTo(pickupPoint);
    }

    @Test
    void getPickupPointWithDeactivationOverrides() {
        var pickupPoint = createPickupPoint(false, false, true, true, true, null);
        deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, true, true,
                LOGISTICS_REASON);

        deactivationCommandService.disableDropOff(pickupPoint.getLmsId(), LocalDate.now(clock), LOGISTICS_REASON);
    }

    @Test
    void deactivateAndActivatePickupPointForTwoReasons() {
        var pickupPoint = createDropOffPickupPointWithoutFirstDeactivation();

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, false,
                true, null);
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason.getId(), LocalDate.now(clock));

        checkNotFullDeactivatedPickupPointWithDropOff(pickupPoint);

        var deactivationReason2 = deactivationReasonCommandService.createDeactivationReason(
                REASON + "2", DETAILS + "2", true, true, LOGISTICS_REASON
        );
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason2.getId(), LocalDate.now(clock));

        checkFullDeactivatedPickupPointWithDropOff(pickupPoint);

        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        var deactivation2 = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason2.getId()
        ).get();

        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation2.getId());

        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    @Test
    void deactivateAndActivatePickupPointForTwoReasonsWithDelay() {
        var pickupPoint = createDropOffPickupPointWithoutFirstDeactivation();

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, false,
                true, null);
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(
                pvzMarketId, deactivationReason.getId(), LocalDate.now(clock).plusDays(2)
        );

        var deactivationReason2 = deactivationReasonCommandService.createDeactivationReason(
                REASON + "2", DETAILS + "2", true, true, LOGISTICS_REASON
        );
        deactivationCommandService.deactivate(
                pvzMarketId, deactivationReason2.getId(), LocalDate.now(clock).plusDays(4)
        );

        clock.setFixed(Instant.EPOCH.plus(3, ChronoUnit.DAYS), clock.getZone());
        deactivationCommandService.deactivateWithDelay();

        checkNotFullDeactivatedPickupPointWithDropOff(pickupPoint);

        clock.setFixed(Instant.EPOCH.plus(5, ChronoUnit.DAYS), clock.getZone());
        deactivationCommandService.deactivateWithDelay();

        checkFullDeactivatedPickupPointWithDropOff(pickupPoint);

        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        var deactivation2 = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason2.getId()
        ).get();

        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation2.getId());

        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    @Test
    void whenFullDeactivationWithDelayDropOffPickupPointThenActivation() {
        var pickupPoint = createDropOffPickupPointWithoutFirstDeactivation();

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, true,
                true, null);
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(
                pvzMarketId, deactivationReason.getId(), LocalDate.now(clock).plusDays(2)
        );
        checkFullActivePickupPointWithDropOff(pickupPoint);

        clock.setFixed(Instant.EPOCH.plus(3, ChronoUnit.DAYS), clock.getZone());
        deactivationCommandService.deactivateWithDelay();
        checkFullDeactivatedPickupPointWithDropOff(pickupPoint);

        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    @Test
    void whenNotFullDeactivationWithDelayDropOffPickupPointThenActivation() {
        var pickupPoint = createDropOffPickupPointWithoutFirstDeactivation();

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, false,
                true, null);
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(
                pvzMarketId, deactivationReason.getId(), LocalDate.now(clock).plusDays(2)
        );
        checkFullActivePickupPointWithDropOff(pickupPoint);
        clock.setFixed(Instant.EPOCH.plus(3, ChronoUnit.DAYS), clock.getZone());
        deactivationCommandService.deactivateWithDelay();
        checkNotFullDeactivatedPickupPointWithDropOff(pickupPoint);

        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    @Test
    void whenNoPickupPointForDeactivation() {
        var pickupPoint = createDropOffPickupPointWithoutFirstDeactivation();
        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, false,
                true, null);
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(
                pvzMarketId, deactivationReason.getId(), LocalDate.now(clock).plusDays(2)
        );

        deactivationCommandService.deactivateWithDelay();
        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    @Test
    void whenFullDeactivationDropOffPickupPointThenActivation() {
        var pickupPoint = createDropOffPickupPointWithoutFirstDeactivation();

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, true,
                true, null);
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason.getId(), LocalDate.now(clock));
        checkFullDeactivatedPickupPointWithDropOff(pickupPoint);

        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    @Test
    void whenNotFullDeactivationDropOffPickupPointThenActivation() {
        var pickupPoint = createDropOffPickupPointWithoutFirstDeactivation();

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(REASON, DETAILS, false,
                true, null);
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason.getId(), LocalDate.now(clock));
        checkNotFullDeactivatedPickupPointWithDropOff(pickupPoint);

        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    @Test
    void checkDropOffActivationWithAppliedNotFullDeactivation() {
        var pickupPoint = createDropOffPickupPointWithoutFirstDeactivation();

        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                REASON, DETAILS, false, true, null
        );
        var deactivationReason2 = deactivationReasonCommandService.createDeactivationReason(
                REASON + "2", DETAILS, true, true, null
        );
        var deactivationReason3 = deactivationReasonCommandService.createDeactivationReason(
                REASON + "3", DETAILS, true, true, null
        );
        var pvzMarketId = pickupPoint.getPvzMarketId();
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason.getId(), LocalDate.now(clock));
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason2.getId(), LocalDate.now(clock));
        deactivationCommandService.deactivate(pvzMarketId, deactivationReason3.getId(), LocalDate.now(clock));
        checkFullDeactivatedPickupPointWithDropOff(pickupPoint);

        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason3.getId()
        ).get();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
        checkFullDeactivatedPickupPointWithDropOff(pickupPoint);

        deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason2.getId()
        ).get();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
        var actual = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(actual.getDropOffFeature()).isTrue();

        deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
        checkFullActivePickupPointWithDropOff(pickupPoint);
    }

    private PickupPoint createDropOffPickupPointWithoutFirstDeactivation() {
        var pickupPoint = createPickupPoint(true, true, true, true, true, PickupPointBrandingType.FULL);
        cancelFirstDeactivation(pickupPoint.getPvzMarketId());
        pickupPointFactory.createDropOff(pickupPoint.getId());
        checkFullActivePickupPointWithDropOff(pickupPoint);
        return pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
    }

    private PickupPoint createPickupPoint(
            boolean cardAllowed, boolean cashAllowed, boolean returnAllowed, boolean prepayAllowed, boolean active,
            PickupPointBrandingType brandingType
    ) {
        return pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(brandingType)
                        .cardAllowed(cardAllowed)
                        .cashAllowed(cashAllowed)
                        .returnAllowed(returnAllowed)
                        .prepayAllowed(prepayAllowed)
                        .active(active)
                        .build());
    }

    private void cancelFirstDeactivation(Long pvzMarketId) {
        var deactivation = pickupPointDeactivationLogRepository.findAll().iterator().next();
        deactivationCommandService.cancelDeactivation(pvzMarketId, deactivation.getId());
    }

    private PickupPoint checkFullActivePickupPoint(PickupPoint pickupPoint) {
        var actual = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(actual.getActive()).isTrue();
        assertThat(actual.getReturnAllowed()).isTrue();
        assertThat(actual.getCardAllowed()).isTrue();
        assertThat(actual.getCashAllowed()).isTrue();
        assertThat(actual.getPrepayAllowed()).isTrue();
        return actual;
    }

    private void checkFullActivePickupPointWithDropOff(PickupPoint pickupPoint) {
        var actual = checkFullActivePickupPoint(pickupPoint);
        assertThat(actual.getDropOffFeature()).isTrue();
    }

    private void checkFullDeactivatedPickupPointWithDropOff(PickupPoint pickupPoint) {
        var actual = checkFullDeactivatedPickupPoint(pickupPoint);
        assertThat(actual.getDropOffFeature()).isFalse();
    }

    private PickupPoint checkFullDeactivatedPickupPoint(PickupPoint pickupPoint) {
        var actual = checkMainFlagsDeactivated(pickupPoint);
        assertThat(actual.getActive()).isFalse();
        return actual;
    }

    private void checkNotFullDeactivatedPickupPointWithDropOff(PickupPoint pickupPoint) {
        var actual = checkMainFlagsDeactivated(pickupPoint);
        assertThat(actual.getActive()).isTrue();
        assertThat(actual.getDropOffFeature()).isTrue();
    }

    private PickupPoint checkMainFlagsDeactivated(PickupPoint pickupPoint) {
        var actual = pickupPointRepository.findByIdOrThrow(pickupPoint.getId());
        assertThat(actual.getReturnAllowed()).isFalse();
        assertThat(actual.getCardAllowed()).isFalse();
        assertThat(actual.getCashAllowed()).isFalse();
        assertThat(actual.getPrepayAllowed()).isFalse();
        return actual;
    }
}
