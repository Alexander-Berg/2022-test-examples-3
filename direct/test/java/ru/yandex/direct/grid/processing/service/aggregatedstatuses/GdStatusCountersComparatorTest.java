package ru.yandex.direct.grid.processing.service.aggregatedstatuses;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.direct.core.entity.aggregatedstatuses.GdEntityLevel;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum;
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason;
import ru.yandex.direct.grid.model.aggregatedstatuses.GdPopupEntityEnum;
import ru.yandex.direct.grid.model.aggregatedstatuses.GdStatusCounter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class GdStatusCountersComparatorTest {
    @Test
    public void sortWithEmptyReasons() {
        final var statusCounter1 = new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.RETARGETING)
                .withEntityLevel(GdEntityLevel.RETARGETING)
                .withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of())
                .withModerationDiags(List.of());

        final var statusCounter2 = new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.BANNER)
                .withEntityLevel(GdEntityLevel.BANNER)
                .withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))
                .withModerationDiags(List.of());

        var counters1 = Stream.of(statusCounter1, statusCounter2).sorted(new GdStatusCountersComparator()).collect(Collectors.toList());
        var counters2 = Stream.of(statusCounter2, statusCounter1).sorted(new GdStatusCountersComparator()).collect(Collectors.toList());

        assertThat(counters1.get(0), equalTo(statusCounter2));
        assertThat(counters2.get(0), equalTo(statusCounter2));
    }

    @Test
    public void sortWithoutEmptyReasons() {
        final var statusCounter1 = new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.BANNER)
                .withEntityLevel(GdEntityLevel.BANNER)
                .withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of(GdSelfStatusReason.RELEVANCE_MATCH_SUSPENDED_BY_USER))
                .withModerationDiags(List.of());

        final var statusCounter2 = new GdStatusCounter()
                .withEntityType(GdPopupEntityEnum.BANNER)
                .withEntityLevel(GdEntityLevel.BANNER)
                .withStatus(GdSelfStatusEnum.STOP_CRIT)
                .withReasons(List.of(GdSelfStatusReason.REJECTED_ON_MODERATION))
                .withModerationDiags(List.of());

        var counters1 = Stream.of(statusCounter1, statusCounter2).sorted(new GdStatusCountersComparator()).collect(Collectors.toList());
        var counters2 = Stream.of(statusCounter2, statusCounter1).sorted(new GdStatusCountersComparator()).collect(Collectors.toList());

        assertThat(counters1.get(0), equalTo(statusCounter2));
        assertThat(counters2.get(0), equalTo(statusCounter2));
    }
}
