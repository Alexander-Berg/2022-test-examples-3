package ru.yandex.market.jmf.module.automation.test.utils;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.module.automation.ActionAutomationRule;
import ru.yandex.market.jmf.module.automation.AutomationRule;
import ru.yandex.market.jmf.module.automation.EventAutomationRule;
import ru.yandex.market.jmf.module.automation.ScheduleAutomationRule;
import ru.yandex.market.jmf.module.entity.snapshot.SnapshottedByStatusLogic;
import ru.yandex.market.jmf.module.ou.Employee;
import ru.yandex.market.jmf.module.ou.Ou;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;

@Component
public class AutomationRuleTestUtils {

    private static final String EDIT_TICKET_AUTOMATION_RULE_GROUP = "editTicket";

    @Inject
    protected BcpService bcpService;
    @Inject
    protected DbService dbService;

    @Inject
    protected ObjectSerializeService objectSerializeService;

    public EventAutomationRule createDraftEventRule(Entity entity,
                                                    String configPath,
                                                    String ruleGroup,
                                                    Set<Employee> responsibleEmployees,
                                                    Set<Ou> responsibleOus,
                                                    String... args) {
        return bcpService.create(EventAutomationRule.FQN, Maps.of(
                EventAutomationRule.TITLE, Randoms.string(),
                EventAutomationRule.CONFIG, getJsonNodeByPath(configPath, args),
                EventAutomationRule.RULE_GROUP, ruleGroup,
                EventAutomationRule.ENTITY, entity,
                EventAutomationRule.RESPONSIBLE_EMPLOYEES, responsibleEmployees,
                EventAutomationRule.RESPONSIBLE_OUS, responsibleOus
        ));
    }

    public EventAutomationRule createApprovedEventRule(Entity entity,
                                                       String configPath,
                                                       String ruleGroup,
                                                       Set<Employee> responsibleEmployees,
                                                       Set<Ou> responsibleOus,
                                                       String... args) {
        EventAutomationRule rule = createDraftEventRule(entity, configPath, ruleGroup, responsibleEmployees,
                responsibleOus, args);
        bcpService.edit(rule, Map.of(AutomationRule.STATUS, SnapshottedByStatusLogic.Statuses.REVIEW));
        bcpService.edit(rule, Map.of(AutomationRule.STATUS, SnapshottedByStatusLogic.Statuses.APPROVED));
        return rule;
    }

    private JsonNode getJsonNodeByPath(String path, String... args) {
        String stringConfig = String.format(CrmStrings.valueOf(ResourceHelpers.getResource(path)), args);
        return objectSerializeService.deserialize(
                CrmStrings.getBytes(stringConfig),
                JsonNode.class
        );
    }

    public void createEventRuleForEditTicketGroup(Entity entity,
                                                  String configPath,
                                                  Set<Employee> responsibleEmployees,
                                                  Set<Ou> responsibleOus) {
        createApprovedEventRule(entity, configPath, EDIT_TICKET_AUTOMATION_RULE_GROUP, responsibleEmployees,
                responsibleOus);
    }

    public ActionAutomationRule editActionRule(ActionAutomationRule automationRule,
                                               String configPath,
                                               String... args) {
        return editActionRule(automationRule, getJsonNodeByPath(configPath, args));
    }

    public EventAutomationRule editEventRule(EventAutomationRule automationRule, String configPath, String... args) {
        return editEventRule(automationRule, getJsonNodeByPath(configPath, args));
    }

    public EventAutomationRule editEventRule(EventAutomationRule automationRule, JsonNode config) {
        return bcpService.edit(automationRule, Maps.of(
                EventAutomationRule.TITLE, Randoms.string(),
                EventAutomationRule.CONFIG, config
        ));
    }

    public ActionAutomationRule editActionRule(ActionAutomationRule automationRule, JsonNode config) {
        return bcpService.edit(automationRule, Maps.of(
                ActionAutomationRule.TITLE, Randoms.string(),
                ActionAutomationRule.CONFIG, config
        ));
    }

    public ActionAutomationRule editActionRuleCondition(ActionAutomationRule automationRule,
                                                        String configPath,
                                                        String... args) {
        return bcpService.edit(automationRule, Maps.of(
                ActionAutomationRule.AVAILABILITY_CONDITION, getJsonNodeByPath(configPath, args)
        ));
    }

