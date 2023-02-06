package ru.yandex.market.jmf.module.def.test;

import java.time.OffsetDateTime;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metainfo.LStringUtils;
import ru.yandex.market.jmf.metainfo.MetaInfoService;
import ru.yandex.market.jmf.metainfo.MetaInfoStorageService;
import ru.yandex.market.jmf.queue.retry.internal.FastRetryTasksQueue;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskProcessor;
import ru.yandex.market.jmf.script.Script;
import ru.yandex.market.jmf.script.storage.ScriptsIndex;
import ru.yandex.market.jmf.script.storage.conf.ScriptConf;
import ru.yandex.market.jmf.script.storage.impl.ScriptsMetaInfoInitializer;
import ru.yandex.market.jmf.security.action.SecurityDomainConfigurationsProvider;
import ru.yandex.market.jmf.security.conf.AccessMatrixConf;
import ru.yandex.market.jmf.security.conf.ActionPermissionConf;
import ru.yandex.market.jmf.security.conf.ProfileActionsConf;
import ru.yandex.market.jmf.security.impl.action.domain.SecurityDomainsInitializer;
import ru.yandex.market.jmf.startrek.support.StartrekService;
import ru.yandex.market.jmf.trigger.conf.TriggerConf;
import ru.yandex.market.jmf.trigger.impl.TriggerData;
import ru.yandex.market.jmf.trigger.impl.TriggerMetadataInitializer;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.startrek.client.model.Issue;

import static ru.yandex.market.jmf.module.def.impl.sox.MetaInfoChangeSoxStartrekIssueListener.ENABLE;
import static ru.yandex.market.jmf.module.def.impl.sox.SoxStartrekIssueTaskHandler.SOX_STARTREK_QUEUE;

@SpringJUnitConfig(SoxStartrekIssueConfiguration.class)
public class MetaInfoChangeSoxStartrekIssueListenerTest {
    private static final String SIMPLE_SCRIPT = "simpleScript";
    private static final String SIMPLE_TRIGGER = "simpleTrigger";
    private static final Fqn SIMPLE_ENTITY_FQN = Fqn.of("simpleEntity");
    private static final String STARTREK_QUEUE = "TESTQUEUE";

    @Inject
    private MetadataService metadataService;
    @Inject
    private MetaInfoService metaInfoService;
    @Inject
    private MetaInfoStorageService metaInfoStorageService;
    @Inject
    private SecurityDomainConfigurationsProvider securityDomainConfigurationsProvider;
    @Inject
    private ConfigurationService configurationService;
    @Inject
    private FastRetryTasksQueue retryTasksQueue;
    @Inject
    private RetryTaskProcessor retryTaskProcessor;
    @Inject
    private TxService txService;
    @Inject
    private StartrekService startrekServiceMock;

    private Boolean oldSoxStartrekEnable;
    private String oldSoxStartrekQueue;

    @BeforeEach
    @Transactional
    void setUp() {
        prepareStartrekServiceMock();
        prepareConfigurationService();
    }

