package ru.yandex.market.crm.campaign.services.importing;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.crm.campaign.domain.export.BaseVersion;
import ru.yandex.market.crm.campaign.domain.export.EntitiesImport;
import ru.yandex.market.crm.campaign.domain.export.EntitiesImportStatus;
import ru.yandex.market.crm.campaign.domain.export.EntityImportStatus;
import ru.yandex.market.crm.campaign.domain.export.ExportedEntityType;
import ru.yandex.market.crm.campaign.domain.export.ImportingEntity;
import ru.yandex.market.crm.campaign.domain.export.UnloadedEntities;
import ru.yandex.market.crm.campaign.dto.export.AbstractUnloadedEntityDto;
import ru.yandex.market.crm.campaign.dto.export.ImportDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedBlockTemplateDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedMessageTemplateDto;
import ru.yandex.market.crm.campaign.dto.export.UnloadedTriggerDto;
import ru.yandex.market.crm.campaign.services.export.EntitiesExportBaseVersionsDAO;
import ru.yandex.market.crm.campaign.services.messages.MessageTemplatesService;
import ru.yandex.market.crm.campaign.services.templates.BlockTemplateService;
import ru.yandex.market.crm.campaign.services.trigger.TriggerService;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.MessageTemplateState;
import ru.yandex.market.crm.core.domain.messages.PushMessageConf;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
import ru.yandex.market.crm.core.domain.trigger.TriggerCustomInfo;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.crm.environment.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.getBlockTemplateExportDto;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.getMessageTemplateExportDto;
import static ru.yandex.market.crm.campaign.services.importing.EntitiesImportTestUtils.getTriggerExportDto;

@ExtendWith(MockitoExtension.class)
public class EntitiesImportServiceTest {

    private static final String TRIGGER_KEY = "cancel";
    private static final String IMPORT_ID = "import_id";
    private static final String ENTITY_KEY = "block_key_1";
    private static final Integer IMPORTING_ENTITY_VERSION = 6;
    private static final Integer SAVED_ENTITY_VERSION = 2;
    private static final String AUTHOR_LOGIN = "AZAZAZA";
    private static final Long IMPORT_ENTITY_ID = 12112L;

    @Mock
    public EntitiesImportsDAO entitiesImportsDAO;
    @Mock
    public EntitiesExportBaseVersionsDAO entitiesExportBaseVersionsDAO;
    @Mock
    public BlockTemplateService blockTemplateService;
    @Mock
    public MessageTemplatesService messageTemplatesService;
    @Mock
    public BlockTemplatesImporter blockTemplatesImporter;
    @Mock
    public MessageTemplatesImporter messageTemplatesImporter;
    @Mock
    public TriggersImporter triggersImporter;
    @Mock
    public TriggerService triggerService;

    private final TestEnvironmentResolver environmentResolver = new TestEnvironmentResolver();
    private final ArgumentCaptor<EntitiesImport> importArgumentCaptor = ArgumentCaptor.forClass(EntitiesImport.class);
    private final ArgumentCaptor<ImportingEntity> importingEntityArgumentCaptor =
            ArgumentCaptor.forClass(ImportingEntity.class);

    private EntitiesImportService importService;

    private static Stream<Arguments> entitiesBaseVersionsAndImportStatusesData() {
        return Stream.of(
                Arguments.of(SAVED_ENTITY_VERSION, EntityImportStatus.SKIPPED),
                Arguments.of(SAVED_ENTITY_VERSION + 1, EntityImportStatus.CONFLICT),
                Arguments.of(SAVED_ENTITY_VERSION - 1, EntityImportStatus.CONFLICT)
        );
    }

    private static Stream<Arguments> entitiesBaseVersionsWithOriginAndImportStatusesData() {
        return Stream.of(
                Arguments.of(SAVED_ENTITY_VERSION, IMPORTING_ENTITY_VERSION, EntityImportStatus.SKIPPED),
                Arguments.of(SAVED_ENTITY_VERSION, IMPORTING_ENTITY_VERSION + 1, EntityImportStatus.IMPORTING),
                Arguments.of(SAVED_ENTITY_VERSION, IMPORTING_ENTITY_VERSION - 1, EntityImportStatus.IMPORTING)
        );
    }

