package ru.yandex.canvas.controllers;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.controllers.video.VideoFilesModifyingController;
import ru.yandex.canvas.model.CreativeData;
import ru.yandex.canvas.model.DraftCreative;
import ru.yandex.canvas.model.IdeaDocument;
import ru.yandex.canvas.model.elements.Element;
import ru.yandex.canvas.model.elements.ElementType;
import ru.yandex.canvas.model.elements.Fade;
import ru.yandex.canvas.model.presets.Preset;
import ru.yandex.canvas.model.presets.PresetSelectionCriteria;
import ru.yandex.canvas.model.stillage.StillageFileInfo;
import ru.yandex.canvas.service.PackshotService;
import ru.yandex.canvas.service.SandBoxService;
import ru.yandex.canvas.service.idea.IdeasService;
import ru.yandex.canvas.service.idea.modifiers.CreativeModifier;
import ru.yandex.canvas.service.idea.modifiers.ElementBackgroundColorModifier;
import ru.yandex.canvas.service.idea.modifiers.ElementColorModifier;
import ru.yandex.canvas.service.idea.modifiers.ElementConditionalModifier;
import ru.yandex.canvas.service.idea.modifiers.ElementDisableModifier;
import ru.yandex.canvas.service.idea.modifiers.RawImageModifier;
import ru.yandex.canvas.service.video.VideoFileUploadServiceInterface;

import static ru.yandex.canvas.model.Util.deepcopy;


