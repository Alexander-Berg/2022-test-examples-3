package ru.yandex.direct.grid.processing.service.attributes;


import org.junit.Test;

import ru.yandex.direct.abac.Attribute;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что для каждого аттрибута {@link Attribute} есть соответствующий предикат в {@link AttributeRules}
 */
public class AttributeRulesTest {

    @Test
    public void checkAttributePredicates_exists() {
        assertThat(AttributeRules.RULES.keySet())
                .containsExactlyInAnyOrder(Attribute.values());
    }

}
