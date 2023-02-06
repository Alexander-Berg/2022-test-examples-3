package ru.yandex.market.pvz.core.domain.pickup_point.change_active;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PickupPointChangeActiveCommandServiceTest {

    private final Clock clock;

    private final TestPickupPointFactory pickupPointFactory;

    private final PickupPointQueryService pickupPointQueryService;
    private final PickupPointChangeActiveCommandService pickupPointChangeActiveCommandService;
    private final PickupPointChangeActiveLogRepository pickupPointChangeActiveLogRepository;

    @Test
    void deactivateThenActivateTest() {
        var pvzMarketIds = new ArrayList<Long>();

        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(false)
                        .cashAllowed(false)
                        .returnAllowed(false)
                        .prepayAllowed(true)
                        .active(true)
                        .build());
        pvzMarketIds.add(pickupPoint.getPvzMarketId());

        pickupPointChangeActiveCommandService.saveToChangeLog(pvzMarketIds, LocalDate.now(clock),
                ChangeActiveReason.NEXT_TO_BRAND, ChangeActiveType.DEACTIVATE);
        pickupPointChangeActiveCommandService.changeActive();

        var deactivated = pickupPointQueryService.getHeavy(pickupPoint.getId());
        assertThat(deactivated.getActive()).isFalse();
        assertThat(deactivated.getCashAllowed()).isFalse();
        assertThat(deactivated.getCardAllowed()).isFalse();
        assertThat(deactivated.getPrepayAllowed()).isFalse();
        assertThat(deactivated.getReturnAllowed()).isFalse();
        assertThat(pickupPointChangeActiveLogRepository.findAll().size()).isEqualTo(1);

        pickupPointChangeActiveCommandService.saveToChangeLog(pvzMarketIds, LocalDate.now(clock),
                ChangeActiveReason.NEXT_TO_BRAND, ChangeActiveType.ACTIVATE);
        pickupPointChangeActiveCommandService.changeActive();

        var activated = pickupPointQueryService.getHeavy(pickupPoint.getId());
        assertThat(activated.getActive()).isTrue();
        assertThat(activated.getCashAllowed()).isFalse();
        assertThat(activated.getCardAllowed()).isFalse();
        assertThat(activated.getPrepayAllowed()).isTrue();
        assertThat(activated.getReturnAllowed()).isFalse();

        var logs = pickupPointChangeActiveLogRepository.findAll();
        assertThat(logs.size()).isEqualTo(2);
        assertThat(logs.get(0).isOutdated()).isTrue();
        assertThat(logs.get(0).getAppliedAt()).isNotNull();
        assertThat(logs.get(1).isOutdated()).isTrue();
        assertThat(logs.get(1).getAppliedAt()).isNotNull();
    }

    @Test
    void deactivateThenDeactivateTest() {
        var pvzMarketIds = new ArrayList<Long>();

        var pickupPoint = getPickupPointForTest(true);
        pvzMarketIds.add(pickupPoint.getPvzMarketId());

        pickupPointChangeActiveCommandService.saveToChangeLog(pvzMarketIds, LocalDate.now(clock),
                ChangeActiveReason.NEXT_TO_BRAND, ChangeActiveType.DEACTIVATE);
        pickupPointChangeActiveCommandService.changeActive();

        assertThat(pickupPointQueryService.getHeavy(pickupPoint.getId()).getActive()).isFalse();
        assertThat(pickupPointChangeActiveLogRepository.findAll().size()).isEqualTo(1);

        pickupPointChangeActiveCommandService.saveToChangeLog(pvzMarketIds, LocalDate.now(clock),
                ChangeActiveReason.NEXT_TO_BRAND, ChangeActiveType.DEACTIVATE);

        assertThat(pickupPointQueryService.getHeavy(pickupPoint.getId()).getActive()).isFalse();
        assertThat(pickupPointChangeActiveLogRepository.findAll().size()).isEqualTo(2);
    }

    @Test
    void addTwoWithDifferentEstimateDate() {
        var pvzMarketIds = new ArrayList<Long>();

        var pickupPoint = pickupPointFactory.createPickupPoint(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .cardAllowed(false)
                                .cashAllowed(false)
                                .returnAllowed(false)
                                .prepayAllowed(false)
                                .build())
                        .build());
        pvzMarketIds.add(pickupPoint.getPvzMarketId());

        pickupPointChangeActiveCommandService.saveToChangeLog(pvzMarketIds, LocalDate.now(clock).plusDays(10),
                ChangeActiveReason.NEXT_TO_BRAND, ChangeActiveType.ACTIVATE);

        assertThat(pickupPointQueryService.getHeavy(pickupPoint.getId()).getActive()).isFalse();
        assertThat(pickupPointChangeActiveLogRepository.findAll().size()).isEqualTo(1);

        pickupPointChangeActiveCommandService.saveToChangeLog(pvzMarketIds, LocalDate.now(clock).plusDays(5),
                ChangeActiveReason.NEXT_TO_BRAND, ChangeActiveType.ACTIVATE);

        assertThat(pickupPointQueryService.getHeavy(pickupPoint.getId()).getActive()).isFalse();

        var logs = pickupPointChangeActiveLogRepository.findAll();
        assertThat(logs.size()).isEqualTo(1);
        assertThat(logs.iterator().next().getEstimatedDate()).isEqualTo(LocalDate.now(clock).plusDays(5));
    }

    @Test
    void testCancelChangeActive() {
        var pvzMarketIds = new ArrayList<Long>();

        var pickupPoint = getPickupPointForTest(true);
        pvzMarketIds.add(pickupPoint.getPvzMarketId());

        pickupPointChangeActiveCommandService.saveToChangeLog(pvzMarketIds, LocalDate.now(clock).plusDays(10),
                ChangeActiveReason.NEXT_TO_BRAND, ChangeActiveType.DEACTIVATE);

        assertThat(pickupPointQueryService.getHeavy(pickupPoint.getId()).getActive()).isTrue();
        assertThat(pickupPointChangeActiveLogRepository.findAll().size()).isEqualTo(1);

        pickupPointChangeActiveCommandService.cancelChangeActive(pvzMarketIds);

        assertThat(pickupPointQueryService.getHeavy(pickupPoint.getId()).getActive()).isTrue();
        var log = pickupPointChangeActiveLogRepository.findAll().get(0);
        assertThat(log.isOutdated()).isTrue();
        assertThat(log.getType()).isEqualTo(ChangeActiveType.CANCEL);
    }

    @Test
    void testInvalidPvzMarketIds() {
        var pvzMarketIds = new ArrayList<Long>();

        var pickupPoint = getPickupPointForTest(true);
        pvzMarketIds.add(pickupPoint.getPvzMarketId());
        pvzMarketIds.add(100L);

        assertThatThrownBy(() -> pickupPointChangeActiveCommandService.saveToChangeLog(pvzMarketIds,
                LocalDate.now(clock), ChangeActiveReason.NEXT_TO_BRAND, ChangeActiveType.DEACTIVATE))
                .isExactlyInstanceOf(TplInvalidParameterException.class)
                .hasMessage("Incorrect pvzMarketIds: [100]");
    }

    private PickupPoint getPickupPointForTest(boolean isActive) {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .active(isActive)
                        .build());

        return pickupPoint;
    }
}
