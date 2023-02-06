package ru.yandex.market.crm.campaign.services.importing;

import java.util.Collections;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.crm.campaign.domain.export.EntityImportStatus;
import ru.yandex.market.crm.campaign.domain.export.ExportedEntityType;
import ru.yandex.market.crm.campaign.domain.export.ImportConflictResolveStrategy;
import ru.yandex.market.crm.campaign.dto.export.ImportingEntityDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedBlockTemplateDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedMessageTemplateDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedTriggerDto;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateType;
import ru.yandex.market.crm.core.domain.templates.TemplateType;

public class EntitiesImportTestUtils {

    static final String ENTITY_KEY = "block_key_1";
    static final Integer IMPORTING_ENTITY_VERSION = 6;
    static final String OLD_ENTITY_ID = "alkdasklk";

    static ImportingEntityDto block(EntityImportStatus status, ImportConflictResolveStrategy conflictStrategy) {
        var block = getBlockTemplateExportDto(ENTITY_KEY, IMPORTING_ENTITY_VERSION);
        var dto = new ImportingEntityDto();
        dto.setId(1L);
        dto.setType(ExportedEntityType.BLOCK_TEMPLATE);
        dto.setConfig(block);
        dto.setStatus(status);
        dto.setResolveStrategy(conflictStrategy);
        dto.setPreviousEntityId(OLD_ENTITY_ID);

        return dto;
    }

    @NotNull
    static UnloadedMessageTemplateDto getMessageTemplateExportDto(String key, Integer version) {
        var dto = new UnloadedMessageTemplateDto();
        dto.setType(ExportedEntityType.MESSAGE_TEMPLATE);
        dto.setTemplateType(MessageTemplateType.EMAIL);

        EmailMessageConf config = new EmailMessageConf();
        config.setBlocks(Collections.emptyList());
        dto.setConfig(config);

        dto.setKey(key);
        dto.setVersion(version);
        return dto;
    }

    @NotNull
    static UnloadedTriggerDto getTriggerExportDto(String key, Integer version, String xml) {
        var dto = new UnloadedTriggerDto();
        dto.setType(ExportedEntityType.TRIGGER);
        dto.setKey(key);
        dto.setXmlDiagram(xml);
        dto.setVersion(version);
        return dto;
    }

    @NotNull
    static UnloadedBlockTemplateDto getBlockTemplateExportDto(String key, Integer version) {
        var dto = new UnloadedBlockTemplateDto();
        dto.setType(ExportedEntityType.BLOCK_TEMPLATE);
        dto.setBlockType(TemplateType.INFO);
        dto.setKey(key);
        dto.setVersion(version);
        return dto;
    }
}
