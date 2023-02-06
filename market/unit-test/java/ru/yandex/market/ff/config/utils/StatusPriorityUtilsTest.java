package ru.yandex.market.ff.config.utils;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.RequestStatus;

public class StatusPriorityUtilsTest extends SoftAssertionSupport {

    @Test
    public void simplePriorityTest() {
        Map<RequestStatus, Integer> statusToPriority = StatusPriorityUtils.getStatusToPriority(
                Map.of(
                        RequestStatus.VALIDATED, List.of(RequestStatus.ACCEPTED_BY_SERVICE),
                        RequestStatus.ACCEPTED_BY_SERVICE, List.of(RequestStatus.VALIDATED, RequestStatus.IN_PROGRESS),
                        RequestStatus.IN_PROGRESS, List.of(RequestStatus.PROCESSED),
                        RequestStatus.PROCESSED, List.of(RequestStatus.FINISHED)
                )
        );
        assertions.assertThat(statusToPriority).hasSize(5);
        assertions.assertThat(statusToPriority.get(RequestStatus.VALIDATED)).isEqualTo(-1);
        assertions.assertThat(statusToPriority.get(RequestStatus.ACCEPTED_BY_SERVICE)).isEqualTo(0);
        assertions.assertThat(statusToPriority.get(RequestStatus.IN_PROGRESS)).isEqualTo(1);
        assertions.assertThat(statusToPriority.get(RequestStatus.PROCESSED)).isEqualTo(2);
        assertions.assertThat(statusToPriority.get(RequestStatus.FINISHED)).isEqualTo(-1);
    }

    @Test
    public void testWithRecalculationPriorityOfSomeStatuses() {
        Map<RequestStatus, Integer> statusToPriority = StatusPriorityUtils.getStatusToPriority(
                Map.of(
                        RequestStatus.VALIDATED, List.of(RequestStatus.ACCEPTED_BY_SERVICE),
                        RequestStatus.ACCEPTED_BY_SERVICE, List.of(RequestStatus.VALIDATED, RequestStatus.IN_PROGRESS),
                        RequestStatus.ACCEPTED_BY_XDOC_SERVICE, List.of(RequestStatus.ARRIVED_TO_XDOC_SERVICE),
                        RequestStatus.ARRIVED_TO_XDOC_SERVICE, List.of(RequestStatus.IN_PROGRESS),
                        RequestStatus.IN_PROGRESS, List.of(RequestStatus.PROCESSED),
                        RequestStatus.PROCESSED, List.of(RequestStatus.FINISHED)
                )
        );
        assertions.assertThat(statusToPriority).hasSize(7);
        assertions.assertThat(statusToPriority.get(RequestStatus.VALIDATED)).isEqualTo(-1);
        assertions.assertThat(statusToPriority.get(RequestStatus.ACCEPTED_BY_SERVICE)).isEqualTo(0);
        assertions.assertThat(statusToPriority.get(RequestStatus.ACCEPTED_BY_XDOC_SERVICE)).isEqualTo(0);
        assertions.assertThat(statusToPriority.get(RequestStatus.ARRIVED_TO_XDOC_SERVICE)).isEqualTo(1);
        assertions.assertThat(statusToPriority.get(RequestStatus.IN_PROGRESS)).isEqualTo(2);
        assertions.assertThat(statusToPriority.get(RequestStatus.PROCESSED)).isEqualTo(3);
        assertions.assertThat(statusToPriority.get(RequestStatus.FINISHED)).isEqualTo(-1);
    }
}
