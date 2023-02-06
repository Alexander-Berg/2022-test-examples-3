package ru.yandex.market.ff.grid.validation.rule.provider.database;

import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.DocumentType;
import ru.yandex.market.ff.config.CustomGridValidationRules;
import ru.yandex.market.ff.grid.validation.rule.ColumnValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsArrayValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsIntegerValidationRule;
import ru.yandex.market.ff.grid.validation.rule.concrete.IsStringValidationRule;
import ru.yandex.market.ff.grid.validation.rule.custom.UniqueValidationRule;
import ru.yandex.market.ff.grid.validation.rule.dictionary.ValidationRulesDictionary;
import ru.yandex.market.ff.grid.validation.rule.provider.database.factory.ColumnValidationRuleFactory;
import ru.yandex.market.ff.i18n.TemplateValidationMessages;
import ru.yandex.market.ff.model.entity.DocumentTemplate;
import ru.yandex.market.ff.model.entity.DocumentTemplateColumn;

import static org.mockito.Mockito.mock;

/**
 * @author kotovdv 02/08/2017.
 */
class ValidationRulesDictionaryCacheLoaderTest {


    TemplateValidationMessages templateValidationMessages = mock(TemplateValidationMessages.class);
    private final ColumnValidationRuleFactory ruleFactory = new ColumnValidationRuleFactory(templateValidationMessages);
    private final CustomGridValidationRules customGridValidationRules =
            new CustomGridValidationRules(templateValidationMessages);

    private final ValidationRulesDictionaryCacheLoader cacheLoader =
            new ValidationRulesDictionaryCacheLoader(ruleFactory, customGridValidationRules);

    private final ValidationRulesDictionaryProvider ruleProvider = new ValidationRulesDictionaryProvider(cacheLoader);

    @BeforeEach
    void init() {
        ruleProvider.init();
    }


    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testRulesExtractionStringAndUnique() {

        DocumentTemplateColumn sku = new DocumentTemplateColumn(0, "Ваш SKU", "STRING[128]", true, true);


        ValidationRulesDictionary rules =
                ruleProvider.getRules(new DocumentTemplate(DocumentType.SUPPLY, List.of(sku), 2));

        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(rules.columnsSize())
                .as("Asserting amount of columns being validated by this dictionary")
                .isEqualTo(1);

        softAssertions.assertThat(rules.rulesSize())
                .as("Asserting amount of rules returned by provider")
                .isEqualTo(2);

        Optional<ColumnValidationRule> isStringValidationRuleOptional =
                rules.getColumnValidationRules().get(0).stream().filter(x -> x instanceof IsStringValidationRule)
                        .findFirst();

        Assertions.assertTrue(isStringValidationRuleOptional.isPresent(), "IsStringValidationRule should be provided");

        IsStringValidationRule isStringValidationRule = (IsStringValidationRule) isStringValidationRuleOptional.get();

        Optional<ColumnValidationRule> uniqueValidationRuleOptional =
                rules.getColumnValidationRules().get(0).stream().filter(x -> x instanceof UniqueValidationRule)
                        .findFirst();

        Assertions.assertTrue(uniqueValidationRuleOptional.isPresent(), "UniqueValidationRule should be provided");
        UniqueValidationRule uniqueValidationRule = (UniqueValidationRule) uniqueValidationRuleOptional.get();

        softAssertions.assertThat(isStringValidationRule.getMaxLength()).isEqualTo(128);
        softAssertions.assertThat(isStringValidationRule.isMandatory()).isTrue();
        softAssertions.assertThat(uniqueValidationRule).isNotNull();

        softAssertions.assertAll();
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testRulesExtractionInteger() {

        DocumentTemplateColumn name =
                new DocumentTemplateColumn(3, "Количество товаров в поставке", "INTEGER", true, false);

        ValidationRulesDictionary rules =
                ruleProvider.getRules(new DocumentTemplate(DocumentType.SUPPLY, List.of(name), 2));

        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(rules.columnsSize())
                .as("Asserting amount of columns being validated by this dictionary")
                .isEqualTo(1);

        softAssertions.assertThat(rules.rulesSize())
                .as("Asserting amount of rules returned by provider")
                .isEqualTo(1);

        Optional<ColumnValidationRule> isIntegerValidationRuleOptional =
                rules.getColumnValidationRules().get(3).stream().filter(x -> x instanceof IsIntegerValidationRule)
                        .findFirst();

        Assertions
                .assertTrue(isIntegerValidationRuleOptional.isPresent(), "IsIntegerValidationRule should be provided");
        IsIntegerValidationRule isIntegerValidationRule =
                (IsIntegerValidationRule) isIntegerValidationRuleOptional.get();
        softAssertions.assertThat(isIntegerValidationRule.isMandatory()).isTrue();

        softAssertions.assertAll();
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    void testRulesExtractionArray() {

        DocumentTemplateColumn name = new DocumentTemplateColumn(2, "Штрихкоды", "ARRAY[STRING[128]][,]", false, false);

        ValidationRulesDictionary rules =
                ruleProvider.getRules(new DocumentTemplate(DocumentType.SUPPLY, List.of(name), 2));

        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(rules.columnsSize())
                .as("Asserting amount of columns being validated by this dictionary")
                .isEqualTo(1);

        softAssertions.assertThat(rules.rulesSize())
                .as("Asserting amount of rules returned by provider")
                .isEqualTo(1);

        Optional<ColumnValidationRule> isArrayValidationRuleOptional =
                rules.getColumnValidationRules().get(2).stream().filter(x -> x instanceof IsArrayValidationRule)
                        .findFirst();

        Assertions.assertTrue(isArrayValidationRuleOptional.isPresent(), "IsArrayValidationRule should be provided");
        IsArrayValidationRule isArrayValidationRule = (IsArrayValidationRule) isArrayValidationRuleOptional.get();
        softAssertions.assertThat(isArrayValidationRule.isMandatory()).isFalse();
        softAssertions.assertThat(isArrayValidationRule.getDelimiter()).isEqualTo(",");
        softAssertions
                .assertThat(((IsStringValidationRule) isArrayValidationRule.getElementValidationRule()).getMaxLength())
                .isEqualTo(128);

        softAssertions.assertAll();
    }
}
