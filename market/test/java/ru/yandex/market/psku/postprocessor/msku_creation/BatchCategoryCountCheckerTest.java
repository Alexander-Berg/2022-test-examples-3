package ru.yandex.market.psku.postprocessor.msku_creation;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchCategoryCountCheckerTest {

    @Test
    public void testTakesAllBatchesIfCanSendAllBatches() {
        List<Long> categories = generateLongList(Arrays.asList(
            10L, 10L, 5L, 5L));
        BatchCategoryCountChecker batchCategoryCountChecker =
            new BatchCategoryCountChecker(new HashMap<>(),
                7,
                100,
                categories.size());

        Map<Long, Long> counts = categories.stream().filter(batchCategoryCountChecker)
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        Assertions.assertThat(counts.values().stream().mapToLong(Long::longValue).sum())
            .isEqualTo(categories.size());
    }

    @Test
    public void testOneMegaBatchIsLimited() {
        List<Long> categories = generateLongList(Arrays.asList(
            100L, 10L, 5L, 5L));
        int batchesToSend = 25;
        int batchPerCategoryLimit = 10;
        BatchCategoryCountChecker batchCategoryCountChecker =
            new BatchCategoryCountChecker(new HashMap<>(),
                batchPerCategoryLimit,
                batchesToSend,
                categories.size());

        Map<Long, Long> counts = categories.stream().filter(batchCategoryCountChecker)
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        Assertions.assertThat(counts.values().stream().mapToLong(Long::longValue).sum())
            .isEqualTo(batchesToSend);
        Assertions.assertThat(counts.values()).allMatch(count -> count <= batchPerCategoryLimit);
    }

    @Test
    public void testTwoMegaBatchesAreLimited() {
        List<Long> categories = generateLongList(Arrays.asList(
            100L, 100L, 10L, 10L, 10L, 10L));
        int batchesToSend = 60;
        int batchPerCategoryLimit = 15;
        BatchCategoryCountChecker batchCategoryCountChecker =
            new BatchCategoryCountChecker(new HashMap<>(),
                batchPerCategoryLimit,
                batchesToSend,
                categories.size());

        Map<Long, Long> counts = categories.stream().filter(batchCategoryCountChecker)
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        Assertions.assertThat(counts.values().stream().mapToLong(Long::longValue).sum())
            .isEqualTo(batchesToSend);
        Assertions.assertThat(counts.values()).allMatch(count -> count <= batchPerCategoryLimit);
    }

    @Test
    public void testWhenLimitsAreLiftedAllSmallCategoriesAreTaken() {
        List<Long> categories = generateLongList(Arrays.asList(
            100L, 100L, 10L, 10L, 10L, 10L));
        int batchesToSend = 100;
        int batchPerCategoryLimit = 15;
        BatchCategoryCountChecker batchCategoryCountChecker =
            new BatchCategoryCountChecker(new HashMap<>(),
                batchPerCategoryLimit,
                batchesToSend,
                categories.size());

        Map<Long, Long> counts = categories.stream().filter(batchCategoryCountChecker)
            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        Assertions.assertThat(counts.values().stream().mapToLong(Long::longValue).sum())
            .isEqualTo(batchesToSend);
        Assertions.assertThat(counts.values()).allMatch(count -> count >= 10L);
        Assertions.assertThat(counts.size()).isEqualTo(6);
    }

    private List<Long> generateLongList(List<Long> counts) {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < counts.size(); i++) {
            Long count = counts.get(i);
            for (int j = 0; j < count; j++) {
                list.add((long) i);
            }
        }
        return list;
    }
}