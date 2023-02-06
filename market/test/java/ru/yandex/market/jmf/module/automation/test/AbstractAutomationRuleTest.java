package ru.yandex.market.jmf.module.automation.test;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.GidService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.module.automation.ActionAutomationRule;
import ru.yandex.market.jmf.module.automation.AutomationRuleGroup;
import ru.yandex.market.jmf.module.automation.EventAutomationRule;
import ru.yandex.market.jmf.module.automation.test.utils.AutomationRuleTestUtils;
import ru.yandex.market.jmf.module.metric.test.impl.InMemoryMetricsTskvService;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.module.ou.impl.OuTestUtils;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;

@Transactional
@SpringJUnitConfig(InternalModuleAutomationRuleTestConfiguration.class)
public abstract class AbstractAutomationRuleTest {

    protected static final Fqn FQN_1 = Fqn.of("simple1");
    protected static final Fqn FQN_2 = Fqn.of("simple2");

    protected static final String LINKED_ATTR = "linked";
    protected static final String TRIGGER_CONDITION_ATTR = "triggerCondition";
    protected static final String RULE_RESULT_ATTR = "ruleResult";
    protected static final String RULE_RESULT_ATTR_2 = "ruleResult2";
    protected static final String RULE_CONDITION_ATTR_1 = "ruleCondition1";
    protected static final String RULE_CONDITION_ATTR_2 = "ruleCondition2";
    protected static final String RULE_CONDITION_ATTR_3 = "ruleCondition3";
    protected static final String INT_ATTR = "intAttr";
    protected static final String OBJECT_ATTR = "objectAttr";
    protected static final String OBJECTS_ATTR = "objectsAttr";

    protected static final String SUCCESS = "success";
    protected static final String TEST_GROUP = "testGroup";

    @Inject
    protected BcpService bcpService;

    @Inject
    protected GidService gidService;

    @Inject
    protected DbService dbService;

    @Inject
    protected AutomationRuleTestUtils automationRuleTestUtils;

    @Inject
    protected InMemoryMetricsTskvService testMetricsService;

    @Inject
    protected OuTestUtils ouTestUtils;

    @Inject
    protected MockSecurityDataService securityDataService;

    @Inject
    private MetadataService metadataService;

    @BeforeEach
    public void setUp() {
        ouTestUtils.createOu();

        bcpService.create(AutomationRuleGroup.FQN, Map.of(
                AutomationRuleGroup.TITLE, Randoms.string(),
                AutomationRuleGroup.CODE, TEST_GROUP
        ));
    }

    @AfterEach
    public void tearDown() {
        securityDataService.reset();
        testMetricsService.clear();
    }

    protected EventAutomationRule createApprovedEventRule(Entity entity, String configPath, String... args) {
        return automationRuleTestUtils.createApprovedEventRule(entity, configPath, TEST_GROUP, Set.of(),
                Set.of(ouTestUtils.getAnyCreatedOu()), args);
    }

    protected EventAutomationRule createDraftEventRule(Entity entity, String configPath, String... args) {
        return automationRuleTestUtils.createDraftEventRule(entity, configPath, TEST_GROUP, Set.of(),
                Set.of(ouTestUtils.getAnyCreatedOu()), args);
    }

    protected ActionAutomationRule createDraftActionRule(String actionTitle, Fqn availableOnMetaclass,
                                                         String configPath,
                                                         String... args) {
        return automationRuleTestUtils.createDraftActionRule(configPath, actionTitle,
                metadataService.getMetaclassOrError(availableOnMetaclass), Set.of(),
                Set.of(ouTestUtils.getAnyCreatedOu()), args);
    }

    protected ActionAutomationRule createApprovedActionRule(String actionTitle, Fqn availableOnMetaclass,
                                                            String configPath,
                                                            String... args) {
        return automationRuleTestUtils.createApprovedActionRule(configPath, actionTitle,
                metadataService.getMetaclassOrError(availableOnMetaclass), Set.of(),
                Set.of(ouTestUtils.getAnyCreatedOu()), args);
    }

    protected ActionAutomationRule createApprovedActionRule(String actionTitle,
                                                            Fqn availableOnMetaclass,
                                                            String configPath,
                                                            Set<Employee> responsibleEmployees,
                                                            Set<Ou> responsibleOus,
                                                            String... args) {
        return automationRuleTestUtils.createApprovedActionRule(configPath, actionTitle,
                metadataService.getMetaclassOrError(availableOnMetaclass), responsibleEmployees,
                responsibleOus, args);
    }

    protected ActionAutomationRule editActionRule(ActionAutomationRule automationRule,
                                                  String configPath,
                                                  String... args) {
        return automationRuleTestUtils.editActionRule(automationRule, configPath, args);
    }

    protected ActionAutomationRule editActionRuleCondition(ActionAutomationRule automationRule,
                                                           String configPath,
                                                           String... args) {
        return automationRuleTestUtils.editActionRuleCondition(automationRule, configPath, args);
    }

    protected EventAutomationRule editEventRule(EventAutomationRule automationRule, String configPath, String... args) {
        return automationRuleTestUtils.editEventRule(automationRule, configPath, args);
    }

    protected void startTrigger(Entity entity1, Entity entity2) {
        bcpService.edit(entity1, Map.of(
                LINKED_ATTR, entity2,
                TRIGGER_CONDITION_ATTR, "START"
        ));
    }

    protected void restartTrigger(Entity entity1, Entity entity2) {
        bcpService.edit(entity1, Map.of(TRIGGER_CONDITION_ATTR, Randoms.string()));
        bcpService.edit(entity1, Map.of(
                LINKED_ATTR, entity2,
                TRIGGER_CONDITION_ATTR, "START"
        ));
    }

    protected void checkErrors(int count) {
        Assertions.assertEquals(
                count,
                testMetricsService
                        .getLoggedItemsByCode("PROCESSING_AUTOMATION_RULE_.*_.*_ERROR_COUNT")
                        .size()
        );
    }
}
