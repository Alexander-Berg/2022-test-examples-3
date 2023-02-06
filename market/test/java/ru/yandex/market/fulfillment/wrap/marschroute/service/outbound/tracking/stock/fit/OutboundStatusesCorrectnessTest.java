package ru.yandex.market.fulfillment.wrap.marschroute.service.outbound.tracking.stock.fit;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.wrap.marschroute.model.response.tracking.TrackingStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.StatusCode;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;

class OutboundStatusesCorrectnessTest extends SoftAssertionSupport {

    private final static List<MappingLogic> expectedLogic = Arrays.asList(
        new MappingLogic(Range.closed(1, 14), StatusCode.CREATED),
        new MappingLogic(Range.singleton(15), StatusCode.ASSEMBLING),
        new MappingLogic(Range.singleton(16), StatusCode.ASSEMBLED),
        new MappingLogic(Range.closed(17, 18), StatusCode.TRANSFERRED),
        new MappingLogic(Range.singleton(50), StatusCode.TRANSFERRED),
        new MappingLogic(Range.closed(30, 35), StatusCode.CANCELLED),
        new MappingLogic(Range.singleton(37), StatusCode.CANCELLED)
    );

    private TrackingStatusToOutboundStatusMapping mapping = new TrackingStatusToOutboundStatusMapping();

    /**
     * Проверяет валидность логики маппинга статусов заказа на статусы изъятия в соответствии с
     * https://wiki.yandex-team.ru/market/pokupka/projects/ff/dev/outbounds/OutboundStatus/
     * <p>
     * Все статусы, которые не объявлены в таблице должны быть замаплены на null.
     */
    @Test
    void mappingCorrectness() {
        for (TrackingStatus currentTrackingStatus : TrackingStatus.values()) {

            List<MappingLogic> applicableLogic = expectedLogic
                .stream()
                .filter(logic -> logic.range.contains(currentTrackingStatus.getValue()))
                .collect(Collectors.toList());

            softly.assertThat(applicableLogic.size())
                .as("Applicable logic should have either 1 or 0 elements")
                .isLessThanOrEqualTo(1);

            StatusCode actualStatus = mapping.getCode(currentTrackingStatus).orElse(null);

            if (applicableLogic.isEmpty()) {
                assertNoMappingExist(currentTrackingStatus, actualStatus);
            } else {
                assertMappingIsCorrect(currentTrackingStatus, applicableLogic, actualStatus);
            }
        }
    }

    private void assertNoMappingExist(TrackingStatus currentTrackingStatus, StatusCode actualStatus) {
        softly.assertThat(actualStatus)
            .as(currentTrackingStatus + " should have 0 values mapped to")
            .isNull();
    }

    private void assertMappingIsCorrect(TrackingStatus currentTrackingStatus, List<MappingLogic> applicableLogic, StatusCode actualStatus) {
        MappingLogic logic = applicableLogic.iterator().next();

        softly.assertThat(logic.expectedStatus.equals(actualStatus))
            .as(currentTrackingStatus + " should be mapped to " + logic.expectedStatus)
            .isTrue();
    }

    private static class MappingLogic {
        final Range<Integer> range;
        final StatusCode expectedStatus;

        private MappingLogic(Range<Integer> range, StatusCode expectedStatus) {
            this.range = range;
            this.expectedStatus = expectedStatus;
        }
    }
}
