package ru.yandex.market.crm.campaign.services.importing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.campaign.domain.export.EntitiesImport;
import ru.yandex.market.crm.campaign.domain.export.EntityImportStatus;
import ru.yandex.market.crm.campaign.domain.export.ImportConflictResolveStrategy;
import ru.yandex.market.crm.campaign.domain.export.ImportingEntity;
import ru.yandex.market.crm.campaign.services.export.EntitiesExportBaseVersionsDAO;
import ru.yandex.market.crm.campaign.services.templates.BlockTemplateService;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.mcrm.tx.TxService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.OLD_ENTITY_ID;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.block;

@ExtendWith(MockitoExtension.class)
class BlockTemplatesImporterTest {

    private final ArgumentCaptor<ImportingEntity> entityArgumentCaptor = ArgumentCaptor.forClass(ImportingEntity.class);

    private BlockTemplatesImporter importer;

    @Mock
    private TxService txService;
    @Mock
    public BlockTemplateService blockTemplateService;
    @Mock
    public EntitiesExportBaseVersionsDAO baseVersionsDAO;
    @Mock
    public EntitiesImportsDAO importsDAO;

    @BeforeEach
    void setUp() {
        importer = new BlockTemplatesImporter(blockTemplateService, baseVersionsDAO, txService, importsDAO);
    }

    @Test
    void importBlockWhenShouldNot() {
        var block = block(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.CURRENT);

        importer.prepareBlock(block, new EntitiesImport());
        Mockito.verify(blockTemplateService, times(0)).saveTemplate(any(BlockTemplate.class));

        assertEquals(OLD_ENTITY_ID, block.getNewEntityId());
        assertEquals(EntityImportStatus.SKIPPED, block.getStatus());
    }

    @Test
    void importBlockWithoutConflictWithoutPreviouslySaved() {
        var block = block(EntityImportStatus.IMPORTING, ImportConflictResolveStrategy.NONE);
        block.setPreviousEntityId(null);

        importer.prepareBlock(block, new EntitiesImport());

        Mockito.verify(blockTemplateService, times(1))
                .saveFirstTemplateVersion(any(BlockTemplate.class));
        assertEquals(EntityImportStatus.IMPORTED, block.getStatus());
    }

    @Test
    void importBlockWithoutConflictWithPreviouslySaved() {
        var block = block(EntityImportStatus.IMPORTING, ImportConflictResolveStrategy.NONE);
        block.setPreviousEntityId("prev_id");

        importer.prepareBlock(block, new EntitiesImport());
        Mockito.verify(blockTemplateService, times(1))
                .saveTemplate(any(BlockTemplate.class));

        assertEquals(EntityImportStatus.IMPORTED, block.getStatus());
    }

    @Test
    void importBlockWithConflictWithForce() {
        var block = block(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.NEW);
        block.setPreviousEntityId("prev_id");

        importer.prepareBlock(block, new EntitiesImport());
        Mockito.verify(blockTemplateService, times(1))
                .saveTemplate(any(BlockTemplate.class));

        assertEquals(EntityImportStatus.IMPORTED, block.getStatus());
    }

    @Test
    void importBlockWithConflictWithoutForce() {
        var block = block(EntityImportStatus.CONFLICT, ImportConflictResolveStrategy.NONE);
        block.setPreviousEntityId("prev_id");

        importer.prepareBlock(block, new EntitiesImport());

        Mockito.verify(blockTemplateService, times(0)).saveTemplate(any(BlockTemplate.class));
        Mockito.verify(baseVersionsDAO, times(0)).save(anyString(), anyInt(), anyList());
    }
}