@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ElementModifiersTest {
    private static final Set<String> defaultColorElements = ImmutableSet.of(ElementType.FADE, ElementType.LEGAL);

    private DraftCreative creativeDraft;

    private DraftCreative creativeDraft2;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private IdeasService ideasService;

    @Autowired
    SandBoxService sandBoxService;

    @Autowired
    VideoFileUploadServiceInterface videoFileUploadServiceInterface;

    @Autowired
    PackshotService packshotService;

    @Autowired
    VideoFilesModifyingController videoFilesModifyingController;

    @Before
    public void setUp() throws IOException {
        Preset preset = objectMapper.readValue(Resources.getResource("test/testPreset.json"), Preset.class);
        creativeDraft =
                new DraftCreative(1, deepcopy(objectMapper, preset.getItems().get(0), CreativeData.class), 10001);
        creativeDraft2 =
                new DraftCreative(2, deepcopy(objectMapper, preset.getItems().get(1), CreativeData.class), 10002);
    }

    @Test
    public void removeElementTest() {
        new ElementDisableModifier(ElementType.LOGO).modify(creativeDraft);
        Assert.assertTrue("CreativeUploadData element should be not available",
                creativeDraft.getData().getElements().stream()
                        .filter(element -> ElementType.LOGO.equals(element.getType()))
                        .noneMatch(Element::getAvailable));
    }

    @Test
    public void notInConditionRemoveElementTest() {
        CreativeModifier modifier = ElementConditionalModifier
                .apply(new ElementDisableModifier(ElementType.FADE))
                .ifPresetIdNotIn(1);
        Stream.of(creativeDraft, creativeDraft2).forEach(modifier::modify);
        Assert.assertTrue("CreativeUploadData element " + ElementType.FADE + " should not be available on preset",
                creativeDraft.getData().getElements().stream()
                        .filter(element -> ElementType.FADE.equals(element.getType()))
                        .allMatch(Element::getAvailable));
        Assert.assertTrue("CreativeUploadData element " + ElementType.FADE + " should be available on preset",
                creativeDraft2.getData().getElements().stream()
                        .filter(element -> ElementType.FADE.equals(element.getType()))
                        .noneMatch(Element::getAvailable));
    }

    @Test
    public void inConditionRemoveElementTest() {
        CreativeModifier modifier = ElementConditionalModifier
                .apply(new ElementDisableModifier(ElementType.FADE))
                .ifPresetIdIn(1);
        Stream.of(creativeDraft, creativeDraft2).forEach(modifier::modify);
        Assert.assertTrue("CreativeUploadData element " + ElementType.FADE + " should not be available on preset",
                creativeDraft.getData().getElements().stream()
                        .filter(element -> ElementType.FADE.equals(element.getType()))
                        .noneMatch(Element::getAvailable));
        Assert.assertTrue("CreativeUploadData element " + ElementType.FADE + " should be available on preset",
                creativeDraft2.getData().getElements().stream()
                        .filter(element -> ElementType.FADE.equals(element.getType()))
                        .allMatch(Element::getAvailable));
    }

    @Test
    public void modifyImageTest() {
        final String newUrl = "https://www.yandex.ru/superImage";
        StillageFileInfo fileInfo = new StillageFileInfo();
        fileInfo.setUrl(newUrl);
        fileInfo.getMetadataInfo().put("height", 123);
        fileInfo.getMetadataInfo().put("width", 123);


        new RawImageModifier(fileInfo, ElementType.IMAGE).modify(creativeDraft);
        Set<String> affectedMediaSets = creativeDraft.getData().getElements().stream()
                .filter(element -> ElementType.IMAGE.equals(element.getType()))
                .map(Element::getMediaSet)
                .collect(Collectors.toSet());

        Assert.assertTrue("Image element was not properly modified. Expected params:\n" +
                        "url: " + newUrl +
                        "width: 123" +
                        "height: 123," +
                        "But got CreativeData: " + creativeDraft.getData(),
                creativeDraft.getData().getMediaSets().entrySet().stream()
                        .filter(entry -> affectedMediaSets.contains(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .allMatch(mediaSet -> mediaSet.getItems().stream().allMatch(
                                mediaSetItem -> mediaSetItem.getItems().stream().allMatch(
                                        mediaSetSubItem ->
                                                newUrl.equals(mediaSetSubItem.getUrl()) &&
                                                        mediaSetSubItem.getHeight() == 123 &&
                                                        mediaSetSubItem.getWidth() == 123
                                )
                        )));

    }

    @Test
    public void textColorModifierTest() {
        Set<String> elementsToModify = ImmutableSet.of(
                ElementType.BUTTON,
                ElementType.DESCRIPTION,
                ElementType.SPECIAL,
                ElementType.DISCLAIMER,
                ElementType.AGE_RESTRICTION,
                ElementType.HEADLINE,
                ElementType.LEGAL,
                ElementType.DOMAIN,
                ElementType.FADE
        );
        final String newColor = "#DEADBE";
        new ElementColorModifier(elementsToModify, newColor).modify(creativeDraft);
        creativeDraft.getData().getElements().stream()
                .filter(element -> elementsToModify.contains(element.getType()))
                .forEach(element -> assertColor(
                        "Element " + element.getType() + " should have " + newColor + " color but have " + getColor(
                                element.getOptions()),
                        newColor, getColor(element.getOptions())));
    }

    @Test
    public void backgroundColorModifierTest() {
        Set<String> elementsToModify = ImmutableSet.of(
                ElementType.BUTTON,
                ElementType.SPECIAL
        );
        final String newColor = "#DEADBE";
        new ElementBackgroundColorModifier(elementsToModify, newColor).modify(creativeDraft);
        creativeDraft.getData().getElements().stream()
                .filter(element -> elementsToModify.contains(element.getType()))
                .forEach(element -> assertColor(
                        "Element " + element.getType() + " should have " + newColor + " color but have "
                                + getBackgroundColor(element.getOptions()),
                        newColor, getBackgroundColor(element.getOptions())));
    }

    @Test
    public void integralCreativeModifyingTest() throws IOException {
        IdeaDocument ideaDocument = objectMapper.readValue(Resources.getResource("test/idea.json"), IdeaDocument.class);
        PresetSelectionCriteria selectionCriteria =
                PresetSelectionCriteria.builder().withCpmCommon(true).withCpmGeoproduct(false).build();
        List<DraftCreative> results = ideasService.previewDraftCreatives(ideaDocument, selectionCriteria);
        for (DraftCreative result : results) {
            checkDraftCreative(result);
        }
    }

    private void checkDraftCreative(DraftCreative draftCreative) throws JsonProcessingException {
        assertColorNotDefault("creative has default background color",
                draftCreative.getData().getOptions().getBackgroundColor());
        assertColorNotDefault("creative has default border color",
                draftCreative.getData().getOptions().getBorderColor());
        draftCreative.getData().getElements().stream()
                .filter(element -> !defaultColorElements.contains(element.getType())) //filter fade, legal, etc.
                .forEach(element -> {
                    if (hasColor(element.getOptions())) {
                        assertColorNotDefault("element " + element.getType() + " has element with default color",
                                getColor(element.getOptions()));
                    }
                    if (hasBackgroundColor(element.getOptions())) {
                        assertColorNotDefault(
                                "element " + element.getType() + " has element with default background color",
                                getBackgroundColor(element.getOptions()));
                    }
                });
    }

    private void assertColor(String msg, String expectedColor, String color) {
        Assert.assertEquals(msg, expectedColor, color);
    }

    private void assertColorNotDefault(String msg, String color) {
        Assert.assertNotNull(msg, color);
        Assert.assertNotEquals(msg, "#000000", color);
        Assert.assertNotEquals(msg, "#FFFFFF", color.toUpperCase());
    }

    @Nullable
    private String getColor(Element.Options options) {
        if (options instanceof Element.ColoredTextOptions) {
            Element.ColoredTextOptions coloredTextOptions = (Element.ColoredTextOptions) options;
            return coloredTextOptions.getColor();
        }

        if (options instanceof Fade.Options) {
            Fade.Options fadeOptions = (Fade.Options) options;
            return fadeOptions.getColor();
        }
        return null;
    }

    @Nullable
    private String getBackgroundColor(Element.Options options) {
        if (options instanceof Element.ColoredTextOptionsWithBackground) {
            Element.ColoredTextOptionsWithBackground coloredTextOptions =
                    (Element.ColoredTextOptionsWithBackground) options;
            return coloredTextOptions.getBackgroundColor();
        }
        return null;
    }

    private boolean hasColor(Element.Options options) {
        return options instanceof Element.ColoredTextOptions || options instanceof Fade.Options;
    }

    private boolean hasBackgroundColor(Element.Options options) {
        return options instanceof Element.ColoredTextOptionsWithBackground;
    }
}
