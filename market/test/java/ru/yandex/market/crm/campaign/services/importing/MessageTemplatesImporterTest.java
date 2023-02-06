package ru.yandex.market.crm.campaign.services.importing;

import java.util.Collections;
import java.util.List;

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
import ru.yandex.market.crm.campaign.dto.export.AbstractUnloadedEntityDto;
import ru.yandex.market.crm.campaign.dto.export.ImportingEntityDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedBlockTemplateDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedMessageTemplateDto;
import ru.yandex.market.crm.campaign.services.export.EntitiesExportBaseVersionsDAO;
import ru.yandex.market.crm.campaign.services.messages.MessageTemplatesService;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.sending.conf.BlockConf;
import ru.yandex.market.mcrm.tx.TxService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.getMessageTemplateExportDto;

@ExtendWith(MockitoExtension.class)
class MessageTemplatesImporterTest {

    private static final String ENTITY_KEY = "block_key_1";
    private static final String BLOCK_KEY = "old_block_key";
    private static final String NEW_BLOCK_ID = "new_block_key";
    private static final String OLD_ENTITY_ID = "old_id";
    private static final Integer IMPORTING_ENTITY_VERSION = 6;

    private MessageTemplatesImporter importer;

    @Mock
    private TxService txService;
    @Mock
    public MessageTemplatesService messageTemplatesService;
    @Mock
    public EntitiesExportBaseVersionsDAO baseVersionsDAO;
    @Mock
    public EntitiesImportsDAO importsDAO;

    @BeforeEach
    void setUp() {
        importer = spy(new MessageTemplatesImporter(messageTemplatesService, baseVersionsDAO, txService, importsDAO));

        MessageTemplate<MessageConf> messageTemplate = new MessageTemplate<>();
        messageTemplate.setState(MessageTemplateState.DRAFT);
        lenient().when(messageTemplatesService.getTemplateById(any())).thenReturn(messageTemplate);
    }

    @Test
    void importTemplateWithConflictWithoutForce() {
        var template = template(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.NONE);
        template.setPreviousEntityId("prev_id");

        importer.prepareTemplate(template, new EntitiesImport());

        Mockito.verify(importsDAO, times(0)).updateEntity(any());
        Mockito.verify(messageTemplatesService, times(0)).update(eq(ENTITY_KEY), any());
    }

    @Test
    void importTemplateWithConflictWithForce() {
        var template = template(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.NEW);
        template.setPreviousEntityId("prev_id");
        var anImport = new EntitiesImport();
        anImport.setEntities(Collections.emptyList());

        importer.prepareTemplate(template, anImport);

        Mockito.verify(messageTemplatesService, times(1)).update(eq(ENTITY_KEY), any());
        Mockito.verify(messageTemplatesService, times(1)).publish(eq("prev_id"));
        assertEquals(EntityImportStatus.IMPORTED, template.getStatus());
        //по дефолту берем id сохраненного шаболона
        assertEquals(template.getPreviousEntityId(), template.getNewEntityId());
    }

    @Test
    void importTemplateWhenShouldNot() {
        var template = template(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.CURRENT);
        MessageTemplate<MessageConf> messageTemplate = new MessageTemplate<>();
        messageTemplate.setState(MessageTemplateState.DRAFT);
        when(messageTemplatesService.getTemplateById(OLD_ENTITY_ID)).thenReturn(messageTemplate);

        importer.prepareTemplate(template, new EntitiesImport());

        Mockito.verify(messageTemplatesService, times(0)).update(eq(ENTITY_KEY), any());
        assertEquals(EntityImportStatus.SKIPPED, template.getStatus());
        assertEquals(OLD_ENTITY_ID, template.getNewEntityId());
    }

    @Test
    void importTemplateWhenShouldNotAlsoUpdatesState() {
        var template = template(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.CURRENT);
        MessageTemplate<MessageConf> messageTemplate = new MessageTemplate<>();
        messageTemplate.setState(MessageTemplateState.PUBLISHED);
        when(messageTemplatesService.getTemplateById(OLD_ENTITY_ID)).thenReturn(messageTemplate);

        importer.prepareTemplate(template, new EntitiesImport());

        Mockito.verify(messageTemplatesService, times(0)).update(eq(ENTITY_KEY), any());


        assertEquals(EntityImportStatus.SKIPPED, template.getStatus());
        assertEquals(OLD_ENTITY_ID, template.getNewEntityId());
        assertEquals(MessageTemplateState.PUBLISHED, ((UnloadedMessageTemplateDto)template.getConfig()).getState());
    }

    @Test
    void importTemplateWithoutConflictWithPreviouslySaved() {
        var template = template(EntityImportStatus.IMPORTING, ImportConflictResolveStrategy.NONE);
        template.setPreviousEntityId("prev_id");
        var anImport = new EntitiesImport();
        anImport.setEntities(Collections.emptyList());

        importer.prepareTemplate(template, anImport);

        Mockito.verify(messageTemplatesService, times(1))
                .update(eq(ENTITY_KEY), any(MessageTemplate.class));
        Mockito.verify(messageTemplatesService, times(1)).publish(eq("prev_id"));

        assertEquals(EntityImportStatus.IMPORTED, template.getStatus());
    }

