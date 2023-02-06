package ru.yandex.market.pvz.core.domain.sla;

import java.util.Collections;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.sla.entity.SlaPickupPoint;
import ru.yandex.market.pvz.core.domain.sla.yt.SlaPickupPointYtModel;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SlaPickupPointCommandServiceTest {

    private static final String DEFAULT_REPORT_MONTH = "2022-02";

    private final SlaPickupPointCommandService slaPickupPointCommandService;
    private final SlaPickupPointRepository slaPickupPointRepository;
    private final TestPickupPointFactory pickupPointFactory;

    @Test
    void saveAndUpdateSlaOrderRows() {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        var model = createYtModel(pickupPoint.getId(), 0);
        slaPickupPointCommandService.saveOrUpdateSlaPickupPointRows(Collections.singletonList(model));

        var slaPickupPoints = slaPickupPointRepository.findAll();
        assertThat(slaPickupPoints.size()).isEqualTo(1);
        assertThat(slaPickupPoints.get(0)).isEqualToIgnoringGivenFields(createSlaPickupPoint(model), "id", "createdAt",
                "updatedAt", "pickupPoint");

        model = createYtModel(pickupPoint.getId(), 1);
        slaPickupPointCommandService.saveOrUpdateSlaPickupPointRows(Collections.singletonList(model));

        slaPickupPoints = slaPickupPointRepository.findAll();
        assertThat(slaPickupPoints.size()).isEqualTo(1);
        assertThat(slaPickupPoints.get(0)).isEqualToIgnoringGivenFields(createSlaPickupPoint(model), "id", "createdAt",
                "updatedAt", "pickupPoint");

    }

    @Test
    void saveSlaOrderRowsWithNoExternalId() {
        var model = createYtModel(null, 0);
        slaPickupPointCommandService.saveOrUpdateSlaPickupPointRows(Collections.singletonList(model));

        var slaPickupPoints = slaPickupPointRepository.findAll();
        assertThat(slaPickupPoints.size()).isEqualTo(0);
    }

    private SlaPickupPointYtModel createYtModel(Long pickupPointId, double acceptTimelines) {
        return SlaPickupPointYtModel.builder()
                .pickupPointId(pickupPointId)
                .acceptTimeliness(acceptTimelines)
                .reportMonth(DEFAULT_REPORT_MONTH)
                .build();
    }

    private SlaPickupPoint createSlaPickupPoint(SlaPickupPointYtModel model) {
        return SlaPickupPoint.builder()
                .acceptTimeliness(model.getAcceptTimeliness())
                .reportMonth(DEFAULT_REPORT_MONTH)
                .build();
    }
}
