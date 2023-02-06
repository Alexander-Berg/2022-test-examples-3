package ru.yandex.market.crm.campaign.services.importing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.campaign.domain.export.EntitiesImport;
import ru.yandex.market.crm.campaign.domain.export.EntityImportStatus;
import ru.yandex.market.crm.campaign.domain.export.ExportedEntityType;
import ru.yandex.market.crm.campaign.domain.export.ImportConflictResolveStrategy;
import ru.yandex.market.crm.campaign.domain.export.ImportingEntity;
import ru.yandex.market.crm.campaign.dto.export.ImportingEntityDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedMessageTemplateDto;
import ru.yandex.market.crm.campaign.services.export.EntitiesExportBaseVersionsDAO;
import ru.yandex.market.crm.campaign.services.trigger.TriggerService;
import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.mcrm.tx.TxService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.getMessageTemplateExportDto;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.getTriggerExportDto;

@ExtendWith(MockitoExtension.class)
class TriggersImporterTest {

    private static final String ENTITY_KEY = "block_key_1";
    private static final String OLD_TEMPLATE_ID = "b25a69f8-e9f0-4ca4-9c4d-74cd89f28196:1";
    private static final String OLD_TEMPLATE_KEY = "b25a69f8-e9f0-4ca4-9c4d-74cd89f28196";
    private static final Integer OLD_TEMPLATE_VERSION = 1;
    private static final String NEW_TEMPLATE_ID = "b25a69f8-e9f0-4ca4-9c4d-74cd89f28196:7";
    private static final Integer IMPORTING_ENTITY_VERSION = 6;

    private final ArgumentCaptor<ImportingEntity> entityArgumentCaptor = ArgumentCaptor.forClass(ImportingEntity.class);
    private final ArgumentCaptor<BpmnModelInstance> bpmnModelCaptor = ArgumentCaptor.forClass(BpmnModelInstance.class);

    private TriggersImporter importer;

    @Mock
    private TxService txService;
    @Mock
    public TriggerService triggerService;
    @Mock
    public EntitiesExportBaseVersionsDAO baseVersionsDAO;
    @Mock
    public EntitiesImportsDAO importsDAO;

    @BeforeEach
    void setUp() {
        importer = spy(new TriggersImporter(triggerService, baseVersionsDAO, txService, importsDAO));
    }

    @Test
    void importTriggerWithConflictWithoutForceShouldNotBeImported() throws IOException {
        var template = trigger(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.NONE);

        var anImport = new EntitiesImport();
        anImport.setEntities(Collections.emptyList());

        importer.prepareTrigger(template, anImport, null);

        Mockito.verify(importsDAO, times(0)).updateEntity(any());
        Mockito.verify(triggerService, times(0)).updateTrigger(any(), any());
        Mockito.verify(triggerService, times(0)).addTrigger(any(BpmnModelInstance.class), any());
    }

    @Test
    void importTriggerWithConflictWithForceShouldBeImported() throws IOException {
        when(triggerService.updateTrigger(any(BpmnModelInstance.class), any()))
                .thenReturn(new ProcessDefinitionEntity());
        var trigger = trigger(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.NEW);

        var anImport = new EntitiesImport();
        anImport.setEntities(Collections.emptyList());

        importer.prepareTrigger(trigger, anImport, null);

        Mockito.verify(triggerService, times(1))
                .updateTrigger(any(BpmnModelInstance.class), any());
        assertEquals(EntityImportStatus.IMPORTED, trigger.getStatus());
    }

    @Test
    void importTriggerWhenTriggerServiceDoesNotReturnProcessDefinitionShouldBeAnError() throws IOException {
        when(triggerService.updateTrigger(any(BpmnModelInstance.class), any())).thenReturn(null);

        var trigger = trigger(EntityImportStatus.IMPORTING, ImportConflictResolveStrategy.NONE);
        trigger.setPreviousEntityId("prev_id");
        var anImport = new EntitiesImport();
        anImport.setEntities(Collections.emptyList());

        importer.prepareTrigger(trigger, anImport, null);

        assertEquals(EntityImportStatus.ERROR, trigger.getStatus());
        assertTrue(Strings.isNotBlank(trigger.getErrorMessage()));
    }

