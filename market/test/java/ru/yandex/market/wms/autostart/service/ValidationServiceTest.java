package ru.yandex.market.wms.autostart.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.validation.ValidationResult;
import ru.yandex.market.wms.autostart.validation.ValidationRule;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ValidationServiceTest extends BaseTest {

    PassedRuleMock passedRule = new PassedRuleMock();
    FailedRuleMock failedRule = new FailedRuleMock();
    AnotherFailedRuleMock anotherFailedRule = new AnotherFailedRuleMock();
    private final OrderDao orderDao = mock(OrderDao.class);

    @Test
    public void validateReturnsOnlyFailedResults() {
        doReturn(List.of(
                Order.builder().orderKey("one").build(),
                Order.builder().orderKey("two").build()
        )).when(orderDao).findOrdersByOrderKeys(Arrays.asList("one", "two"));

        List<ValidationRule> validationRules = Arrays.asList(passedRule, failedRule, anotherFailedRule);
        ValidationService validationService = new ValidationService(orderDao, validationRules);

        List<ValidationResult> results = validationService.validate(Arrays.asList("one", "two"));
        Optional<ValidationResult> failedRuleMockResult = results.stream()
                .filter(x -> x.getRuleName().equals("FailedRuleMock"))
                .findFirst();
        Optional<ValidationResult> anotherFailedRuleMockResult = results.stream()
                .filter(x -> x.getRuleName().equals("AnotherFailedRuleMock"))
                .findFirst();

        assertions.assertThat(results.size()).isEqualTo(2);
        assertions.assertThat(failedRuleMockResult.isPresent()).isTrue();
        assertions.assertThat(anotherFailedRuleMockResult.isPresent()).isTrue();
        assertions.assertThat(failedRuleMockResult.get().isOk()).isFalse();
        assertions.assertThat(failedRuleMockResult.get().getMessage()).isEqualTo("failed_rule_msg");
        assertions.assertThat(anotherFailedRuleMockResult.get().isOk()).isFalse();
        assertions.assertThat(anotherFailedRuleMockResult.get().getMessage()).isEqualTo("another_failed_rule_msg");
    }

    @Test
    public void validateAllRulesPassedReturnsEmptyResult() {
        doReturn(List.of(
                Order.builder().orderKey("one").build(),
                Order.builder().orderKey("two").build()
        )).when(orderDao).findOrdersByOrderKeys(Arrays.asList("one", "two"));

        List<ValidationRule> validationRules = Arrays.asList(passedRule, passedRule);
        ValidationService validationService = new ValidationService(orderDao, validationRules);

        List<ValidationResult> results = validationService.validate(Arrays.asList("one", "two"));

        assertions.assertThat(results.isEmpty()).isTrue();
    }

    private class PassedRuleMock implements ValidationRule {

        @Override
        public ValidationResult validate(List<Order> orders) {
            return ValidationResult.createOkResult(getName());
        }

        @Override
        public String getName() {
            return "PassedRuleMock";
        }
    }

    private class FailedRuleMock implements ValidationRule {

        @Override
        public ValidationResult validate(List<Order> orders) {
            return ValidationResult.createFailedResult(getName(), "failed_rule_msg");
        }

        @Override
        public String getName() {
            return "FailedRuleMock";
        }
    }

    private class AnotherFailedRuleMock implements ValidationRule {

        @Override
        public ValidationResult validate(List<Order> orders) {
            return ValidationResult.createFailedResult(getName(), "another_failed_rule_msg");
        }

        @Override
        public String getName() {
            return "AnotherFailedRuleMock";
        }
    }
}
