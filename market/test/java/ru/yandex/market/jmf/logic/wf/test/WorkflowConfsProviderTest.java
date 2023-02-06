package ru.yandex.market.jmf.logic.wf.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.logic.wf.DefaultWfConfigurationProvider;
import ru.yandex.market.jmf.logic.wf.WfConfigurationProvider;
import ru.yandex.market.jmf.logic.wf.conf.Config;
import ru.yandex.market.jmf.logic.wf.conf.StatusConf;
import ru.yandex.market.jmf.logic.wf.conf.TransitionConf;
import ru.yandex.market.jmf.logic.wf.conf.WorkflowBuilder;
import ru.yandex.market.jmf.logic.wf.conf.WorkflowConf;
import ru.yandex.market.jmf.logic.wf.conf.WorkflowConfsProvider;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.metainfo.LStringUtils;
import ru.yandex.market.jmf.metainfo.MetaInfoStorageService;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.XmlUtils;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SpringJUnitConfig(WorkflowConfsProviderTest.Configuration.class)
public class WorkflowConfsProviderTest {
    private final Fqn FQN_ROOT = Fqn.of("rootEntity");

    @Inject
    private WorkflowConfsProvider workflowConfsProvider;
    @Inject
    private MetaInfoStorageService metaInfoStorageService;
    @Inject
    private TxService txService;
    @Inject
    private XmlUtils xmlUtils;

    @Test
    void getWorkflowConfs_xmlProvider() {
        WorkflowConf actualWorkflow = txService.doInNewTx(() -> workflowConfsProvider.getWorkflowConfs().get(FQN_ROOT));
        WorkflowConf expectedWorkflow = getDefaultWorkflowConf();

        assertEqualsWorkflow(actualWorkflow, expectedWorkflow);
    }

    @Test
    void getWorkflowConfs_xmlAndDbProvider() {
        addAndEditStatuses();

        WorkflowConf actualWorkflow = txService.doInNewTx(() -> workflowConfsProvider.getWorkflowConfs().get(FQN_ROOT));
        WorkflowConf expectedWorkflow = getOverrideResultWorkflowConf();

        assertEqualsWorkflow(actualWorkflow, expectedWorkflow);
    }

    private void assertEqualsWorkflow(WorkflowConf actualWorkflow, WorkflowConf expectedWorkflow) {
        Assertions.assertNotNull(actualWorkflow);
        Assertions.assertEquals(expectedWorkflow.getStatuses().getInitial(), actualWorkflow.getStatuses().getInitial());

        for (StatusConf expectedStatus : expectedWorkflow.getStatuses().getStatus()) {
            StatusConf actualStatus = actualWorkflow.getStatuses().getStatus().stream()
                    .filter(s -> s.getCode().equals(expectedStatus.getCode()))
                    .findAny()
                    .orElse(null);

            Assertions.assertNotNull(actualStatus);
            Assertions.assertEquals(
                    LStringUtils.get(expectedStatus.getTitle(), LStringUtils.DEFAULT_LANG),
                    LStringUtils.get(actualStatus.getTitle(), LStringUtils.DEFAULT_LANG));
        }

        for (TransitionConf expectedTransition : expectedWorkflow.getTransitions().getTransition()) {
            TransitionConf actualTransition = actualWorkflow.getTransitions().getTransition().stream()
                    .filter(t -> t.getFrom().equals(expectedTransition.getFrom()))
                    .filter(t -> t.getTo().equals(expectedTransition.getTo()))
                    .findAny()
                    .orElse(null);

            Assertions.assertNotNull(actualTransition);
            Assertions.assertEquals(
                    LStringUtils.get(expectedTransition.getTitle(), LStringUtils.DEFAULT_LANG),
                    LStringUtils.get(actualTransition.getTitle(), LStringUtils.DEFAULT_LANG));
        }
    }

    private void addAndEditStatuses() {
        txService.runInNewTx(() -> {
            WorkflowConf workflow = getOverrideWorkflowConf();
            metaInfoStorageService.save(WorkflowConf.TYPE, FQN_ROOT.toString(), workflow, WorkflowBuilder.KEY);
        });
    }

    private WorkflowConf getDefaultWorkflowConf() {
        Config config = xmlUtils.readResource("WorkflowConfsProviderTest/workflow_default.xml", Config.class);
        return config.getWorkflow().stream()
                .filter(wf -> wf.getMetaclass().equals(FQN_ROOT))
                .findAny()
                .orElseThrow();
    }

    private WorkflowConf getOverrideWorkflowConf() {
        Config config = xmlUtils.readResource("WorkflowConfsProviderTest/workflow_override.xml", Config.class);
        return config.getWorkflow().stream()
                .filter(wf -> wf.getMetaclass().equals(FQN_ROOT))
                .findAny()
                .orElseThrow();
    }

    private WorkflowConf getOverrideResultWorkflowConf() {
        Config config = xmlUtils.readResource("WorkflowConfsProviderTest/workflow_override_result.xml", Config.class);
        return config.getWorkflow().stream()
                .filter(wf -> wf.getMetaclass().equals(FQN_ROOT))
                .findAny()
                .orElseThrow();
    }

    @Import(LogicWfTestConfiguration.class)
    @org.springframework.context.annotation.Configuration
    public static class Configuration {
        @Bean
        public MetadataProvider metaclass(MetadataProviders providers) {
            return providers.of("classpath:WorkflowConfsProviderTest/metaclass.xml");
        }

        @Bean
        public WfConfigurationProvider workflow(XmlUtils xmlUtils) {
            return new DefaultWfConfigurationProvider("classpath:WorkflowConfsProviderTest/workflow_default.xml",
                    xmlUtils);
        }
    }
}