    public void deleteAll() {
        dbService.list(Query.of(AutomationRule.FQN).withFilters(Filters.eq("status", "active")))
                .forEach(e -> bcpService.edit(e, Map.of("status", "archived")));
    }

    public ActionAutomationRule createDraftActionRule(JsonNode config,
                                                      String actionTitle,
                                                      Metaclass availableOnMetaclass,
                                                      JsonNode conditionConfig,
                                                      Set<Employee> responsibleEmployees,
                                                      Set<Ou> responsibleOus) {
        return bcpService.create(ActionAutomationRule.FQN, Maps.of(
                ActionAutomationRule.TITLE, Randoms.string(),
                ActionAutomationRule.CONFIG, config,
                ActionAutomationRule.ACTION_TITLE, actionTitle,
                ActionAutomationRule.AVAILABLE_ON_METACLASS, availableOnMetaclass,
                ActionAutomationRule.AVAILABILITY_CONDITION, conditionConfig,
                ActionAutomationRule.RESPONSIBLE_EMPLOYEES, responsibleEmployees,
                ActionAutomationRule.RESPONSIBLE_OUS, responsibleOus
        ));
    }

    public ActionAutomationRule createDraftActionRule(String configPath,
                                                      String actionTitle,
                                                      Metaclass availableOnMetaclass,
                                                      Set<Employee> responsibleEmployees,
                                                      Set<Ou> responsibleOus,
                                                      String... args) {
        String stringConfig = String.format(CrmStrings.valueOf(ResourceHelpers.getResource(configPath)), args);
        JsonNode config = objectSerializeService.deserialize(
                CrmStrings.getBytes(stringConfig),
                JsonNode.class
        );
        String stringConditionConfig =
                String.format(CrmStrings.valueOf(ResourceHelpers.getResource(configPath.replaceAll("(\\.[^.]+)$",
                        ".condition$1"))), args);
        JsonNode conditionConfig = objectSerializeService.deserialize(
                CrmStrings.getBytes(stringConditionConfig),
                JsonNode.class
        );
        return createDraftActionRule(config, actionTitle, availableOnMetaclass, conditionConfig, responsibleEmployees
                , responsibleOus);
    }

    public ActionAutomationRule createApprovedActionRule(String configPath,
                                                         String actionTitle,
                                                         Metaclass availableOnMetaclass,
                                                         Set<Employee> responsibleEmployees,
                                                         Set<Ou> responsibleOus,
                                                         String... args) {
        ActionAutomationRule rule = createDraftActionRule(configPath, actionTitle, availableOnMetaclass,
                responsibleEmployees, responsibleOus, args);
        bcpService.edit(rule, Map.of(AutomationRule.STATUS, SnapshottedByStatusLogic.Statuses.REVIEW));
        bcpService.edit(rule, Map.of(AutomationRule.STATUS, SnapshottedByStatusLogic.Statuses.APPROVED));
        return rule;
    }

    public ScheduleAutomationRule createDraftScheduleRule(String ruleConfigPath,
                                                          Fqn objectsMetaclass,
                                                          String filtersConfigPath,
                                                          Set<Employee> responsibleEmployees,
                                                          Set<Ou> responsibleOus,
                                                          String... args) {
        return bcpService.create(ScheduleAutomationRule.FQN, Maps.of(
                ScheduleAutomationRule.TITLE, Randoms.string(),
                ScheduleAutomationRule.CONFIG, getJsonNodeByPath(ruleConfigPath),
                ScheduleAutomationRule.OBJECTS_METACLASS, objectsMetaclass,
                ScheduleAutomationRule.FILTERS, getJsonNodeByPath(filtersConfigPath, args),
                ScheduleAutomationRule.RESPONSIBLE_EMPLOYEES, responsibleEmployees,
                ScheduleAutomationRule.RESPONSIBLE_OUS, responsibleOus
        ));
    }

    public ScheduleAutomationRule editScheduleRuleFilters(ScheduleAutomationRule automationRule,
                                                          String filtersConfigPath,
                                                          String... args) {
        return bcpService.edit(automationRule, Maps.of(
                ScheduleAutomationRule.FILTERS, getJsonNodeByPath(filtersConfigPath, args)
        ));
    }
}
