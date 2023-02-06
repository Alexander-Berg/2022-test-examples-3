package ru.yandex.market.ff.grid.validation.rule.provider.database.factory;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsIntegerValidationRule;
import ru.yandex.market.ff.i18n.TemplateValidationMessages;
import ru.yandex.market.ff.model.entity.DocumentTemplateColumn;

import static org.mockito.Mockito.mock;

/**
 * @author kotovdv 02/08/2017.
 */
class ColumnValidationRuleFactoryTest {

    private final ColumnValidationRuleFactory ruleFactory =
            new ColumnValidationRuleFactory(mock(TemplateValidationMessages.class));

    private SoftAssertions softly;

    @BeforeEach
    void beforeTest() {
        this.softly = new SoftAssertions();
    }

    @AfterEach
    private void afterTest() {
        this.softly.assertAll();
    }

    @Test
    void testMandatoryRuleCreation() {
        DocumentTemplateColumn documentTemplate = new DocumentTemplateColumn(0,
                "",
                "INTEGER",
                true,
                true);

        ColumnValidationRule rule = ruleFactory.create(documentTemplate);

        softly.assertThat(rule)
                .as("Asserting that rule exists")
                .isNotNull();

        softly.assertThat(rule.getClass())
                .as("Asserting that rule is IntegerValidationRule")
                .isEqualTo(IsIntegerValidationRule.class);

        softly.assertThat(rule.isMandatory())
                .as("Asserting that rule is mandatory")
                .isTrue();
    }
}
