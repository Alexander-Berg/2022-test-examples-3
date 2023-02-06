package ru.yandex.market.pvz.core.domain.pickup_point.deactivation;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DeactivationReasonQueryServiceTest {

    private static final String REASON_1 = "Причина 1";
    private static final String REASON_2 = "Причина 2";
    private static final String DETAILS_1 = "Описание причины 1";
    private static final String DETAILS_2 = "Описание причины 2";
    private static final String LOGISTICS_REASON = "LOGISTICS_REASON";

    private final DeactivationReasonCommandService deactivationReasonCommandService;
    private final DeactivationReasonQueryService deactivationReasonQueryService;

    @Test
    void getLogisticsReasons() {
        deactivationReasonCommandService.createDeactivationReason(REASON_1, DETAILS_1, true, true,
                "LOGISTICS_REASON");
        deactivationReasonCommandService.createDeactivationReason(REASON_2, DETAILS_2, true, true, null);

        var actual = deactivationReasonQueryService.getLogisticsDeactivationReasons();
        assertThat(actual.size()).isEqualTo(1);
        assertThat(actual.get(0).getLogisticsReason()).isEqualTo(LOGISTICS_REASON);
        assertThat(actual.get(0).getReason()).isEqualTo(REASON_1);
    }
}
