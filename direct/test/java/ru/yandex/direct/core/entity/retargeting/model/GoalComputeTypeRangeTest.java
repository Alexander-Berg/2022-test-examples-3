package ru.yandex.direct.core.entity.retargeting.model;

import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем, что не съехали диапазоны ID у сегментов в конвертации.
 * Актуальное описание тут: https://wiki.yandex-team.ru/JurijjGalickijj/raznoe/goalid/
 */
@RunWith(JUnitParamsRunner.class)
public class GoalComputeTypeRangeTest {

    public static List<Object[]> parametersForComputeType_success() {
        // Внимание! Эти значения могут меняться только при изменении границ диапазонов ID со стороны Метрики
        return asList(new Object[][]{
                {100L, GoalType.GOAL},
                {999_999_999L, GoalType.GOAL},
                {1_000_000_000L, GoalType.SEGMENT},
                {1_499_999_999L, GoalType.SEGMENT},
                {1_500_000_000L, GoalType.LAL_SEGMENT},
                {1_899_999_999L, GoalType.LAL_SEGMENT},
                {1_900_000_000L, GoalType.MOBILE},
                {1_999_999_999L, GoalType.MOBILE},
                {2_000_000_000L, GoalType.AUDIENCE},
                {2_498_999_999L, GoalType.AUDIENCE},
                // 1М ID зарезервированы под кастомные сегменты, создаваемые Директом
                // Подробнее: https://wiki.yandex-team.ru/users/aliho/projects/direct/crypta/
                {2_500_000_000L, GoalType.AB_SEGMENT},
                {2_599_999_999L, GoalType.AB_SEGMENT},
                {2_600_000_000L, GoalType.CDP_SEGMENT},
                {2_999_999_999L, GoalType.CDP_SEGMENT},
                {3_000_000_000L, GoalType.ECOMMERCE},
                {3_899_999_999L, GoalType.ECOMMERCE},
                {4_000_000_000L, GoalType.GOAL}
        });
    }

    @Test
    @Parameters
    public void computeType_success(Long id, GoalType expected) {
        assertThat(Goal.computeType(id))
                .isEqualTo(expected);
    }
}
