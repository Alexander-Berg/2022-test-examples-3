package ru.yandex.autotests.direct.cmd.data.counters;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableMap;

import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;

import static java.util.Arrays.asList;

/**
 * аб-сегменты созданы для логина at-direct-absegment. Остальным логинам дан доступ через представительство
 */
public enum MetrikaCountersData {

    DEFAULT_COUNTER(31844711L, asList(16819470L, 16819515L), ImmutableMap.of(3L, asList(2_500_000_005L, 2_500_000_006L)));

    private Long counterId;
    private List<Long> goalIds;
    private Map<Long, List<Long>> abSegmentIdsBySectionId;

    MetrikaCountersData(Long counterId, List<Long> goalIds, Map<Long, List<Long>> abSegmentIdsBySectionId) {
        this.counterId = counterId;
        this.goalIds = goalIds;
        this.abSegmentIdsBySectionId = abSegmentIdsBySectionId;
    }

    public Long getCounterId() {
        return counterId;
    }

    public List<Long> getGoalIds() {
        return goalIds;
    }

    public Long getFirstGoalId() {
        return goalIds.get(0) != null ? goalIds.get(0) : 0L;
    }

    public Map<Long, List<Long>> getAbSegmentIdsBySectionId() {
        return abSegmentIdsBySectionId;
    }

    public Long getFirstAbSegmentId() {
        return abSegmentIdsBySectionId.get(getFirstAbSectionId()).stream()
                .findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("Сегмент не найден"));
    }

    public Long getFirstAbSectionId() {
        return abSegmentIdsBySectionId.keySet().stream().findFirst()
                .orElseThrow(() -> new DirectCmdStepsException("Эксперимент не найден"));
    }
}
