package ru.yandex.market.mboc.common.offers.acceptance.rule;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;


public class CategoryRuleRepositoryTest extends BaseDbTestClass {
    @Autowired
    private CategoryRuleRepository repository;

    @Test
    public void testRepository() {
        assertThat(repository.findAll()).isEmpty();

        repository.insert(List.of(
            new CategoryRule(1, 1, 0),
            new CategoryRule(2, 1, 0),
            new CategoryRule(1, 1, 1),
            new CategoryRule(1, 2, 1)
        ));

        assertThat(repository.findAll()).containsExactlyInAnyOrder(
            new CategoryRule(1, 1, 0),
            new CategoryRule(2, 1, 0),
            new CategoryRule(1, 1, 1),
            new CategoryRule(1, 2, 1)
        );

        repository.clear();

        assertThat(repository.findAll()).isEmpty();
    }
}
