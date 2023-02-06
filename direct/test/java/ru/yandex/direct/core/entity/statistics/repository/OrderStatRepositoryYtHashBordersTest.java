package ru.yandex.direct.core.entity.statistics.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.statistics.model.YtHashBorders;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.statistics.repository.OrderStatRepository.getYtHashBorders;

@RunWith(Parameterized.class)
public class OrderStatRepositoryYtHashBordersTest {

    @Parameterized.Parameter(0)
    public Integer workersCount;
    @Parameterized.Parameter(1)
    public Integer batchesCount;
    @Parameterized.Parameter(2)
    public Integer ytHashMaxValue;

    @Parameterized.Parameters(name = "workersCount={0}, batchesCount={1}, ytHashMaxValue={2}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {3, 4, 64},
                {4, 4, 64},
                {5, 4, 64},
                {6, 4, 64},
                {3, 5, 65},
                {4, 5, 65},
                {5, 5, 65},
                {6, 5, 65},
                {3, 12, 66},
                {4, 12, 66},
                {5, 12, 66},
                {6, 12, 66},
        });
    }

    @Test
    public void validYtHashBorders() {
        Map<Long, Integer> ytHashValueMap = new HashMap<>();
        for (int workerNum = 0; workerNum < workersCount; ++workerNum) {
            for (int batchNum = 0; batchNum < batchesCount; ++batchNum) {
                YtHashBorders ytHashBorders = getYtHashBorders(
                        workerNum, workersCount, batchNum, batchesCount, ytHashMaxValue);
                if (ytHashBorders == null) {
                    continue;
                }
                for (long hashValue = ytHashBorders.getFirst(); hashValue <= ytHashBorders.getSecond(); ++hashValue) {
                    ytHashValueMap.put(hashValue, ytHashValueMap.getOrDefault(hashValue, 0) + 1);
                }
            }
        }

        assertThat(ytHashValueMap).hasSize(ytHashMaxValue + 1);
        assertThat(ytHashValueMap.values().stream().mapToInt(i -> i).max().orElse(0)).isEqualTo(1);
    }
}

