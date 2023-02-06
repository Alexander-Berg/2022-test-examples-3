package ru.yandex.market.mbo.gurulight.template.conditional;

import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.gurulight.template.RuleTemplateHelper;
import ru.yandex.market.mbo.gwt.models.rules.gl.templates.ConditionalRulesTemplate;
import ru.yandex.market.mbo.gwt.models.rules.gl.templates.CategoryRulesTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author commince
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class GLRuleTestHelper {
    static final long CONDITIONAL_PARAM_ID = 1;
    static final long MAIN_PARAM_ID = 2;
    static final long DEFINITION_PARAM_ID = 3;
    static final long HID = 0;

    private GLRuleTestHelper() {
    }

    /*
         * Тестовый шаблон:
         * CONDITIONAL      MAIN     ALIASES             DEFINITON
         * 11               21       223                 31, 32
         * 12               21       223, 224            33, 34
         * 12               22       224                 34, 35, 36
         *
         */
    public static List<ConditionalRulesTemplate> generateConditionalTestTemplates() {
        List<ConditionalRulesTemplate> result = new ArrayList<>();

        result.add(generateConditionalTemplate(11,
            21,
            Arrays.asList(223L),
            Arrays.asList(31L, 32L)));

        result.add(generateConditionalTemplate(12,
            21,
            Arrays.asList(223L, 225L),
            Arrays.asList(33L, 34L)));

        result.add(generateConditionalTemplate(12,
            22,
            Arrays.asList(224L),
            Arrays.asList(34L, 35L, 36L)));

        return result;
    }

    /*
     * Тестовый шаблон:
     * MAIN     ALIASES             DEFINITON
     * 21       223                 31, 32
     * 22       224, 225            33, 34
     * 23       226                 34, 35, 36
     *
     */
    public static List<CategoryRulesTemplate> generateCategoryTestTemplates() {
        List<CategoryRulesTemplate> result = new ArrayList<>();

        result.add(generateCategoryTemplate(21,
            Arrays.asList(223L),
            Arrays.asList(31L, 32L)));

        result.add(generateCategoryTemplate(22,
            Arrays.asList(224L, 225L),
            Arrays.asList(33L, 34L)));

        result.add(generateCategoryTemplate(23,
            Arrays.asList(226L),
            Arrays.asList(34L, 35L, 36L)));

        return result;
    }

    public static ConditionalRulesTemplate generateConditionalTemplate(long conditionOptionId,
                                                                       long mainOptionId,
                                                                       List<Long> aliasIds,
                                                                       List<Long> definitionIds) {

        ConditionalRulesTemplate result = new ConditionalRulesTemplate();
        result.setCategoryHid(HID);
        result.setConditionalParamId(CONDITIONAL_PARAM_ID);
        result.setMainParamId(MAIN_PARAM_ID);
        result.setDefinitionParamId(DEFINITION_PARAM_ID);

        result.setConditionalParamOptionId(conditionOptionId);
        result.setMainParamOptionId(mainOptionId);
        result.setMainParamAliasOptionIds(aliasIds);
        result.setDefinitionParamOptionIds(definitionIds);

        return result;
    }



    public static CategoryRulesTemplate generateCategoryTemplate(long mainOptionId,
                                                                 List<Long> aliasIds,
                                                                 List<Long> definitionIds) {

        CategoryRulesTemplate result = new CategoryRulesTemplate();
        result.setCategoryHid(HID);
        result.setMainParamId(MAIN_PARAM_ID);
        result.setDefinitionParamId(DEFINITION_PARAM_ID);

        result.setMainParamOptionId(mainOptionId);
        result.setMainParamAliasOptionIds(aliasIds);
        result.setDefinitionParamOptionIds(definitionIds);

        return result;
    }

    /*
     *
     * Рулы:
     * CONDITIONAL
     * if:
     * conditional = 11
     * then
     * revoke all but (21)
     * if:
     * conditional = 12
     * then
     * revoke all but (21, 22)
     *
     * ALIAS
     * if:
     * conditional = 11
     * main = 223
     * then
     * main = 21
     * if:
     * conditional = 12
     * main = 223
     * then
     * main = 21
     * if:
     * conditional = 12
     * main = 224
     * then
     * main = 21
     * if:
     * conditional = 12
     * main = 224
     * then
     * main = 22
     *
     * DEFINITION
     * if:
     * conditional = 11
     * main = 21
     * then
     * definition = 32
     * definition = 33
     * if:
     * conditional = 12
     * main = 21
     * then
     * definition = 33
     * definition = 34
     * if:
     * conditional = 12
     * main = 22
     * then
     * definition = 34
     * definition = 35
     * definition = 36
     */

    public static List<GLRule> generateConditionalTestRules() {
        List<GLRule> result = new ArrayList<>();

        //CONDITION
        result.add(new GLRuleBuilder(GLRuleType.CONDITIONAL_VALUES,
            RuleTemplateHelper.CONDITION_ALLOWED_VALUES_RULE_WEIGTH)
            .addIf(CONDITIONAL_PARAM_ID, 11L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, null, GLRulePredicate.VALUE_UNDEFINED, Arrays.asList(21L))
            .addThen(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.SET_PROCESSING_TAG)
            .build());
        result.add(new GLRuleBuilder(GLRuleType.CONDITIONAL_VALUES,
            RuleTemplateHelper.CONDITION_ALLOWED_VALUES_RULE_WEIGTH)
            .addIf(CONDITIONAL_PARAM_ID, 12L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, null, GLRulePredicate.VALUE_UNDEFINED, Arrays.asList(21L, 22L))
            .addThen(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.SET_PROCESSING_TAG)
            .build());

        //ALIAS
        result.add(new GLRuleBuilder(GLRuleType.CONDITIONAL_ALIASES, RuleTemplateHelper.CONDITION_ALIAS_RULE_WEIGTH)
            .addIf(CONDITIONAL_PARAM_ID, 11L, GLRulePredicate.ENUM_MATCHES)
            .addIf(MAIN_PARAM_ID, 223L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, 21L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CONDITIONAL_ALIASES, RuleTemplateHelper.CONDITION_ALIAS_RULE_WEIGTH)
            .addIf(CONDITIONAL_PARAM_ID, 12L, GLRulePredicate.ENUM_MATCHES)
            .addIf(MAIN_PARAM_ID, 223L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, 21L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CONDITIONAL_ALIASES, RuleTemplateHelper.CONDITION_ALIAS_RULE_WEIGTH)
            .addIf(CONDITIONAL_PARAM_ID, 12L, GLRulePredicate.ENUM_MATCHES)
            .addIf(MAIN_PARAM_ID, 224L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, 21L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CONDITIONAL_ALIASES, RuleTemplateHelper.CONDITION_ALIAS_RULE_WEIGTH)
            .addIf(CONDITIONAL_PARAM_ID, 12L, GLRulePredicate.ENUM_MATCHES)
            .addIf(MAIN_PARAM_ID, 224L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, 22L, GLRulePredicate.ENUM_MATCHES)
            .build());

        //DEFINITION
        result.add(new GLRuleBuilder(GLRuleType.CONDITIONAL_DEFINITIONS,
            RuleTemplateHelper.CONDITION_DEFINITION_RULE_WEIGTH)
            .addIf(CONDITIONAL_PARAM_ID, 11L, GLRulePredicate.ENUM_MATCHES)
            .addIf(MAIN_PARAM_ID, 21L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 31L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 32L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CONDITIONAL_DEFINITIONS,
            RuleTemplateHelper.CONDITION_DEFINITION_RULE_WEIGTH)
            .addIf(CONDITIONAL_PARAM_ID, 12L, GLRulePredicate.ENUM_MATCHES)
            .addIf(MAIN_PARAM_ID, 21L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 33L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 34L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CONDITIONAL_DEFINITIONS,
            RuleTemplateHelper.CONDITION_DEFINITION_RULE_WEIGTH)
            .addIf(CONDITIONAL_PARAM_ID, 12L, GLRulePredicate.ENUM_MATCHES)
            .addIf(MAIN_PARAM_ID, 22L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 34L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 35L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 36L, GLRulePredicate.ENUM_MATCHES)
            .build());

        return result;
    }

    /*
     *
     * Рулы:
     * CONDITIONAL
     * if:
     * null
     * then
     * revoke all but (21, 22, 23)
     *
     * ALIAS
     * if:
     * main = 223
     * then
     * main = 21
     * if:
     * main = 224
     * then
     * main = 22
     * if:
     * main = 225
     * then
     * main = 22
     * if:
     * main = 226
     * then
     * main = 23
     *
     * DEFINITION
     * if:
     * main = 21
     * then
     * definition = 31
     * definition = 32
     * if:
     * main = 22
     * then
     * definition = 33
     * definition = 34
     * if:
     * main = 23
     * then
     * definition = 34
     * definition = 35
     * definition = 36
     */
    public static List<GLRule> generateCategoryTestRules() {
        List<GLRule> result = new ArrayList<>();

        //CONDITION
        result.add(new GLRuleBuilder(GLRuleType.CATEGORY_VALUES, RuleTemplateHelper.CATEGORY_ALLOWED_VALUES_RULE_WEIGTH)
            .addIf(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.HAS_NO_PROCESSING_TAG)
            .addThen(MAIN_PARAM_ID, null, GLRulePredicate.VALUE_UNDEFINED, Arrays.asList(21L, 22L, 23L))
            .build());

        //ALIAS
        result.add(new GLRuleBuilder(GLRuleType.CATEGORY_ALIASES, RuleTemplateHelper.CATEGORY_ALIAS_RULE_WEIGTH)
            .addIf(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.HAS_NO_PROCESSING_TAG)
            .addIf(MAIN_PARAM_ID, 223L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, 21L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CATEGORY_ALIASES, RuleTemplateHelper.CATEGORY_ALIAS_RULE_WEIGTH)
            .addIf(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.HAS_NO_PROCESSING_TAG)
            .addIf(MAIN_PARAM_ID, 224L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, 22L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CATEGORY_ALIASES, RuleTemplateHelper.CATEGORY_ALIAS_RULE_WEIGTH)
            .addIf(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.HAS_NO_PROCESSING_TAG)
            .addIf(MAIN_PARAM_ID, 225L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, 22L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CATEGORY_ALIASES, RuleTemplateHelper.CATEGORY_ALIAS_RULE_WEIGTH)
            .addIf(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.HAS_NO_PROCESSING_TAG)
            .addIf(MAIN_PARAM_ID, 226L, GLRulePredicate.ENUM_MATCHES)
            .addThen(MAIN_PARAM_ID, 23L, GLRulePredicate.ENUM_MATCHES)
            .build());

        //DEFINITION
        result.add(new GLRuleBuilder(GLRuleType.CATEGORY_DEFINITIONS,
            RuleTemplateHelper.CATEGORY_DEFINITION_RULE_WEIGTH)
            .addIf(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.HAS_NO_PROCESSING_TAG)
            .addIf(MAIN_PARAM_ID, 21L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 31L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 32L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CATEGORY_DEFINITIONS,
            RuleTemplateHelper.CATEGORY_DEFINITION_RULE_WEIGTH)
            .addIf(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.HAS_NO_PROCESSING_TAG)
            .addIf(MAIN_PARAM_ID, 22L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 33L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 34L, GLRulePredicate.ENUM_MATCHES)
            .build());

        result.add(new GLRuleBuilder(GLRuleType.CATEGORY_DEFINITIONS,
            RuleTemplateHelper.CATEGORY_DEFINITION_RULE_WEIGTH)
            .addIf(String.valueOf(MAIN_PARAM_ID), GLRulePredicate.HAS_NO_PROCESSING_TAG)
            .addIf(MAIN_PARAM_ID, 23L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 34L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 35L, GLRulePredicate.ENUM_MATCHES)
            .addThen(DEFINITION_PARAM_ID, 36L, GLRulePredicate.ENUM_MATCHES)
            .build());

        return result;
    }

    public static boolean rulesEqualForTest(GLRule rule1, GLRule rule2) {
        if (rule1 == rule2) {
            return true;
        }
        return rule1.getId() == rule2.getId() &&
            rule1.getWeight() == rule2.getWeight() &&
            rule1.isPublished() == rule2.isPublished() &&
            rule1.getHid() == rule2.getHid() &&
            rule1.getType() == rule2.getType() &&

            rule1.getIfs().size() == rule2.getIfs().size() &&
            rule1.getIfs().containsAll(rule2.getIfs()) &&
            rule1.getThens().size() == rule2.getThens().size() &&
            rule1.getThens().containsAll(rule2.getThens());
    }

    public static boolean rulesContainAll(List<GLRule> source, List<GLRule> rules) {
        for (GLRule rule : rules) {
            if (!rulesContain(source, rule)) {
                return false;
            }
        }

        return true;
    }

    public static boolean rulesContain(List<GLRule> source, GLRule glRule) {
        for (GLRule sourceRule : source) {
            if (ruleEqIgnoreId(sourceRule, glRule)) {
                return true;
            }
        }

        return false;
    }

    public static boolean ruleEqIgnoreId(GLRule r1, GLRule r2) {
        if (r1 == r2) {
            return true;
        }
        if (r1 == null || r2 == null || r2.getClass() != r1.getClass()) {
            return false;
        }
        return r2.getWeight() == r1.getWeight() &&
            r2.isPublished() == r1.isPublished() &&
            r2.getHid() == r1.getHid() &&
            r2.getType() == r1.getType() &&
            Objects.equals(r2.getName(), r1.getName()) &&
            Objects.equals(r2.getIfs().size(), r1.getIfs().size()) &&
            r2.getIfs().containsAll(r1.getIfs()) &&
            Objects.equals(r2.getThens().size(), r1.getThens().size()) &&
            r2.getThens().containsAll(r1.getThens());
    }
}