    private static Stream<Arguments> savedBaseVersionsAndImportStatusesData() {
        return Stream.of(
                Arguments.of(IMPORTING_ENTITY_VERSION, EntityImportStatus.SKIPPED),
                Arguments.of(IMPORTING_ENTITY_VERSION - 1, EntityImportStatus.IMPORTING),
                Arguments.of(IMPORTING_ENTITY_VERSION + 1, EntityImportStatus.CONFLICT)
        );
    }

    private static Stream<Arguments> messageTemplateStatesData() {
        return Stream.of(
                Arguments.of(MessageTemplateState.PUBLISHED),
                Arguments.of(MessageTemplateState.DRAFT)
        );
    }

    @BeforeEach
    void setUp() {
        environmentResolver.setEnvironment(Environment.PRODUCTION);

        importService = new EntitiesImportService(
                entitiesExportBaseVersionsDAO,
                blockTemplateService,
                messageTemplatesService,
                blockTemplatesImporter,
                messageTemplatesImporter,
                triggersImporter,
                triggerService,
                entitiesImportsDAO,
                environmentResolver
        );
    }

    @Test
    public void uploadImportFile() {
        var exportResult = new UnloadedEntities();
        exportResult.setSourceEnvironment(Environment.PRODUCTION);
        List<AbstractUnloadedEntityDto> entities = getEntities();
        exportResult.setEntities(entities);

        var importEntities = entities.stream()
                .map(ImportingEntity::new)
                .peek(x -> {
                    x.setStatus(EntityImportStatus.IMPORTING);
                    x.setType(ExportedEntityType.BLOCK_TEMPLATE);
                })
                .collect(Collectors.toList());

        importService.uploadImportFile(exportResult);

        Mockito.verify(entitiesImportsDAO, Mockito.times(1))
                .createImport(importArgumentCaptor.capture());

        assertImport(importArgumentCaptor.getValue(), getImport(importEntities));
    }

