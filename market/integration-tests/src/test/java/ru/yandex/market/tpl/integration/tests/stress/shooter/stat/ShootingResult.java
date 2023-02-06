package ru.yandex.market.tpl.integration.tests.stress.shooter.stat;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Класс содержит информацию о проведённых стрельбах.
 */
@Log4j2
@AllArgsConstructor
@NoArgsConstructor
public class ShootingResult {
    @Getter
    private Collection<ShootingResultItem> requests = new ConcurrentLinkedQueue<>();

    public ShootingResultItem create() {
        ShootingResultItem item = new ShootingResultItem();
        requests.add(item);
        return item;
    }

    public Map<Integer, Integer> failedActionsCountByRps() {
        return requests.stream()
                .filter(ShootingResultItem::isFailed)
                .collect(Collectors.groupingBy(ShootingResultItem::getRps,
                        Collectors.collectingAndThen(Collectors.toList(), List::size)));
    }

    public Long get99Duration() {
        return getPercentileDuration(0.99);
    }

    public Long getPercentileDuration(double percentile) {
        List<Long> durations = requests.stream()
                .map(ShootingResultItem::getDurationMs)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        return durations.size() == 0 ? 0L : durations.get((int) ((durations.size() - 1.0) * percentile));
    }

    public ShootingResult rpsSlice(int rps) {
        return requests.stream()
                .filter(x -> x.getRps() <= rps)
                .collect(Collectors.collectingAndThen(Collectors.toList(), ShootingResult::new));
    }

    public void printResult() {
        Map<Integer, List<ShootingResultItem>> itemsByRps = requests.stream()
                .collect(Collectors.groupingBy(ShootingResultItem::getRps));
        Map<Integer, List<ShootingResultItem>> failedItemsByRps = requests.stream()
                .filter(ShootingResultItem::isFailed)
                .collect(Collectors.groupingBy(ShootingResultItem::getRps));
        itemsByRps.keySet().stream().sorted()
                .forEach(rps -> {
                    List<ShootingResultItem> items = itemsByRps.get(rps);
                    List<ShootingResultItem> failedItems = failedItemsByRps.getOrDefault(rps, List.of());
                    log.info("On rps {} failed {} / {} ({}%) actions", rps, failedItems.size(), items.size(),
                            (failedItems.size() * 100 / items.size()));
                });
    }

    public boolean allRequestsFinished() {
        return requests.stream().allMatch(ShootingResultItem::isFinished);
    }

    public boolean allRequestsSuccess() {
        return requests.stream().noneMatch(ShootingResultItem::isFailed);
    }

    public void checkAllFinishedSuccessfully() {
        assertThat(allRequestsFinished()).withFailMessage("Some requests not finished.").isTrue();
        assertThat(allRequestsSuccess()).withFailMessage("Some requests finished with errors.").isTrue();
    }

    public void checkRps() {
        Map<Integer, Integer> failedActionsCountByRps = failedActionsCountByRps();
        int minFailedRpsAll = failedActionsCountByRps.keySet().stream().mapToInt(x -> x).min().orElse(0);
        assertThat(failedActionsCountByRps).withFailMessage("Не выдерживаем rps=%d.", minFailedRpsAll).isEmpty();
    }

}
