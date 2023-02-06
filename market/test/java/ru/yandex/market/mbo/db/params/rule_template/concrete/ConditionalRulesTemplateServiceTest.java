package ru.yandex.market.mbo.db.params.rule_template.concrete;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.db.params.GLRulesServiceMock;
import ru.yandex.market.mbo.gwt.models.rules.gl.RuleTemplateModifications;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.gurulight.template.conditional.ConditionalRuleGenerator;
import ru.yandex.market.mbo.gurulight.template.conditional.GLRuleTestHelper;
import ru.yandex.market.mbo.gwt.models.rules.gl.templates.ConditionalRulesTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.gurulight.template.conditional.GLRuleTestHelper.rulesContainAll;

/**
 * Created by commince on 05.06.17.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class ConditionalRulesTemplateServiceTest {

    ConditionalRulesTemplateService service = new ConditionalRulesTemplateService();

    @Mock
    private TransactionTemplate contentTx;

    private List<GLRule> rulesStorage;

    @Before
    public void setup() {
        GLRulesServiceMock rulesServiceMock = new GLRulesServiceMock();
        List<GLRule> testRules =
            new ConditionalRuleGenerator(GLRuleTestHelper.generateConditionalTestTemplates()).generateAllRules();

        for (GLRule rule : testRules) {
            rulesServiceMock.addRule(rule, 0L);
        }
        rulesStorage = rulesServiceMock.getRules();

        service.rulesService = rulesServiceMock;
        service.contentTx = this.contentTx;

        when(contentTx.execute(Mockito.<TransactionCallback>any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            TransactionCallback arg = (TransactionCallback) args[0];
            return arg.doInTransaction(new SimpleTransactionStatus());
        });
    }

    @Test
    public void testAdd() {
        ConditionalRulesTemplate add =
            GLRuleTestHelper.generateConditionalTemplate(12,
                23,
            Arrays.asList(227L),
            Arrays.asList(36L, 37L));

        Assert.assertFalse(
            rulesContainAll(rulesStorage,
                new ConditionalRuleGenerator(Collections.singletonList(add)).generateBaseRules())
        );

        Assert.assertFalse(templateIncludedInAllowedValues(rulesStorage, add));

        service.add(Collections.singletonList(add), 0L);

        Assert.assertTrue(
            rulesContainAll(rulesStorage,
                new ConditionalRuleGenerator(Collections.singletonList(add)).generateBaseRules())
        );

        Assert.assertTrue(templateIncludedInAllowedValues(rulesStorage, add));
    }

    @Test
    public void testRemove() {
        List<ConditionalRulesTemplate> testTemplates = GLRuleTestHelper.generateConditionalTestTemplates();

        ConditionalRulesTemplate remove = testTemplates.get(0);

        Assert.assertTrue(
            rulesContainAll(rulesStorage,
                new ConditionalRuleGenerator(Collections.singletonList(remove)).generateBaseRules())
        );

        Assert.assertTrue(templateIncludedInAllowedValues(rulesStorage, remove));

        service.remove(Collections.singletonList(remove), 0L);

        Assert.assertFalse(
            rulesContainAll(rulesStorage,
                new ConditionalRuleGenerator(Collections.singletonList(remove)).generateBaseRules())
        );

        Assert.assertFalse(templateIncludedInAllowedValues(rulesStorage, remove));
    }

    @Test
    public void testModify() {
        List<ConditionalRulesTemplate> testTemplates = GLRuleTestHelper.generateConditionalTestTemplates();

        ConditionalRulesTemplate oldTemplate = testTemplates.get(0);
        ConditionalRulesTemplate newTemplate = new ConditionalRulesTemplate(oldTemplate);
        newTemplate.setMainParamOptionId(123L);

        RuleTemplateModifications.Modification modification =
            new RuleTemplateModifications.Modification(oldTemplate, newTemplate);

        Assert.assertTrue(
            rulesContainAll(rulesStorage,
                new ConditionalRuleGenerator(Collections.singletonList(oldTemplate)).generateBaseRules())
        );
        Assert.assertTrue(templateIncludedInAllowedValues(rulesStorage, oldTemplate));

        Assert.assertFalse(
            rulesContainAll(rulesStorage,
                new ConditionalRuleGenerator(Collections.singletonList(newTemplate)).generateBaseRules())
        );
        Assert.assertFalse(templateIncludedInAllowedValues(rulesStorage, newTemplate));

        service.modify(Collections.singletonList(modification), 0L);

        Assert.assertFalse(
            rulesContainAll(rulesStorage,
                new ConditionalRuleGenerator(Collections.singletonList(oldTemplate)).generateBaseRules())
        );
        Assert.assertFalse(templateIncludedInAllowedValues(rulesStorage, oldTemplate));

        Assert.assertTrue(
            rulesContainAll(rulesStorage,
                new ConditionalRuleGenerator(Collections.singletonList(newTemplate)).generateBaseRules())
        );
        Assert.assertTrue(templateIncludedInAllowedValues(rulesStorage, newTemplate));
    }

    @Test
    public void testProcessModification() {
        List<ConditionalRulesTemplate> testTemplates = GLRuleTestHelper.generateConditionalTestTemplates();

        ConditionalRulesTemplate oldTemplateForModification = testTemplates.get(0);
        ConditionalRulesTemplate newTemplateForModification = new ConditionalRulesTemplate(oldTemplateForModification);
        newTemplateForModification.setMainParamOptionId(123L);

        RuleTemplateModifications.Modification modification =
            new RuleTemplateModifications.Modification(oldTemplateForModification, newTemplateForModification);

        ConditionalRulesTemplate forRemove = testTemplates.get(1);
        ConditionalRulesTemplate forAddition =
            GLRuleTestHelper.generateConditionalTemplate(12,
                124,
                Arrays.asList(223L),
                Arrays.asList(31L, 32L));

        RuleTemplateModifications modifications = new RuleTemplateModifications();
        modifications.getModify().add(modification);
        modifications.getAdd().add(forAddition);
        modifications.getRemove().add(forRemove);

        List<ConditionalRulesTemplate> totalAdd = new ArrayList<>();
        totalAdd.addAll((List<ConditionalRulesTemplate>) (List<?>) modifications.getAdd());
        totalAdd.add((ConditionalRulesTemplate) modifications.getModify().get(0).getNewTemplate());

        List<ConditionalRulesTemplate> totalRemove = new ArrayList<>();
        totalRemove.addAll((List<ConditionalRulesTemplate>) (List<?>) modifications.getRemove());
        totalRemove.add((ConditionalRulesTemplate) modifications.getModify().get(0).getOldTemplate());

        Assert.assertFalse(
            rulesContainAll(rulesStorage, new ConditionalRuleGenerator(totalAdd).generateBaseRules())
        );
        Assert.assertFalse(templatesIncludedInAllowedValues(rulesStorage, totalAdd));

        Assert.assertTrue(
            rulesContainAll(rulesStorage, new ConditionalRuleGenerator(totalRemove).generateBaseRules())
        );
        Assert.assertTrue(templatesIncludedInAllowedValues(rulesStorage, totalRemove));

        service.processModifications(modifications, 0L);

        Assert.assertTrue(
            rulesContainAll(rulesStorage, new ConditionalRuleGenerator(totalAdd).generateBaseRules())
        );
        Assert.assertTrue(templatesIncludedInAllowedValues(rulesStorage, totalAdd));

        Assert.assertFalse(
            rulesContainAll(rulesStorage, new ConditionalRuleGenerator(totalRemove).generateBaseRules())
        );
        Assert.assertFalse(templatesIncludedInAllowedValues(rulesStorage, totalRemove));
    }

    private static boolean templateIncludedInAllowedValues(List<GLRule> source, ConditionalRulesTemplate template) {
        return source.stream().filter(o ->
            o.getType() == GLRuleType.CONDITIONAL_VALUES &&
                o.getIfs().size() == 1 &&
                o.getIfs().iterator().next().getValueId() == template.getConditionalParamOptionId() &&
                o.getThens().stream()
                    .filter(
                        oo -> oo.getCondition().equals(GLRulePredicate.VALUE_UNDEFINED) &&
                            oo.getExcludeRevokeValueIds().contains(template.getMainParamOptionId()))
                    .count() == 1).count() == 1;
    }

    private static boolean templatesIncludedInAllowedValues(List<GLRule> source,
                                                            List<ConditionalRulesTemplate> templates) {

        for (ConditionalRulesTemplate template : templates) {
            if (!templateIncludedInAllowedValues(source, template)) {
                return false;
            }
        }

        return true;
    }

}
