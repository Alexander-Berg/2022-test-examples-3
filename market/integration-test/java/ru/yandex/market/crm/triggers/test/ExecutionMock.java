package ru.yandex.market.crm.triggers.test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineServices;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.runtime.Incident;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;

import ru.yandex.market.crm.triggers.test.helpers.TriggersHelper.ProcessInstance;

/**
 * @author apershukov
 */
public class ExecutionMock implements DelegateExecution {

    private final String procDefinitionId;
    private final FlowElement element;
    private final Map<String, Object> vars;
    private final ProcessEngine processEngine;
    private ExecutionMock(String procDefinitionId,
                          FlowElement element,
                          Map<String, Object> initialVars,
                          ProcessEngine processEngine) {
        this.procDefinitionId = procDefinitionId;
        this.element = element;
        this.vars = Maps.newHashMap(initialVars);
        this.processEngine = processEngine;
    }

    public static ExecutionMock create(String procDefId,
                                       ProcessInstance processInstance,
                                       FlowElement element,
                                       ProcessEngine processEngine) {
        return new ExecutionMock(procDefId, element, processInstance.getVars(), processEngine);
    }

    @Override
    public String getProcessInstanceId() {
        return null;
    }

    @Override
    public String getProcessBusinessKey() {
        return null;
    }

    @Override
    public void setProcessBusinessKey(String businessKey) {

    }

    @Override
    public String getProcessDefinitionId() {
        return procDefinitionId;
    }

    @Override
    public String getParentId() {
        return null;
    }

    @Override
    public String getCurrentActivityId() {
        return element.getId();
    }

    @Override
    public String getCurrentActivityName() {
        return null;
    }

    @Override
    public String getActivityInstanceId() {
        return null;
    }

    @Override
    public String getParentActivityInstanceId() {
        return null;
    }

    @Override
    public String getCurrentTransitionId() {
        return null;
    }

    @Override
    public DelegateExecution getProcessInstance() {
        return null;
    }

    @Override
    public DelegateExecution getSuperExecution() {
        return null;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public String getTenantId() {
        return null;
    }

    @Override
    public void setVariable(String variableName, Object value, String activityId) {

    }

    @Override
    public Incident createIncident(String incidentType, String configuration) {
        return null;
    }

    @Override
    public Incident createIncident(String incidentType, String configuration, String message) {
        return null;
    }

    @Override
    public void resolveIncident(String incidentId) {

    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getEventName() {
        return null;
    }

    @Override
    public String getBusinessKey() {
        return null;
    }

    @Override
    public BpmnModelInstance getBpmnModelInstance() {
        return null;
    }

    @Override
    public FlowElement getBpmnModelElementInstance() {
        return element;
    }

    @Override
    public ProcessEngineServices getProcessEngineServices() {
        return processEngine;
    }

    @Override
    public ProcessEngine getProcessEngine() {
        return processEngine;
    }

    @Override
    public String getVariableScopeKey() {
        return null;
    }

    @Override
    public Map<String, Object> getVariables() {
        return vars;
    }

    @Override
    public void setVariables(Map<String, ?> variables) {
        vars.putAll(variables);
    }

    @Override
    public VariableMap getVariablesTyped() {
        return null;
    }

    @Override
    public VariableMap getVariablesTyped(boolean deserializeValues) {
        return null;
    }

    @Override
    public Map<String, Object> getVariablesLocal() {
        return null;
    }

    @Override
    public void setVariablesLocal(Map<String, ?> variables) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableMap getVariablesLocalTyped() {
        return null;
    }

    @Override
    public VariableMap getVariablesLocalTyped(boolean deserializeValues) {
        return null;
    }

    @Override
    public Object getVariable(String variableName) {
        return vars.get(variableName);
    }

    @Override
    public Object getVariableLocal(String variableName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends TypedValue> T getVariableTyped(String variableName) {
        return null;
    }

    @Override
    public <T extends TypedValue> T getVariableTyped(String variableName, boolean deserializeValue) {
        return null;
    }

    @Override
    public <T extends TypedValue> T getVariableLocalTyped(String variableName) {
        return null;
    }

    @Override
    public <T extends TypedValue> T getVariableLocalTyped(String variableName, boolean deserializeValue) {
        return null;
    }

    @Override
    public Set<String> getVariableNames() {
        return null;
    }

    @Override
    public Set<String> getVariableNamesLocal() {
        return null;
    }

    @Override
    public void setVariable(String variableName, Object value) {
        vars.put(variableName, value);
    }

    @Override
    public void setVariableLocal(String variableName, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVariables() {
        return !vars.isEmpty();
    }

    @Override
    public boolean hasVariablesLocal() {
        return false;
    }

    @Override
    public boolean hasVariable(String variableName) {
        return false;
    }

    @Override
    public boolean hasVariableLocal(String variableName) {
        return false;
    }

    @Override
    public void removeVariable(String variableName) {
        vars.remove(variableName);
    }

    @Override
    public void removeVariableLocal(String variableName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVariables(Collection<String> variableNames) {
        variableNames.forEach(vars::remove);
    }

    @Override
    public void removeVariablesLocal(Collection<String> variableNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVariables() {
        vars.clear();
    }

    @Override
    public void removeVariablesLocal() {
        throw new UnsupportedOperationException();
    }
}
