package ru.yandex.market.mbi.bpmn.process;

import java.util.List;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bpmn.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessDefinitionTest extends FunctionalTest {

    @Autowired
    private RepositoryService repositoryService;

    @Test
    void checkAllProcessDefinitionsHasName() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .list();
        for (ProcessDefinition processDefinition : processDefinitions) {
            assertNotNull(processDefinition.getName(),
                    () -> "Process " + processDefinition.getKey() + " should have name");
        }

    }

    @Test
    void checkAllProcessDefinitionsHasTtl() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .list();
        for (ProcessDefinition processDefinition : processDefinitions) {
            assertTrue(processDefinition.getHistoryTimeToLive() != null
                            && processDefinition.getHistoryTimeToLive() > 0
                            && processDefinition.getHistoryTimeToLive() < 15,
                    () -> "Process " + processDefinition.getKey() + " should have TTL between 1 and 14");
        }

    }
}
