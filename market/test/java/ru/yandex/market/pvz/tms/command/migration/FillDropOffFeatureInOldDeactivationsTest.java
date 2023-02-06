package ru.yandex.market.pvz.tms.command.migration;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Collections;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationCommandService;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLogRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.tms.command.migration.FillDropOffFeatureInOldDeactivations.COMMAND_NAME;

@TransactionlessEmbeddedDbTest
@Import({
        FillDropOffFeatureInOldDeactivations.class,
})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class FillDropOffFeatureInOldDeactivationsTest {

    private final FillDropOffFeatureInOldDeactivations command;
    private final PickupPointDeactivationCommandService deactivationCommandService;
    private final PickupPointDeactivationLogRepository pickupPointDeactivationLogRepository;
    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestableClock clock;

    @MockBean
    private Terminal terminal;

    @MockBean
    private PrintWriter printWriter;

    @Test
    void checkCommand() {
        var pickupPoint = createPickupPoint(true, true, true, true, true, PickupPointBrandingType.FULL);
        cancelFirstDeactivation(pickupPoint.getPvzMarketId());
        var deactivationReason = deactivationReasonCommandService.createDeactivationReason(
                "REASON", "DETAILS", true, true, null
        );
        deactivationCommandService.deactivate(
                pickupPoint.getPvzMarketId(), deactivationReason.getId(), LocalDate.now(clock)
        );

        var deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        deactivation.getDetails().setDropOffFeature(null);
        pickupPointDeactivationLogRepository.save(deactivation);
        deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        var expected = deactivation.getDetails();
        assertThat(expected.getDropOffFeature()).isNull();

        when(terminal.getWriter()).thenReturn(printWriter);
        command.executeCommand(new CommandInvocation(COMMAND_NAME, new String[]{}, Collections.emptyMap()), terminal);

        deactivation = pickupPointDeactivationLogRepository.findFirstByPickupPointIdAndDeactivationReasonId(
                pickupPoint.getId(), deactivationReason.getId()
        ).get();
        expected.setDropOffFeature(false);
        assertThat(deactivation.getDetails()).isEqualTo(expected);
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

}
