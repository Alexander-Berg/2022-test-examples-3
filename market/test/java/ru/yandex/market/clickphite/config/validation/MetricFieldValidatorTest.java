package ru.yandex.market.clickphite.config.validation;

import com.google.common.collect.Sets;
import org.junit.Test;

import ru.yandex.market.health.configs.clickphite.config.metric.MetricField;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 27.12.16
 */
public class MetricFieldValidatorTest {
    @Test(expected = IllegalStateException.class)
    public void invalidField() throws Exception {
        MetricFieldValidator.validateField(createField("resptime_ms"));
    }

    @Test(expected = IllegalStateException.class)
    public void invalidFieldInComplexExpression() throws Exception {
        MetricFieldValidator.validateField(createField("100 * resptime_ms / 2"));
    }

    @Test
    public void allowedFieldNames() throws Exception {
        MetricFieldValidator.validateField(createField("resptime_ms"), Sets.newHashSet("resptime_ms"));
    }

    @Test
    public void allowedFieldNamesInComplexExpression() throws Exception {
        MetricFieldValidator.validateField(createField("100 * resptime_ms / 2"), Sets.newHashSet("resptime_ms"));
    }

    @Test
    public void underAggregateFunction() throws Exception {
        MetricFieldValidator.validateField(createField("count(resptime_ms) / 100"));
    }

    private MetricField createField(final String fieldExpression) {
        return new MetricField() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getField() {
                return fieldExpression;
            }
        };
    }
}
