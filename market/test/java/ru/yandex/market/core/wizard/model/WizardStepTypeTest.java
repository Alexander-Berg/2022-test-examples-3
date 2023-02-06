package ru.yandex.market.core.wizard.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link WizardStepType}.
 *
 * @author Vladislav Bauer
 */
class WizardStepTypeTest {

    /**
     * Проверить что порядок элементов уникальный.
     */
    @Test
    void testOrderUniqueness() {
        final WizardStepType[] types = WizardStepType.values();
        final Set<Integer> orders = Arrays.stream(types)
                .map(WizardStepType::getOrder)
                .collect(Collectors.toSet());

        assertThat(orders, hasSize(types.length));
    }

}
