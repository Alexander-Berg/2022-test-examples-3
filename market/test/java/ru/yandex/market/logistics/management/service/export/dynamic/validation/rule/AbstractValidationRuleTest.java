package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;

import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.Result;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;


abstract class AbstractValidationRuleTest<T> {

    void assertValidationResult(T entity, ValidationStatus status, String error) {
        Result actual = getRule().test(entity);
        Assert.assertEquals(status, actual.getStatus());

        if (actual.isNotSuccessful()) {
            MatcherAssert.assertThat(actual.getReason(), Matchers.containsString(error));
        }
    }

    abstract ValidationRule getRule();
}
