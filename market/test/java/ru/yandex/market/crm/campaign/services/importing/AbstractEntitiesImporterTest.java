package ru.yandex.market.crm.campaign.services.importing;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.campaign.domain.export.EntitiesImport;
import ru.yandex.market.crm.campaign.domain.export.EntitiesImportStatus;
import ru.yandex.market.crm.campaign.domain.export.EntityImportStatus;
import ru.yandex.market.crm.campaign.domain.export.ExportedEntityType;
import ru.yandex.market.crm.campaign.domain.export.ImportConflictResolveStrategy;
import ru.yandex.market.crm.campaign.domain.export.ImportingEntity;
import ru.yandex.market.crm.campaign.dto.export.ImportingEntityDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedBlockTemplateDto;
import ru.yandex.market.crm.campaign.services.export.EntitiesExportBaseVersionsDAO;
import ru.yandex.market.crm.environment.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.block;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.getBlockTemplateExportDto;

@ExtendWith(MockitoExtension.class)
class AbstractEntitiesImporterTest {

    private static final String IMPORT_ID = "import_id";
    private static final String ENTITY_KEY = "block_key_1";
    private static final String ENTITY_KEY_2 = "block_key_2";
    private static final Integer IMPORTING_ENTITY_VERSION = 6;
    private static final String AUTHOR_LOGIN = "login";

    @Mock
    public EntitiesExportBaseVersionsDAO baseVersionsDAO;
    @Mock
    public EntitiesImportsDAO importsDAO;

    private AbstractEntitiesImporter importer;
    private final ArgumentCaptor<ImportingEntity> entityArgumentCaptor = ArgumentCaptor.forClass(ImportingEntity.class);

    @BeforeEach
    void setUp() {
        importer = spy(new AbstractEntitiesImporter(baseVersionsDAO, ExportedEntityType.BLOCK_TEMPLATE, importsDAO) {
            @Override
            protected ImportingEntityDto prepareEntityDto(ImportingEntityDto entityDto,
                                                          EntitiesImport importEntity,
                                                          Long authorUid) {
                return entityDto;
            }
        });
    }

    @Test
    void doImportOnlyNonFinalStatusesEntities() {
        var entities = List.of(
                block(EntityImportStatus.SKIPPED, ImportConflictResolveStrategy.NEW),
                block(EntityImportStatus.IMPORTED, ImportConflictResolveStrategy.NEW)
        );
        EntitiesImport anImport = getImport(List.of(
                getBlockImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING),
                getBlockImportEntities(ENTITY_KEY_2, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTED)
        ));

        importer.doImport(entities, anImport, null);

        verify(importer, times(0)).prepareEntityDto(any(), any(), any());
    }


    @Test
    void doImportHandleWhenErrorOccurs() {
        var entities = List.of(
                block(EntityImportStatus.IMPORTING,ImportConflictResolveStrategy.NEW)
        );
        EntitiesImport anImport = getImport(List.of(
                getBlockImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING),
                getBlockImportEntities(ENTITY_KEY_2, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTED)
        ));

        importer = spy(new AbstractEntitiesImporter(baseVersionsDAO, ExportedEntityType.BLOCK_TEMPLATE, importsDAO) {
            @Override
            protected ImportingEntityDto prepareEntityDto(ImportingEntityDto entityDto,
                                                          EntitiesImport importEntity,
                                                          Long authorUid) {
                throw new RuntimeException("Exception!");
            }
        });

        importer.doImport(entities, anImport, null);

        verify(importer, times(1)).prepareEntityDto(any(), any(), any());
        verify(importsDAO, times(1)).updateEntity(entityArgumentCaptor.capture());

        final ImportingEntity importingEntity = entityArgumentCaptor.getValue();

        assertNotNull(importingEntity);
        assertEquals(EntityImportStatus.ERROR, importingEntity.getStatus());
        assertFalse(importingEntity.getErrorMessage().isBlank());
    }

    private static EntitiesImport getImport(List<ImportingEntity> entities) {
        var anImport = new EntitiesImport();
        anImport.setId(IMPORT_ID);

        anImport.setEntities(entities);
        anImport.setAuthorLogin(AUTHOR_LOGIN);
        anImport.setStatus(EntitiesImportStatus.IMPORTING);
        anImport.setSourceEnvironment(Environment.PRODUCTION);

        return anImport;
    }

    private ImportingEntity getBlockImportEntities(String key, Integer version, EntityImportStatus status) {
        UnloadedBlockTemplateDto dto = getBlockTemplateExportDto(key, version);

        ImportingEntity importingEntity = new ImportingEntity(dto);
        importingEntity.setStatus(status);

        return importingEntity;
    }
}