    @Test
    void refreshStateWithWrongIdShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> importService.refreshState("wrong_id"));
    }

    @Test
    void refreshStateOfBlockTemplateWithoutSavedWithSameKey() {

        var block = getBlockImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);

        EntitiesImport anImport = getImport(Collections.singletonList(block));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(block, EntityImportStatus.IMPORTING);
    }

    @Test
    void refreshStateOfBlockTemplateWhenAlreadyInErrorStatus() {

        var block = getBlockImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.ERROR);

        EntitiesImport anImport = getImport(Collections.singletonList(block));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(block, EntityImportStatus.ERROR);
    }

    @Test
    void refreshStateOfBlockTemplateWithSavedWithSameKeyWithoutBaseVersions() {

        var block = getBlockImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        mockBlockTemplateService();

        EntitiesImport anImport = getImport(Collections.singletonList(block));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(block, EntityImportStatus.CONFLICT);
    }

    @ParameterizedTest
    @MethodSource("entitiesBaseVersionsAndImportStatusesData")
    void refreshStateOfBlockTemplateWithOriginalBaseVersion(Integer blockVersion, EntityImportStatus expectedStatus) {

        var block = getBlockImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        BaseVersion baseVersion = new BaseVersion(Environment.PRODUCTION, blockVersion, IMPORTING_ENTITY_VERSION);
        block.getConfig().setBaseVersions(Collections.singletonList(baseVersion));
        mockBlockTemplateService();

        EntitiesImport anImport = getImport(Collections.singletonList(block));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(block, expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("entitiesBaseVersionsAndImportStatusesData")
    void refreshStateOfMessageTemplateWithOriginalBaseVersion(Integer templateVersion, EntityImportStatus expectedStatus) {

        mockMessageTemplateService();
        var messageTemplate = getMessageTemplateImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        BaseVersion baseVersion = new BaseVersion(Environment.PRODUCTION, templateVersion, IMPORTING_ENTITY_VERSION);
        messageTemplate.getConfig().setBaseVersions(Collections.singletonList(baseVersion));

        EntitiesImport anImport = getImport(Collections.singletonList(messageTemplate));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(messageTemplate, expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("entitiesBaseVersionsAndImportStatusesData")
    void refreshStateOfTriggerWithOriginalBaseVersion(Integer triggerVersion,
                                                      EntityImportStatus expectedStatus) throws IOException {
        mockTriggerService();
        var trigger = getTriggerImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        BaseVersion baseVersion = new BaseVersion(Environment.PRODUCTION, triggerVersion, IMPORTING_ENTITY_VERSION);
        trigger.getConfig().setBaseVersions(Collections.singletonList(baseVersion));

        EntitiesImport anImport = getImport(Collections.singletonList(trigger));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(trigger, expectedStatus);
    }


    @ParameterizedTest
    @MethodSource("entitiesBaseVersionsWithOriginAndImportStatusesData")
    void refreshStateOfBlockTemplateBaseVersion(Integer baseEntityVersion,
                                                Integer originVersion,
                                                EntityImportStatus expectedStatus) {

        var block = getBlockImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        BaseVersion baseVersion = new BaseVersion(Environment.PRODUCTION, baseEntityVersion, originVersion);
        block.getConfig().setBaseVersions(Collections.singletonList(baseVersion));
        mockBlockTemplateService();

        EntitiesImport anImport = getImport(Collections.singletonList(block));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(block, expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("entitiesBaseVersionsWithOriginAndImportStatusesData")
    void refreshStateOfMessageTemplateWithBaseVersion(Integer baseEntityVersion,
                                                      Integer originVersion,
                                                      EntityImportStatus expectedStatus) {

        mockMessageTemplateService();
        var messageTemplate = getMessageTemplateImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        BaseVersion baseVersion = new BaseVersion(Environment.PRODUCTION, baseEntityVersion, originVersion);
        messageTemplate.getConfig().setBaseVersions(Collections.singletonList(baseVersion));

        EntitiesImport anImport = getImport(Collections.singletonList(messageTemplate));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(messageTemplate, expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("entitiesBaseVersionsWithOriginAndImportStatusesData")
    void refreshStateOfTriggerWithBaseVersion(Integer baseEntityVersion,
                                              Integer originVersion,
                                              EntityImportStatus expectedStatus) throws IOException {
        mockTriggerService();
        var trigger = getTriggerImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        BaseVersion baseVersion = new BaseVersion(Environment.PRODUCTION, baseEntityVersion, originVersion);
        trigger.getConfig().setBaseVersions(Collections.singletonList(baseVersion));

        EntitiesImport anImport = getImport(Collections.singletonList(trigger));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(trigger, expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("savedBaseVersionsAndImportStatusesData")
    void refreshStateOfBlockTemplateWithBaseVersionWithSavedBaseVersions(Integer blockVersion,
                                                                         EntityImportStatus expectedStatus) {
        mockBlockTemplateService();
        var block = getBlockImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        EntitiesImport anImport = getImport(Collections.singletonList(block));
        prepareImportDAO(anImport);

        when(entitiesExportBaseVersionsDAO.getBaseVersionsByKeyAndVersion(ENTITY_KEY, SAVED_ENTITY_VERSION))
                .thenReturn(List.of(new BaseVersion(Environment.PRODUCTION, blockVersion)));
        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(block, expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("savedBaseVersionsAndImportStatusesData")
    void refreshStateOfMessageTemplateWithBaseVersionWithSavedBaseVersions(Integer messageTemplateVersion,
                                                                           EntityImportStatus expectedStatus) {
        mockMessageTemplateService();
        var messageTemplate = getMessageTemplateImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        EntitiesImport anImport = getImport(Collections.singletonList(messageTemplate));
        prepareImportDAO(anImport);

        when(entitiesExportBaseVersionsDAO.getBaseVersionsByKeyAndVersion(ENTITY_KEY, SAVED_ENTITY_VERSION))
                .thenReturn(List.of(new BaseVersion(Environment.PRODUCTION, messageTemplateVersion)));
        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(messageTemplate, expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("savedBaseVersionsAndImportStatusesData")
    void refreshStateOfTriggerWithBaseVersionWithSavedBaseVersions(Integer triggerVersion,
                                                                   EntityImportStatus expectedStatus) throws IOException {
        mockTriggerService();
        var trigger = getTriggerImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        EntitiesImport anImport = getImport(Collections.singletonList(trigger));
        prepareImportDAO(anImport);

        when(entitiesExportBaseVersionsDAO.getBaseVersionsByKeyAndVersion(ENTITY_KEY, SAVED_ENTITY_VERSION))
                .thenReturn(List.of(new BaseVersion(Environment.PRODUCTION, triggerVersion)));
        importService.refreshState(IMPORT_ID);

        assertUpdateImportedEntity(trigger, expectedStatus);
    }

    @ParameterizedTest
    @EnumSource(MessageTemplateState.class)
    void refreshStateOfMessageTemplateWhenSkippedShouldActualizeState(MessageTemplateState templateState) {

        mockMessageTemplateService();
        mockMessageTemplateServiceGetTemplate(templateState);

        var messageTemplate = getMessageTemplateImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        BaseVersion baseVersion = new BaseVersion(Environment.PRODUCTION, SAVED_ENTITY_VERSION, IMPORTING_ENTITY_VERSION);
        messageTemplate.getConfig().setBaseVersions(Collections.singletonList(baseVersion));

        EntitiesImport anImport = getImport(Collections.singletonList(messageTemplate));
        prepareImportDAO(anImport);

        importService.refreshState(IMPORT_ID);

        Mockito.verify(entitiesImportsDAO, Mockito.times(1))
                .updateEntity(importingEntityArgumentCaptor.capture());

        ImportingEntity actualEntity = importingEntityArgumentCaptor.getValue();
        assertEquals(templateState, ((UnloadedMessageTemplateDto) actualEntity.getConfig()).getState());
    }

    private void mockMessageTemplateServiceGetTemplate(MessageTemplateState templateState) {
        var messageTemplate = new MessageTemplate<>();

        String entityId = String.format("%s:%d", ENTITY_KEY, 1);
        messageTemplate.setId(entityId);
        messageTemplate.setVersion(SAVED_ENTITY_VERSION);
        messageTemplate.setState(templateState);

        when(messageTemplatesService.getTemplateById(entityId)).thenReturn(messageTemplate);
    }


    @Test
    void showImportInfoWithWrongIdShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> importService.showImportInfo("wrong_id"));
    }

    @Test
    void showImportInfo() throws IOException {

        var messageTemplate = getMessageTemplateImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        var trigger = getTriggerImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING);
        EntitiesImport anImport = getImport(List.of(trigger, messageTemplate));
        prepareImportDAO(anImport);

        ImportDto importDto = importService.showImportInfo(IMPORT_ID);

        assertNotNull(importDto);
        assertEquals(anImport.getId(), importDto.getId());
        assertEquals(anImport.getStatus(), importDto.getStatus());
        assertEquals(anImport.getSourceEnvironment(), importDto.getSourceEnvironment());
        assertEquals(anImport.getAuthorLogin(), importDto.getAuthorLogin());
        assertEquals(anImport.getEntities().size(), importDto.getEntities().size());
    }

    @Test
    void publishTemplatesWithWrongImportIdShouldThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> importService.publishTemplate("wrong_id", 146L));
    }

    @Test
    void publishImportedTemplate() {

        ImportingEntity messageTemplate = getMessageTemplateImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTED);
        messageTemplate.setNewEntityId("new_id");

        EntitiesImport anImport = getImport(List.of(
                getMessageTemplateImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING),
                messageTemplate
        ));
        prepareImportDAO(anImport);

        importService.publishTemplate(IMPORT_ID, IMPORT_ENTITY_ID);

        verify(messageTemplatesService, times(1)).publish("new_id");
    }

    @Test
    void publishSkippedTemplate() {

        ImportingEntity messageTemplate = getMessageTemplateImportEntities(ENTITY_KEY,
                IMPORTING_ENTITY_VERSION, EntityImportStatus.SKIPPED);
        messageTemplate.setNewEntityId("new_id");

        EntitiesImport anImport = getImport(List.of(
                getMessageTemplateImportEntities(ENTITY_KEY, IMPORTING_ENTITY_VERSION, EntityImportStatus.IMPORTING),
                messageTemplate
        ));
        prepareImportDAO(anImport);

        importService.publishTemplate(IMPORT_ID, IMPORT_ENTITY_ID);

        verify(messageTemplatesService, times(1)).publish("new_id");
    }

    @Test
    void doImportWithWrongIdShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> importService.doImport(
                Collections.emptyList(),
                "wrong_id",
                null
        ));
    }

    private void assertUpdateImportedEntity(ImportingEntity expectedEntity, EntityImportStatus expectedStatus) {
        Mockito.verify(entitiesImportsDAO, Mockito.times(1))
                .updateEntity(importingEntityArgumentCaptor.capture());

        ImportingEntity actualEntity = importingEntityArgumentCaptor.getValue();

        assertEquals(expectedEntity.getId(), actualEntity.getId());
        assertEquals(expectedStatus, actualEntity.getStatus());
        assertEquals(expectedEntity.getType(), actualEntity.getType());
        assertEquals(expectedEntity.getConfig(), actualEntity.getConfig());
        assertEquals(expectedEntity.getNewEntityId(), actualEntity.getNewEntityId());
        assertEquals(expectedEntity.getPreviousEntityId(), actualEntity.getPreviousEntityId());
    }

    private void mockBlockTemplateService() {
        BlockTemplate blockTemplate = new BlockTemplate();
        blockTemplate.setId(String.format("%s:%d", ENTITY_KEY, 1));
        blockTemplate.setVersion(SAVED_ENTITY_VERSION);
        when(blockTemplateService.getLastVersion(ENTITY_KEY)).thenReturn(blockTemplate);
    }

    private void mockMessageTemplateService() {
        var messageTemplate = new MessageTemplate<>();
        String templateId = String.format("%s:%d", ENTITY_KEY, 1);
        messageTemplate.setId(templateId);
        messageTemplate.setVersion(SAVED_ENTITY_VERSION);
        messageTemplate.setConfig(new PushMessageConf());

        when(messageTemplatesService.getLastTemplateUntransactional(ENTITY_KEY)).thenReturn(messageTemplate);
        lenient().when(messageTemplatesService.getTemplateById(templateId)).thenReturn(messageTemplate);
    }

    private void mockTriggerService() {
        TriggerCustomInfo triggerCustomInfo = new TriggerCustomInfo(
                ENTITY_KEY,
                String.format("%s:%d", ENTITY_KEY, SAVED_ENTITY_VERSION),
                SAVED_ENTITY_VERSION,
                false,
                null
        );
        when(triggerService.getLastVersionInfo(ENTITY_KEY)).thenReturn(triggerCustomInfo);

    }

    private ImportingEntity getMessageTemplateImportEntities(String key, Integer version, EntityImportStatus status) {
        UnloadedMessageTemplateDto dto = getMessageTemplateExportDto(key, version);

        ImportingEntity importingEntity = new ImportingEntity(dto);
        importingEntity.setStatus(status);
        importingEntity.setId(IMPORT_ENTITY_ID);

        return importingEntity;
    }

    private ImportingEntity getTriggerImportEntities(String key, Integer version, EntityImportStatus status) throws IOException {
        UnloadedTriggerDto dto = getTriggerExportDto(key, version, null);

        ImportingEntity importingEntity = new ImportingEntity(dto);
        importingEntity.setStatus(status);

        return importingEntity;
    }

    private ImportingEntity getBlockImportEntities(String key, Integer version, EntityImportStatus status) {
        UnloadedBlockTemplateDto dto = getBlockTemplateExportDto(key, version);

        ImportingEntity importingEntity = new ImportingEntity(dto);
        importingEntity.setStatus(status);

        return importingEntity;
    }

    private void prepareImportDAO(EntitiesImport anImport, EntitiesImport... otherImports) {
        when(entitiesImportsDAO.getEntitiesImport(IMPORT_ID)).thenReturn(anImport, otherImports);
    }

    private void assertImport(EntitiesImport actual, EntitiesImport expected) {
        assertNotNull(actual);
        assertEquals(expected.getSourceEnvironment(), actual.getSourceEnvironment());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEntities(actual.getEntities(), expected.getEntities());
    }

    private void assertEntities(List<ImportingEntity> actual, List<ImportingEntity> expected) {
        if (expected == null) {
            assertNull(actual);
        }
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
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

    private static List<AbstractUnloadedEntityDto> getEntities() {
        var trigger = new UnloadedTriggerDto();
        trigger.setXmlDiagram("azaz");
        trigger.setKey(TRIGGER_KEY);
        trigger.setVersion(1);

        return Collections.singletonList(trigger);
    }
}
