package ru.yandex.market.crm.triggers.test.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.MessageCorrelationResultType;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.services.bpm.AbstractBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.services.bpm.delegates.AbstractExecutionDelegate;
import ru.yandex.market.crm.triggers.test.ExecutionMock;
import ru.yandex.market.crm.triggers.test.helpers.builders.CrmProcessBuilder;

import static org.camunda.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.camunda.bpm.model.bpmn.impl.BpmnModelConstants.CAMUNDA_NS;
import static org.junit.Assert.assertEquals;

/**
 * @author apershukov
 */
@Component
public class TriggersHelper {

    private final TriggerService triggerService;
    private final RepositoryService repositoryService;
    private final ProcessEngine processEngine;

    public TriggersHelper(TriggerService triggerService,
                          RepositoryService repositoryService,
                          ProcessEngine processEngine) {
        this.triggerService = triggerService;
        this.repositoryService = repositoryService;
        this.processEngine = processEngine;
    }

    public static CrmProcessBuilder triggerBuilder() {
        return triggerBuilder("Test_" + UUID.randomUUID().toString());
    }

    /**
     * Создание builder'а триггерного процесса с предустановленными значениями
     * имени и флага executable
     * @param triggerKey идентификатор триггера (key в bpmn процессах)
     */
    public static CrmProcessBuilder triggerBuilder(String triggerKey) {
        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();
        Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace(BPMN20_NS);
        definitions.getDomElement().registerNamespace("camunda", CAMUNDA_NS);
        definitions.getDomElement().registerNamespace("custom", CustomAttributesHelper.LILU_CUSTOM_URI);
        modelInstance.setDefinitions(definitions);
        Process process = modelInstance.newInstance(Process.class);
        definitions.addChildElement(process);

        BpmnDiagram bpmnDiagram = modelInstance.newInstance(BpmnDiagram.class);

        BpmnPlane bpmnPlane = modelInstance.newInstance(BpmnPlane.class);
        bpmnPlane.setBpmnElement(process);

        bpmnDiagram.addChildElement(bpmnPlane);
        definitions.addChildElement(bpmnDiagram);

        return new CrmProcessBuilder(modelInstance, process)
                .id(triggerKey)
                .name("Test trigger")
                .executable();
    }

    public org.camunda.bpm.engine.runtime.ProcessInstance startProcessWithMessage(AbstractBpmMessage message) {
        List<MessageCorrelationResult> results = triggerService.sendBpmMessage(message);

        assertEquals(1, results.size());

        MessageCorrelationResult result = results.get(0);
        assertEquals(MessageCorrelationResultType.ProcessDefinition, result.getResultType());

        return result.getProcessInstance();
    }

    public DelegateExecution runTask(AbstractExecutionDelegate delegate,
                        String processDefinitionId,
                        String elementId,
                        ProcessInstance processInstance) throws Exception {
        BpmnModelInstance modelInstance = repositoryService.getBpmnModelInstance(processDefinitionId);

        DelegateExecution execution = ExecutionMock.create(
                processDefinitionId,
                processInstance,
                modelInstance.getModelElementById(elementId),
                processEngine
        );

        delegate.execute(execution);

        return execution;
    }

    public static class ProcessInstance {

        private final Map<String, Object> vars;

        public ProcessInstance(Uid uid) {
            this.vars = new HashMap<>();
            vars.put(ProcessVariablesNames.ID_VALUE, uid.getValue());
            vars.put(ProcessVariablesNames.ID_TYPE, uid.getType().name());
        }

        @Nonnull
        public Map<String, Object> getVars() {
            return vars;
        }

        public ProcessInstance setVariable(String name, Object value) {
            vars.put(name, value);
            return this;
        }
    }
}
