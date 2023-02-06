package ru.yandex.market.tpl.integration.tests.stress;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Класс содержит статистику http-запросов.
 */
@Component
@AllArgsConstructor
public class StressStat {
    @Getter
    private final Collection<RequestStat> requests;

    public StressStat() {
        this.requests = new ConcurrentLinkedDeque<>();
    }

    public void add(RequestStat requestStat) {
        requests.add(requestStat);
    }

    public void clear() {
        requests.clear();
    }

    public Map<Integer, Integer> httpStatusCodeMap() {
        return requests.stream()
                .collect(Collectors.groupingBy(RequestStat::getHttpStatus, Collectors.summingInt(x -> 1)));
    }

    public Long get99Duration() {
        return getPercentileDuration(0.99);
    }

    public Long getPercentileDuration(double percentile) {
        List<Long> durations = requests.stream()
                .map(RequestStat::getDurationMs)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        return durations.size() == 0 ? 0L : durations.get((int) ((durations.size() - 1.0) * percentile));
    }

    public boolean allRequestsFinished() {
        return requests.stream().allMatch(RequestStat::isFinished);
    }

    public Map<String, StressStat> splitByEndpoints() {
        return requests.stream()
                .collect(Collectors.groupingBy(RequestStat::getEndpoint,
                        Collectors.collectingAndThen(Collectors.toList(), StressStat::new)));
    }


    public void checkAllFinished() {
        assertThat(allRequestsFinished()).withFailMessage("Some requests not finished.").isTrue();
    }
}
