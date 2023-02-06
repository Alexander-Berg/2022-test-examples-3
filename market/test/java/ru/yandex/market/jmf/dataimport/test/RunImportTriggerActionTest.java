package ru.yandex.market.jmf.dataimport.test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.jmf.bcp.BcpConstants;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.dataimport.ProcessExecutor;
import ru.yandex.market.jmf.dataimport.impl.importconfiguration.FileAwareImport;
import ru.yandex.market.jmf.dataimport.impl.importconfiguration.Import;
import ru.yandex.market.jmf.dataimport.impl.importconfiguration.ImportConfiguration;
import ru.yandex.market.jmf.dataimport.impl.importconfiguration.ImportStatus;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.queue.retry.internal.FastRetryTasksQueue;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskProcessor;
import ru.yandex.market.jmf.trigger.impl.TriggerServiceImpl;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig(InternalDataImportTestConfiguration.class)
public class RunImportTriggerActionTest {

    @Inject
    DbService dbService;
    @Inject
    TxService txService;
    @Inject
    BcpService bcpService;
    @Inject
    MetadataService metadataService;
    @Inject
    TriggerServiceImpl triggerService;
    @Inject
    RetryTaskProcessor retryTaskProcessor;
    @Inject
    FastRetryTasksQueue queue;
    @Inject
    ConfigurationService configurationService;

    @BeforeEach
    public void cleanUp() {
        txService.runInNewTx(() -> {
            configurationService.setValue(ProcessExecutor.PROGRESS_UPDATE_PERIOD, 1);
            Map<String, Object> attributes = Map.of(BcpConstants.Attributes.ATTRIBUTE_SKIP_DEPENDENCY, true);
            dbService.list(Query.of(Import.FQN)).forEach(x -> bcpService.delete(x, attributes));
            dbService.list(Query.of(Fqn.of("ticket"))).forEach(x -> bcpService.delete(x, attributes));
        });
    }

    @Test
    public void testImportTickets() throws InterruptedException {
        String stringConfig = CrmStrings.valueOf(ResourceHelpers.getResource(
                "/runImportTriggerActionTest/ticket.error.import.xml"))
                .formatted("ticket.success.csv");
        runImport(stringConfig, "ticket");

        // Проверка утверждений
        txService.runInNewTx(() -> {
            assertImportStatus(ImportStatus.COMPLETED);
            assertTicketsCount(3);
        });
    }

    @Test
    public void testErrorBeforeImport() throws InterruptedException {
        String stringConfig = CrmStrings.valueOf(ResourceHelpers.getResource(
                "/delegateTestFiles/poor_delegate.import.xml"));
        runImport(stringConfig, "ou");

        // Проверка утверждений
        txService.runInNewTx(() -> {
            assertImportStatus(ImportStatus.FAILED);
            assertTicketsCount(0);
        });
    }

    @Test
    public void testErrorOnCreateTicket() throws InterruptedException {
        String stringConfig = CrmStrings.valueOf(ResourceHelpers.getResource(
                "/runImportTriggerActionTest/ticket.error.import.xml")).formatted("ticket.errorOnCreateTicket.csv");
        runImport(stringConfig, "ticket");

        // Проверка утверждений
        txService.runInNewTx(() -> {
            assertImportStatus(ImportStatus.COMPLETED_WITH_FAILURES);
            assertTicketsCount(2);
        });
    }

    @Test
    public void testErrorOnEditProgress() throws InterruptedException {
        String stringConfig = CrmStrings.valueOf(ResourceHelpers.getResource(
                "/runImportTriggerActionTest/ticket.error.import.xml")).formatted("ticket.errorOnEditProgress.csv");
        runImport(stringConfig, "ticket");

        // Проверка утверждений
        txService.runInNewTx(() -> {
            assertImportStatus(ImportStatus.FAILED);
            assertTicketsCount(4);
        });
    }

    private void runImport(String stringConfig, String tableClass) throws InterruptedException {
        String importConfigurationCode = "x%s".formatted(Randoms.positiveIntValue());

        ImportConfiguration importConfiguration = txService.doInNewTx(() ->
                bcpService.create(ImportConfiguration.FQN, Map.of(
                        ImportConfiguration.TITLE, Randoms.string(),
                        ImportConfiguration.CODE, importConfigurationCode,
                        ImportConfiguration.IMPORT_CONF, stringConfig,
                        ImportConfiguration.TABLE_CLASS, tableClass
                ))
        );
        assertNotNull(metadataService.getMetaclass(Import.FQN.ofType(importConfigurationCode)));

        triggerService.withAsyncTriggersMode(() -> {
            Import testImport = txService.doInNewTx(() -> {
                Attachment source = bcpService.create(Attachment.FQN_DEFAULT, Maps.of(
                        Attachment.NAME, Randoms.string(),
                        Attachment.CONTENT_TYPE, "text/csv",
                        Attachment.URL, Randoms.url()
                ));
                return bcpService.create(Import.FQN.ofType(importConfigurationCode), Map.of(
                        Import.CONFIGURATION, importConfiguration,
                        FileAwareImport.SOURCES, Set.of(source)
                ));
            });
            assertNotNull(testImport);
        });

        Thread.sleep(1100);
        txService.doInNewTx(() -> retryTaskProcessor.processPendingTasksWithReset(queue));
    }

    private void assertImportStatus(String status) {
        List<Import> imports = dbService.list(Query.of(Import.FQN));
        Assertions.assertEquals(1, imports.size());
        Assertions.assertEquals(status, imports.get(0).getStatus().getCode());
    }

    private void assertTicketsCount(int count) {
        List<Entity> tickets = dbService.list(Query.of(Fqn.parse("ticket")));
        Assertions.assertEquals(count, tickets.size());
    }
}