    private void prepareStartrekServiceMock() {
        Issue issueMock = new Issue(null, null, STARTREK_QUEUE + "-1", null, 1, new EmptyMap<>(), null);
        Mockito.when(startrekServiceMock.createIssue(
                        Mockito.anyString(), Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(issueMock);
    }

    private void prepareConfigurationService() {
        this.oldSoxStartrekEnable = configurationService.getValue(ENABLE);
        this.oldSoxStartrekQueue = configurationService.getValue(SOX_STARTREK_QUEUE);

        configurationService.setValues(Map.of(
                ENABLE, true,
                SOX_STARTREK_QUEUE, STARTREK_QUEUE
        ));
    }

    @AfterEach
    @Transactional
    void tearDown() {
        retryTasksQueue.reset();
        Mockito.reset(startrekServiceMock);

        configurationService.setValues(Maps.of(
                ENABLE, oldSoxStartrekEnable,
                SOX_STARTREK_QUEUE, oldSoxStartrekQueue
        ));
    }

    @Test
    public void testDisable() {
        txService.runInTx(() -> configurationService.setValue(ENABLE, false));

        Assertions.assertTrue(metaInfoService.get(ScriptsIndex.class).get(SIMPLE_SCRIPT).isPresent());
        Script existScript = metaInfoService.get(ScriptsIndex.class).getOrError(SIMPLE_SCRIPT);

        var newScriptConf = new ScriptConf();
        newScriptConf.setCode(SIMPLE_SCRIPT);
        newScriptConf.setBody("LOG.info('change')");
        newScriptConf.setTitle(LStringUtils.of(existScript.getTitle()));
        newScriptConf.setType(existScript.getType());
        newScriptConf.setVersion(OffsetDateTime.now());

        txService.runInTx(() -> {
            metaInfoStorageService.save(ScriptConf.TYPE, SIMPLE_SCRIPT, newScriptConf, ScriptsMetaInfoInitializer.KEY);

            retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue);
        });
        Mockito.verify(startrekServiceMock, Mockito.never())
                .createIssue(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testScript() {
        Assertions.assertTrue(metaInfoService.get(ScriptsIndex.class).get(SIMPLE_SCRIPT).isPresent());
        Script existScript = metaInfoService.get(ScriptsIndex.class).getOrError(SIMPLE_SCRIPT);

        var newScriptConf = new ScriptConf();
        newScriptConf.setCode(SIMPLE_SCRIPT);
        newScriptConf.setBody("LOG.info('change')");
        newScriptConf.setTitle(LStringUtils.of(existScript.getTitle()));
        newScriptConf.setType(existScript.getType());
        newScriptConf.setVersion(OffsetDateTime.now());

        txService.runInTx(() -> {
            metaInfoStorageService.save(ScriptConf.TYPE, SIMPLE_SCRIPT, newScriptConf, ScriptsMetaInfoInitializer.KEY);

            retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue);
        });
        Mockito.verify(startrekServiceMock, Mockito.atLeastOnce())
                .createIssue(Mockito.eq(STARTREK_QUEUE), Mockito.any(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testAccessMatrix() {
        var metaclass = metadataService.getMetaclass(SIMPLE_ENTITY_FQN);
        Assertions.assertNotNull(metaclass);

        var accessMatrixConf = securityDomainConfigurationsProvider.getAccessMatrixConfs()
                .computeIfAbsent(SIMPLE_ENTITY_FQN, k -> new AccessMatrixConf());
        accessMatrixConf.setMetaclass(SIMPLE_ENTITY_FQN);

        ProfileActionsConf profileActionsConf = new ProfileActionsConf();
        profileActionsConf.setProfileId("test");
        accessMatrixConf.getProfileActions().add(profileActionsConf);

        ActionPermissionConf actionPermissionConf = new ActionPermissionConf();
        actionPermissionConf.setActionId("@create");
        actionPermissionConf.setValue(true);
        actionPermissionConf.setDecisionScriptCode(SIMPLE_SCRIPT);
        profileActionsConf.getAction().add(actionPermissionConf);

        txService.runInTx(() -> {
            metaInfoStorageService.save(
                    AccessMatrixConf.TYPE, SIMPLE_ENTITY_FQN.toString(), accessMatrixConf,
                    SecurityDomainsInitializer.KEY);

            retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue);
        });

        Mockito.verify(startrekServiceMock, Mockito.atLeastOnce())
                .createIssue(Mockito.eq(STARTREK_QUEUE), Mockito.any(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testTrigger() {
        TriggerData triggerData = metaInfoService.get(TriggerData.class);
        TriggerConf triggerConf = new TriggerConf();
        triggerData.getTriggerOrError(SIMPLE_TRIGGER).copyTo(triggerConf);

        triggerConf.setEnabled(false);
        triggerConf.setTitle(LStringUtils.of("Change Title"));

        txService.runInTx(() -> {
            metaInfoStorageService.save(TriggerConf.TYPE, SIMPLE_TRIGGER, triggerConf, TriggerMetadataInitializer.KEY);

            retryTaskProcessor.processPendingTasksWithReset(retryTasksQueue);
        });

        Mockito.verify(startrekServiceMock, Mockito.atLeastOnce())
                .createIssue(Mockito.eq(STARTREK_QUEUE), Mockito.any(), Mockito.anyString(), Mockito.anyString());
    }
}
