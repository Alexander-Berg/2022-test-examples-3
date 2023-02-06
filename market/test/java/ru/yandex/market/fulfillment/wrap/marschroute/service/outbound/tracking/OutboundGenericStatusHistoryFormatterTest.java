package ru.yandex.market.fulfillment.wrap.marschroute.service.outbound.tracking;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.service.util.status.GenericStatusHistoryFormatter;
import ru.yandex.market.fulfillment.wrap.marschroute.service.util.status.adapter.GenericCommonStatusAdapter;
import ru.yandex.market.fulfillment.wrap.marschroute.service.util.status.strategy.OutboundStatusHistoryFormatterStrategy;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.Status;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.StatusCode;
import ru.yandex.market.logistic.api.utils.DateTime;

import static java.time.LocalDateTime.of;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.logistic.api.utils.DateTime.fromLocalDateTime;

@ExtendWith(MockitoExtension.class)
class OutboundGenericStatusHistoryFormatterTest {

    private final GenericStatusHistoryFormatter<Status, GenericCommonStatusAdapter> handler = new GenericStatusHistoryFormatter<>(
        new OutboundStatusHistoryFormatterStrategy()
    );

    @Test
    void deduplicateSingleStatus() {
        DateTime statusDate = fromLocalDateTime(of(1970, 1, 1, 0, 0));

        List<Status> history = handler.formatHistory(Collections.singletonList(
            new Status(StatusCode.CREATED, statusDate))
        );

        assertSoftly(assertions -> {
            assertions.assertThat(history)
                .as("Asserting that history has exactly 1 status")
                .hasSize(1);

            Status status = history.get(0);

            assertions.assertThat(status.getStatusCode())
                .as("Asserting status code")
                .isEqualTo(StatusCode.CREATED);

            assertions.assertThat(status.getDate())
                .as("Asserting status date")
                .isEqualTo(statusDate);
        });
    }

    @Test
    void deduplicateMultipleDifferentStatuses() {
        List<Status> initialList = Arrays.asList(
            new Status(StatusCode.ERROR, fromLocalDateTime(of(1970, 1, 3, 0, 0))),
            new Status(StatusCode.CREATED, fromLocalDateTime(of(1970, 1, 2, 0, 0))),
            new Status(StatusCode.PENDING, fromLocalDateTime(of(1970, 1, 1, 0, 0)))
        );
        List<Status> history = handler.formatHistory(initialList);

        assertSoftly(assertions -> {
            assertions.assertThat(history)
                .as("Assert that history contains 3 statuses")
                .hasSize(3);

            assertions.assertThat(history.get(0))
                .as("Asserting first status")
                .isEqualToComparingFieldByFieldRecursively(initialList.get(0));

            assertions.assertThat(history.get(1))
                .as("Asserting second status")
                .isEqualToComparingFieldByFieldRecursively(initialList.get(1));

            assertions.assertThat(history.get(2))
                .as("Asserting third status")
                .isEqualToComparingFieldByFieldRecursively(initialList.get(2));
        });
    }


    @Test
    void deduplicateMultipleIdenticalStatuses() {
        List<Status> initialList = Arrays.asList(
            new Status(StatusCode.PENDING, fromLocalDateTime(of(1970, 1, 1, 0, 0))),
            new Status(StatusCode.PENDING, fromLocalDateTime(of(1970, 1, 2, 0, 0))),
            new Status(StatusCode.PENDING, fromLocalDateTime(of(1970, 1, 3, 0, 0)))

        );
        List<Status> history = handler.formatHistory(initialList);

        assertSoftly(assertions -> {
            assertions.assertThat(history)
                .as("Assert that history contains 1 status")
                .hasSize(1);

            assertions.assertThat(history.get(0))
                .as("Asserting first status")
                .isEqualToComparingFieldByFieldRecursively(initialList.get(0));
        });
    }

    @Test
    void deduplicateMultipleIdenticalStatusesWithUnknown() {
        List<Status> initialList = Arrays.asList(
            new Status(StatusCode.PENDING, fromLocalDateTime(of(1970, 1, 1, 0, 0))),
            new Status(StatusCode.UNKNOWN, fromLocalDateTime(of(1970, 1, 2, 0, 0))),
            new Status(StatusCode.PENDING, fromLocalDateTime(of(1970, 1, 3, 0, 0)))

        );
        List<Status> history = handler.formatHistory(initialList);

        assertSoftly(assertions -> {
            assertions.assertThat(history)
                .as("Assert that history contains 2 statuses")
                .hasSize(3);

            assertions.assertThat(history.get(0))
                .as("Asserting first status")
                .isEqualToComparingFieldByFieldRecursively(initialList.get(2));

            assertions.assertThat(history.get(1))
                .as("Asserting second status")
                .isEqualToComparingFieldByFieldRecursively(initialList.get(1));

            assertions.assertThat(history.get(2))
                .as("Asserting second status")
                .isEqualToComparingFieldByFieldRecursively(initialList.get(0));
        });
    }
}