    @Test
    void importTemplateWithoutConflictWithoutPreviouslySaved() {
        var template = template(EntityImportStatus.IMPORTING, ImportConflictResolveStrategy.NONE);
        template.setPreviousEntityId(null);
        var anImport = new EntitiesImport();
        anImport.setEntities(Collections.emptyList());

        importer.prepareTemplate(template, anImport);

        Mockito.verify(messageTemplatesService, times(1))
                .addTemplate(any(MessageTemplate.class));
        assertEquals(EntityImportStatus.IMPORTED, template.getStatus());
    }

    @Test
    void importTemplateAndReplaceOldBlockIds() {

        UnloadedMessageTemplateDto exportDto = getMessageTemplateExportDto(ENTITY_KEY, IMPORTING_ENTITY_VERSION);
        ((EmailMessageConf)exportDto.getConfig()).setBlocks(List.of(
                blockConf(BLOCK_KEY)
        ));
        var template = template(
                exportDto,
                EntityImportStatus.IMPORTING,
                ImportConflictResolveStrategy.NONE
        );
        template.setPreviousEntityId(null);
        var anImport = new EntitiesImport();
        anImport.setEntities(List.of(
                blockEntity(EntityImportStatus.IMPORTED, BLOCK_KEY, NEW_BLOCK_ID),
                blockEntity(EntityImportStatus.SKIPPED, "wrong_key", "wrong_block_id")
        ));

        importer.prepareTemplate(template, anImport);

        ArgumentCaptor<MessageTemplate> templateCaptor = ArgumentCaptor.forClass(MessageTemplate.class);
        Mockito.verify(messageTemplatesService, times(1))
                .addTemplate(templateCaptor.capture());

        MessageTemplate finalTemplate = templateCaptor.getValue();
        List<BlockConf> blocks = getBlockConfs(finalTemplate);
        assertEquals(1, blocks.size());
        assertEquals(NEW_BLOCK_ID, blocks.get(0).getTemplate());

        List<BlockConf> blockTemplates = getBlockTemplates(template.getConfig());
        assertEquals(1, blockTemplates.size());
        assertEquals(NEW_BLOCK_ID, blockTemplates.get(0).getTemplate());
    }

    @Test
    void importTemplateWithUnimportedBlocksThrowsError() {

        UnloadedMessageTemplateDto exportDto = getMessageTemplateExportDto(ENTITY_KEY, IMPORTING_ENTITY_VERSION);
        ((EmailMessageConf)exportDto.getConfig()).setBlocks(List.of(
                blockConf(BLOCK_KEY)
        ));
        var template = template(
                exportDto,
                EntityImportStatus.IMPORTING,
                ImportConflictResolveStrategy.NONE
        );
        var anImport = new EntitiesImport();
        anImport.setEntities(List.of(
                blockEntity(EntityImportStatus.ERROR, BLOCK_KEY, "wrong_block_id")
        ));

        assertThrows(RuntimeException.class, () -> importer.prepareTemplate(template, anImport));
    }

    private List<BlockConf> getBlockConfs(MessageTemplate finalTemplate) {
        assertNotNull(finalTemplate);
        return ((EmailMessageConf) finalTemplate.getConfig()).getBlocks();
    }

    private List<BlockConf> getBlockTemplates(AbstractUnloadedEntityDto dto) {
        assertNotNull(dto);
        assertTrue(dto instanceof UnloadedMessageTemplateDto);
        UnloadedMessageTemplateDto messageDto = (UnloadedMessageTemplateDto) dto;
        assertTrue(messageDto.getConfig() instanceof EmailMessageConf);

        return ((EmailMessageConf)messageDto.getConfig()).getBlocks();
    }

    private ImportingEntity blockEntity(EntityImportStatus status, String key, String newId) {
        var blockConf = new UnloadedBlockTemplateDto();
        blockConf.setKey(key);
        blockConf.setId(key);
        var entity = new ImportingEntity(blockConf);
        entity.setType(ExportedEntityType.BLOCK_TEMPLATE);
        entity.setStatus(status);
        entity.setNewEntityId(newId);

        return entity;
    }

    private BlockConf blockConf(String key) {
        BannerBlockConf block = new BannerBlockConf();
        block.setId(key);
        block.setTemplate(key);

        return block;
    }

    private ImportingEntityDto template(UnloadedMessageTemplateDto exportDto,
                                        EntityImportStatus status,
                                        ImportConflictResolveStrategy resolveStrategy) {
        var dto = new ImportingEntityDto();
        dto.setId(1L);
        dto.setType(ExportedEntityType.MESSAGE_TEMPLATE);
        dto.setConfig(exportDto);
        dto.setStatus(status);
        dto.setResolveStrategy(resolveStrategy);
        dto.setPreviousEntityId(OLD_ENTITY_ID);

        return dto;
    }

    private ImportingEntityDto template(EntityImportStatus status, ImportConflictResolveStrategy resolveStrategy) {
        var templateExportDto = getMessageTemplateExportDto(ENTITY_KEY, IMPORTING_ENTITY_VERSION);

        return template(templateExportDto, status, resolveStrategy);
    }
}