    @Test
    void importTriggerWhenCancelShouldBeCancelled() throws IOException {
        var trigger = trigger(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.CURRENT);
        trigger.setPreviousEntityId("prev_id");

        var anImport = new EntitiesImport();
        anImport.setEntities(Collections.emptyList());

        importer.prepareTrigger(trigger, anImport, null);
        Mockito.verify(triggerService, times(0))
                .updateTrigger(any(BpmnModelInstance.class), any());
    }

    @Test
    void importTriggerShouldUpdateTemplateIds() throws IOException {
        when(triggerService.addTrigger(any(BpmnModelInstance.class), any())).thenReturn(new ProcessDefinitionEntity());
        var template = trigger(EntityImportStatus.IMPORTING, ImportConflictResolveStrategy.NONE);

        var anImport = new EntitiesImport();
        anImport.setEntities(List.of(
                messageTemplate(EntityImportStatus.IMPORTED, OLD_TEMPLATE_ID, NEW_TEMPLATE_ID,
                        OLD_TEMPLATE_KEY, OLD_TEMPLATE_VERSION),
                messageTemplate(EntityImportStatus.SKIPPED, "wrong_id", "wrong_id")
        ));

        importer.prepareTrigger(template, anImport, null);

        Mockito.verify(triggerService, times(1)).addTrigger(bpmnModelCaptor.capture(), any());
        BpmnModelInstance savedModel = bpmnModelCaptor.getValue();

        assertNotNull(savedModel);
        assertEquals(NEW_TEMPLATE_ID, getTemplateId(savedModel));
    }

    @Test
    void importTriggerWithoutMessageTemplatesShouldRemoveTemplateId() throws IOException {
        var template = trigger(EntityImportStatus.IMPORTING, ImportConflictResolveStrategy.NONE);

        var anImport = new EntitiesImport();
        anImport.setEntities(List.of(
                messageTemplate(EntityImportStatus.SKIPPED, OLD_TEMPLATE_ID, null)
        ));

        importer.prepareTrigger(template, anImport, null);

        Mockito.verify(triggerService, times(1)).addTrigger(bpmnModelCaptor.capture(), any());
        BpmnModelInstance savedModel = bpmnModelCaptor.getValue();

        assertNotNull(savedModel);
        assertNull(getTemplateId(savedModel));
    }

    private String getTemplateId(BpmnModelInstance savedModel) {
        for (ServiceTask element : savedModel.getModelElementsByType(ServiceTask.class)) {
            var templateId = CustomAttributesHelper.getAttribute(
                    element,
                    CustomAttributesNames.TEMPLATE_ID,
                    String.class
            );
            if (templateId != null) {
                return templateId;
            }
        }
        return null;
    }

    private ImportingEntityDto trigger(EntityImportStatus status, ImportConflictResolveStrategy resolveStrategy)
            throws IOException {
        InputStream stream = getClass().getResourceAsStream("../trigger/with_send_email.xml");
        var xml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        var triggerExportDto = getTriggerExportDto(ENTITY_KEY, IMPORTING_ENTITY_VERSION, xml);

        var dto = new ImportingEntityDto();
        dto.setId(1L);
        dto.setType(ExportedEntityType.TRIGGER);
        dto.setConfig(triggerExportDto);
        dto.setStatus(status);
        dto.setResolveStrategy(resolveStrategy);

        return dto;
    }

    private ImportingEntity messageTemplate(EntityImportStatus status, String oldId, String newId) {
        return messageTemplate(status, oldId, newId, "key", 1);
    }

    private ImportingEntity messageTemplate(EntityImportStatus status,
                                            String oldId,
                                            String newId,
                                            String key,
                                            Integer version) {

        UnloadedMessageTemplateDto dto = getMessageTemplateExportDto(key, version);

        ImportingEntity importingEntity = new ImportingEntity(dto);
        importingEntity.setStatus(status);
        importingEntity.setPreviousEntityId(oldId);
        importingEntity.setNewEntityId(newId);

        return importingEntity;
    }
}
