package ru.yandex.market.jmf.module.automation.test;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.Entity;

public class FindEntityAutomationRuleActionTest extends AbstractAutomationRuleTest {
    @Test
    public void findEntity_found() {
        String ruleResultValue = Randoms.string();

        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());
        bcpService.create(FQN_2, Map.of(
                "ruleCondition3", "test",
                "ruleResult", ruleResultValue
        ));

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/findEntitySingle.json");
        startTrigger(entity1, entity2);

        Entity newEntity1 = dbService.get(entity1.getGid());
        Assertions.assertEquals(ruleResultValue, newEntity1.getAttribute("dataKey"));
    }

    @Test
    public void findEntity_notFound() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());
        bcpService.create(FQN_2, Map.of(
                "ruleCondition3", "testWrong",
                "ruleResult", Randoms.string()
        ));

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/findEntitySingleNull.json");
        startTrigger(entity1, entity2);

        Entity newEntity1 = dbService.get(entity1.getGid());
        Assertions.assertEquals("null", newEntity1.getAttribute("dataKey"));
    }

    @Test
    public void findEntity_foundList() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        Entity entity21 = bcpService.create(FQN_2, Map.of(
                "ruleCondition3", "test",
                "ruleResult", Randoms.string()
        ));
        Entity entity22 = bcpService.create(FQN_2, Map.of(
                "ruleCondition3", "test",
                "ruleResult", Randoms.string()
        ));

        createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/findEntityList.json");
        startTrigger(entity1, entity2);

        Entity newEntity1 = dbService.get(entity1.getGid());
        Object ruleResult21 = entity21.getAttribute("ruleResult");
        Object ruleResult22 = entity22.getAttribute("ruleResult");
        Assertions.assertEquals("[%s, %s]".formatted(ruleResult21, ruleResult22), newEntity1.getAttribute("dataKey"));
    }
}
