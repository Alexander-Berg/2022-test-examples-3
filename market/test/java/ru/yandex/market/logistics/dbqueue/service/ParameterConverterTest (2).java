package ru.yandex.market.logistics.dbqueue.service;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.dbqueue.BaseTest;
import ru.yandex.market.logistics.dbqueue.domain.condition.Comparison;
import ru.yandex.market.logistics.dbqueue.domain.condition.SimpleSqlCondition;
import ru.yandex.market.logistics.dbqueue.dto.DbQueueTaskFilter;

class ParameterConverterTest extends BaseTest {
    private TaskParameterConverter taskParameterConverter;

    @BeforeEach
    void setUp() {
        taskParameterConverter = new TaskParameterConverter();
    }

    @Test
    public void testConvertToEntityFilterParams() {
        List<SimpleSqlCondition<?>> filter = taskParameterConverter.convertToEntityFilterParams(
            new DbQueueTaskFilter().setAttempt(10L));
        softly.assertThat(filter).isEqualTo(Collections.singletonList(
            new SimpleSqlCondition<>("attempt", Comparison.EQ, 10L)
        ));
    }

    @Test
    public void testParameterNameConversion() {
        List<SimpleSqlCondition<?>> filter = taskParameterConverter.convertToEntityFilterParams(
            new DbQueueTaskFilter().setQueueName("test"));
        softly.assertThat(filter).isEqualTo(Collections.singletonList(
            new SimpleSqlCondition<>("queue_name", Comparison.LIKE, "test%")
        ));
    }

}
