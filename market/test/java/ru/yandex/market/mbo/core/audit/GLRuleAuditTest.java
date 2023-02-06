package ru.yandex.market.mbo.core.audit;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleImpl;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author dmserebr
 */
@SuppressWarnings("checkstyle:magicnumber")
public class GLRuleAuditTest {
    private AuditServiceMock auditService;

    @Before
    public void before() throws Exception {
        auditService = new AuditServiceMock();
    }

    @Test
    public void testGLRuleAuditActions() throws Exception {
        GLRule glRule = createTestRule("TEST1");
        auditService.glRuleCreate(0L, glRule);
        List<AuditAction> auditActions = auditService.loadAudit(0, Integer.MAX_VALUE, new AuditFilter());

        assertEquals(5, auditActions.size());
        assertEquals(1, auditActions.stream()
            .filter(action -> action.getActionType().equals(AuditAction.ActionType.CREATE)).count());
        assertEquals(4, auditActions.stream()
            .filter(action -> action.getActionType().equals(AuditAction.ActionType.UPDATE)).count());
        assertTrue(auditActions.stream()
            .allMatch(action -> action.getEntityType().equals(AuditAction.EntityType.DEPENDENCY_RULE)));
        assertTrue(auditActions.stream().allMatch(action -> action.getEntityName().equals("TEST1")));
        assertEquals("TEST1", auditActions.stream()
            .filter(action -> action.getPropertyName().equals("Название")).findFirst().get().getNewValue());
        assertEquals("MANUAL", auditActions.stream()
            .filter(action -> action.getPropertyName().equals("Тип правила")).findFirst().get().getNewValue());
        assertEquals("Условие №1 {условие=MATCHES_NUMBER, субъект=PARAMETER, paramId=1, мин.значение=0, " +
                "макс.значение=0}; Условие №2 {условие=MISMATCHES, субъект=PARAMETER, paramId=3, valueId=4}",
            auditActions.stream().filter(action -> action.getPropertyName().equals("Условия"))
                .findFirst().get().getNewValue());
        assertEquals("Следствие №1 {условие=MATCHES_NUMBER, субъект=PARAMETER, paramId=1, мин.значение=0, " +
                "макс.значение=0}; Следствие №2 {условие=MISMATCHES, субъект=PARAMETER, paramId=3, valueId=4}",
            auditActions.stream().filter(action -> action.getPropertyName().equals("Следствия"))
                .findFirst().get().getNewValue());

        auditService.clearActions();

        GLRule glRule2 = createModifiedTestRule("TEST2");
        auditService.glRuleUpdate(0L, glRule, glRule2);
        auditActions = auditService.loadAudit(0, Integer.MAX_VALUE, new AuditFilter());

        assertEquals(2, auditActions.size());
        assertTrue(auditActions.stream()
            .allMatch(action -> action.getActionType().equals(AuditAction.ActionType.UPDATE)));
        assertTrue(auditActions.stream()
            .allMatch(action -> action.getEntityType().equals(AuditAction.EntityType.DEPENDENCY_RULE)));
        assertTrue(auditActions.stream()
            .allMatch(action -> action.getEntityName().equals("TEST2")));
        assertEquals("TEST1", auditActions.stream()
            .filter(action -> action.getPropertyName().equals("Название")).findFirst().get().getOldValue());
        assertEquals("TEST2", auditActions.stream()
            .filter(action -> action.getPropertyName().equals("Название")).findFirst().get().getNewValue());
        assertEquals("Условие №1 {условие=MATCHES_NUMBER, субъект=PARAMETER, paramId=1, мин.значение=0, " +
                "макс.значение=0}; Условие №2 {условие=MISMATCHES, субъект=PARAMETER, paramId=3, valueId=4}",
            auditActions.stream().filter(action -> action.getPropertyName().equals("Условия"))
                .findFirst().get().getOldValue());
        assertEquals("Условие №1 {условие=MATCHES_NUMBER, субъект=PARAMETER, paramId=1, мин.значение=0, " +
                "макс.значение=0}; Условие №2 {условие=MISMATCHES, субъект=PARAMETER, paramId=3, valueId=4}; " +
                "Условие №3 {условие=VALUE_UNDEFIND, субъект=PARAMETER, paramId=5}",
            auditActions.stream().filter(action -> action.getPropertyName().equals("Условия"))
                .findFirst().get().getNewValue());
        auditService.clearActions();

        auditService.glRuleDelete(0L, glRule2);
        auditActions = auditService.loadAudit(0, Integer.MAX_VALUE, new AuditFilter());

        assertEquals(1, auditActions.size());
        assertTrue(auditActions.stream()
            .allMatch(action -> action.getActionType().equals(AuditAction.ActionType.DELETE)));
        assertTrue(auditActions.stream()
            .allMatch(action -> action.getEntityType().equals(AuditAction.EntityType.DEPENDENCY_RULE)));
        assertTrue(auditActions.stream().allMatch(action -> action.getEntityName().equals("TEST2")));
    }

    private static GLRule createTestRule(String ruleName) {
        GLRule rule = new GLRuleImpl();
        rule.setName(ruleName);
        rule.setType(GLRuleType.MANUAL);
        rule.setIfs(Arrays.asList(
            new GLRulePredicate(1L, 2L, GLRulePredicate.NUMBER_MATCHES),
            new GLRulePredicate(3L, 4L, GLRulePredicate.ENUM_NOMATCHES)));
        rule.setThens(Arrays.asList(
            new GLRulePredicate(1L, 2L, GLRulePredicate.NUMBER_MATCHES),
            new GLRulePredicate(3L, 4L, GLRulePredicate.ENUM_NOMATCHES)));
        return rule;
    }

    private static GLRule createModifiedTestRule(String newRuleName) {
        GLRule glRule2 = createTestRule(newRuleName);
        ArrayList<GLRulePredicate> newIfs = new ArrayList<>(glRule2.getIfs());
        newIfs.add(new GLRulePredicate(5L, GLRulePredicate.VALUE_UNDEFINED));
        glRule2.setIfs(newIfs);
        return glRule2;
    }
}
