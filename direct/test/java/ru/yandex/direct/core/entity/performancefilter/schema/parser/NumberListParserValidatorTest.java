package ru.yandex.direct.core.entity.performancefilter.schema.parser;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilterCondition;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class NumberListParserValidatorTest {

    @Test
    public void validate_error_whenEmpty() {
        PerformanceFilterCondition<List<Double>> condition =
                new PerformanceFilterCondition<>("old_price", Operator.GREATER, "[]");
        condition.setParsedValue(emptyList());
        ValidationResult<PerformanceFilterCondition, Defect> result = NumberListParser.INSTANCE
                .validate(condition);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("parsed_value")),
                        CollectionDefects.notEmptyCollection()))));
    }
}
