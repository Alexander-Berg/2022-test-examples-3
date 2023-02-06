package ru.yandex.direct.core.entity.performancefilter.utils;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class PerformanceFilterUtilsTest {

    @Test
    public void validAndEqual_success_firstArgNull() {
        assertThatCode(() -> PerformanceFilterUtils.validAndEqual(null, emptyList()))
                .doesNotThrowAnyException();
    }

    @Test
    public void validAndEqual_success_secondArgNull() {
        assertThatCode(() -> PerformanceFilterUtils.validAndEqual(emptyList(), null))
                .doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validAndEqual_success_withAvailableFalse() {
        // "available": false не сохраняем при сериализации
        PerformanceFilterCondition condition = new PerformanceFilterCondition("available", Operator.EQUALS, "false");
        condition.setParsedValue(false);
        List<PerformanceFilterCondition> first =
                singletonList(condition);
        List<PerformanceFilterCondition> second = emptyList();
        boolean actual = PerformanceFilterUtils.validAndEqual(first, second);
        assertThat(actual).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validAndEqual_false_onDuplicatedFieldOperator() {
        PerformanceFilterCondition condition = new PerformanceFilterCondition("price", Operator.EQUALS, "[\"10\"]");
        condition.setParsedValue(singletonList(10L));
        PerformanceFilterCondition conditionDuplicate =
                new PerformanceFilterCondition("price", Operator.EQUALS, "[\"20\"]");
        conditionDuplicate.setParsedValue(singletonList(20L));
        List<PerformanceFilterCondition> conditions = asList(condition, conditionDuplicate);
        // при наличии дублирующихся пар поле-оператор должны безусловно возвращать false
        boolean actual = PerformanceFilterUtils.validAndEqual(conditions, conditions);
        assertThat(actual).isFalse();
    }

}
