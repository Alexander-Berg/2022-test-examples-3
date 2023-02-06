package ru.yandex.canvas.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.Bundle;
import ru.yandex.canvas.model.CreativeData;
import ru.yandex.canvas.model.CreativeDocument;
import ru.yandex.canvas.model.CreativeDocumentBatch;
import ru.yandex.canvas.model.CreativeMigrationResult;
import ru.yandex.canvas.model.File;
import ru.yandex.canvas.model.MediaSet;
import ru.yandex.canvas.model.MediaSetItem;
import ru.yandex.canvas.model.MediaSetSubItem;
import ru.yandex.canvas.model.elements.Description;
import ru.yandex.canvas.model.elements.Element;
import ru.yandex.canvas.model.elements.Fade;
import ru.yandex.canvas.model.elements.Image;
import ru.yandex.canvas.model.elements.Legal;
import ru.yandex.canvas.model.elements.Logo;
import ru.yandex.canvas.model.presets.Preset;
import ru.yandex.canvas.model.presets.PresetItem;
import ru.yandex.canvas.model.presets.PresetSelectionCriteria;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.canvas.model.presets.PresetTag.COMMON;

/**
 * @author pupssman
 */
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MigrationServiceTest {
    @Autowired
    private PresetsService presetsService;
    @Autowired
    private MigrationService migrationService;

    private static PresetItem getPresetItem() {
        PresetItem presetItem = new PresetItem();
        presetItem.setWidth(728);
        presetItem.setHeight(90);
        presetItem.setBundle(new Bundle("A_BUNDLE", 2));
        presetItem.setElements(new ArrayList<>());
        presetItem.setMediaSets(new HashMap<>());

        return presetItem;
    }

    private static MigrationService getMigrationServiceForPresets(Preset... presets) {
        File mockFile = new File();
        mockFile.setUrl("MOCKED");

        FileService fileService = Mockito.mock(FileService.class);
        Mockito.when(fileService.getByIdInternal(Mockito.anyString())).thenReturn(Optional.of(mockFile));
        PresetsService presetsService = Mockito.mock(PresetsService.class);

        MigrationService migrationService = new MigrationService(presetsService, fileService,
                new Jackson2ObjectMapperBuilder().build());

        Mockito.when(presetsService.getList(any(), Mockito.isNull(), Mockito.isNull())).thenReturn(Arrays.asList(presets));

        return migrationService;
    }

    private static CreativeData getCreativeData() {
        CreativeData creativeData = new CreativeData();
        creativeData.setMediaSets(new HashMap<>());
        creativeData.setBundle(new Bundle("A_BUNDLE", 1));
        creativeData.setElements(new ArrayList<>()); // not Collections.emptyList() as it is changed later;
        creativeData.setWidth(728);
        creativeData.setHeight(90);
        return creativeData;
    }

    private static CreativeDocumentBatch getBatch(CreativeData... data) {
        CreativeDocumentBatch creativeDocumentBatch = new CreativeDocumentBatch();
        creativeDocumentBatch.setItems(Arrays.stream(data).map(creativeData -> {
            CreativeDocument creativeDocument = new CreativeDocument();
            creativeDocument.setData(creativeData);
            return creativeDocument;
        }).collect(Collectors.toList()));
        creativeDocumentBatch.setId("123");  // we need ID here so the getId works
        return creativeDocumentBatch;
    }

    @Test
    public void testFindPreset() {
        PresetItem presetItem = getPresetItem();

        Preset preset = new Preset(1, "", "", singleton(COMMON), singletonList(presetItem));
        MigrationService migrationService = getMigrationServiceForPresets(preset);

        PresetSelectionCriteria selectionCriteria =
                PresetSelectionCriteria.builder().withCpmCommon(true).withCpmGeoproduct(false).build();
        assertEquals(Optional.of(preset), migrationService.findPreset(getBatch(getCreativeData()), selectionCriteria));

        assertEquals(Optional.of(presetItem), migrationService.findPresetItem(preset, getCreativeData()));
    }

    @Test
    public void testNotFindPreset() {
        PresetItem presetItem = getPresetItem();

        Preset preset = new Preset(1, "", "", singleton(COMMON), singletonList(presetItem));
        MigrationService migrationService = getMigrationServiceForPresets(preset);

        // wrong width
        CreativeData creativeData = getCreativeData();
        creativeData.setWidth(creativeData.getWidth() + 1);
        assertFalse(migrationService.findPresetItem(preset, creativeData).isPresent());

        // wrong height
        creativeData = getCreativeData();
        creativeData.setHeight(creativeData.getHeight() + 1);
        assertFalse(migrationService.findPresetItem(preset, creativeData).isPresent());

        // wrong bundle
        creativeData = getCreativeData();
        creativeData.getBundle().setName(creativeData.getBundle().getName() + "_NOT");
        PresetSelectionCriteria selectionCriteria =
                PresetSelectionCriteria.builder().withCpmCommon(true).withCpmGeoproduct(false).build();
        assertFalse(migrationService.findPreset(getBatch(creativeData), selectionCriteria).isPresent());
    }

    @Test
    public void testMigrateCreativeSmoke() {
        PresetItem presetItem = getPresetItem();

        MigrationService migrationService = getMigrationServiceForPresets(
                new Preset(1, "", "", singleton(COMMON), singletonList(presetItem)));

        CreativeData creativeData = getCreativeData();
        CreativeMigrationResult migrationResult = migrationService.migrate(creativeData, getPresetItem());

        assertEquals(2, creativeData.getBundle().getVersion()); // it actually changed!
        assertEquals("A_BUNDLE", creativeData.getBundle().getName());
        assertEquals(false, migrationResult.isHasCropChanges());
        assertEquals(0, migrationResult.getNewElements().size());
    }

    @Test
    public void testMigrateCreativeAddElement() {
        PresetItem presetItem = getPresetItem();
        MigrationService migrationService = getMigrationServiceForPresets();
        CreativeData creativeData = getCreativeData();

        Description description = new Description();
        description.setType("description");

        presetItem.getElements().add(description);

        CreativeMigrationResult migrationResult = migrationService.migrate(creativeData, presetItem);

        assertEquals(1, creativeData.getElements().size()); // 1 element added
        assertEquals(Description.class, creativeData.getElements().get(0).getClass());
        assertEquals(false, creativeData.getElements().get(0).getAvailable());  // new element is added disabled
        assertEquals(false, migrationResult.isHasCropChanges());
        assertEquals(1, migrationResult.getNewElements().size());
        assertThat(migrationResult.getNewElements(), contains("description"));
    }

    @Test
    public void testMigrateLeaveExistingElement() {
        PresetItem presetItem = getPresetItem();
        MigrationService migrationService = getMigrationServiceForPresets();
        CreativeData creativeData = getCreativeData();

        Fade fade = new Fade();
        fade.setType("fade");
        creativeData.getElements().add(fade);

        Fade presetFade = new Fade();
        presetFade.setType("fade");

        presetItem.getElements().add(presetFade);

        CreativeMigrationResult migrationResult = migrationService.migrate(creativeData, presetItem);

        assertEquals(1, creativeData.getElements().size(), 1); // still 1 element
        assertEquals(fade, creativeData.getElements().get(0));
        assertEquals(false, migrationResult.isHasCropChanges());
        assertEquals(0, migrationResult.getNewElements().size());
    }

    @Test
    public void testChangeInSizeIsFlaggedForCropCleanup() {
        MigrationService migrationService = getMigrationServiceForPresets();

        Image image = new Image();
        image.setType("image");
        image.setMediaSet("imageSet");
        Image.Options imageOptions = new Image.Options();
        imageOptions.setWidth(100);
        imageOptions.setHeight(100);
        image.setOptions(imageOptions);

        Image.Options presetElementOptions = new Image.Options();
        presetElementOptions.setWidth(100);
        presetElementOptions.setHeight(100);

        Image presetElement = new Image();
        presetElement.setType("image");
        presetElement.setMediaSet("imageSet");
        presetElement.setOptions(presetElementOptions);

        assertEquals(
                0,
                migrationService.migrate(singleton(image), singleton(presetElement)).getMediaSetsToResetCrop().size());

        imageOptions.setHeight(101);
        assertEquals(
                "imageSet",
                migrationService.migrate(singleton(image), singleton(presetElement)).getMediaSetsToResetCrop().get(0));

        imageOptions.setHeight(100);
        imageOptions.setWidth(101);
        assertEquals(
                "imageSet",
                migrationService.migrate(singleton(image), singleton(presetElement)).getMediaSetsToResetCrop().get(0));
    }

    @Test
    public void testCropIsReset() {
        MigrationService migrationService = spy(getMigrationServiceForPresets());

        MigrationService.ElementsMigrationResult migrationResult = new MigrationService.ElementsMigrationResult(
                emptyList(),
                singletonList("newSet"),
                emptyList());

        when(migrationService.migrate(anyList(), anyList())).thenReturn(migrationResult);

        PresetItem presetItem = getPresetItem();
        CreativeData creativeData = getCreativeData();

        MediaSet mediaSet = new MediaSet();
        creativeData.getMediaSets().put("newSet", mediaSet);
        MediaSetItem mediaSetItem = new MediaSetItem();
        mediaSet.setItems(singletonList(mediaSetItem));
        MediaSetSubItem mediaSetSubItem = new MediaSetSubItem();
        mediaSetItem.setItems(singletonList(mediaSetSubItem));
        mediaSetSubItem.setCroppedFileId("12345");
        mediaSetSubItem.setFileId("54321");
        mediaSetSubItem.setUrl("ololo:pewpew");

        CreativeMigrationResult result = migrationService.migrate(creativeData, presetItem);

        assertEquals(true, result.isHasCropChanges());
        assertNull(mediaSetSubItem.getCroppedFileId());
        assertEquals("MOCKED", mediaSetSubItem.getUrl());
    }

    @Test
    public void testMediaSetsAreAdded() {
        MigrationService migrationService = spy(getMigrationServiceForPresets());

        Logo presetElement = new Logo();
        presetElement.setType("logo");
        presetElement.setMediaSet("newSet");

        MigrationService.ElementsMigrationResult migrationResult = new MigrationService.ElementsMigrationResult(
                singletonList(presetElement),
                emptyList(),
                emptyList());

        when(migrationService.migrate(anyList(), anyList())).thenReturn(migrationResult);

        PresetItem presetItem = getPresetItem();
        CreativeData creativeData = getCreativeData();

        presetItem.getMediaSets().put("newSet", new MediaSet());

        CreativeMigrationResult result = migrationService.migrate(creativeData, presetItem);

        assertEquals(1, result.getNewElements().size());
        assertThat(result.getNewElements(), contains("logo"));
        assertEquals(1, creativeData.getMediaSets().size());
        assertEquals(true, creativeData.getMediaSets().containsKey("newSet"));
    }

    @Test
    public void testNewElements() {
        MigrationService migrationService = getMigrationServiceForPresets();

        Logo presetElement = new Logo();
        presetElement.setType("logo");
        presetElement.setMediaSet("testSet");

        assertEquals(presetElement,
                migrationService.migrate(emptyList(), singleton(presetElement)).getNewElements().get(0));

        Logo logo = new Logo();
        logo.setType("logo");

        assertEquals(0,
                migrationService.migrate(singleton(logo), singleton(presetElement)).getNewElements().size());
    }

    @Test
    public void testNewSize() {
        PresetItem presetItemA = getPresetItem().withId(1);
        PresetItem presetItemB = getPresetItem().withId(2);
        presetItemB.setWidth(presetItemB.getWidth() + 5);
        Preset preset = new Preset(1, "", "", singleton(COMMON), Arrays.asList(presetItemA, presetItemB));
        CreativeDocumentBatch migrated = getMigrationServiceForPresets().migrate(getBatch(getCreativeData()), preset);

        assertEquals(2, migrated.getItems().size());
        assertEquals(presetItemB.getWidth(), migrated.getItems().get(1).getData().getWidth());
        assertEquals(presetItemB.getHeight(), migrated.getItems().get(1).getData().getHeight());
    }

    @Test
    public void addPlaceholdersFromPreset() {
        Preset preset = presetsService.getList(false).get(0);
        PresetItem presetItem = preset.getItems().get(0);

        Legal elementToAddPlaceholder = new Legal();
        elementToAddPlaceholder.setOptions(new Legal.Options());

        CreativeData creativeData = new CreativeData();
        creativeData.setElements(singletonList(elementToAddPlaceholder));
        creativeData.setWidth(presetItem.getWidth());
        creativeData.setHeight(presetItem.getHeight());

        CreativeDocument creative = new CreativeDocument()
                .withPresetId(preset.getId())
                .withData(creativeData);

        CreativeDocumentBatch batch = new CreativeDocumentBatch()
                .withItems(singletonList(creative));

        migrationService.addPlaceholdersFromPreset(preset, batch);

        String exptected = StreamEx.of(presetItem.getElements())
                .select(Legal.class)
                .map(Legal::getOptions)
                .map(Element.ColoredTextOptions::getPlaceholder)
                .findFirst().orElse(null);
        String result = elementToAddPlaceholder.getOptions().getPlaceholder();
        assertThat(result, is(exptected));
    }
}
