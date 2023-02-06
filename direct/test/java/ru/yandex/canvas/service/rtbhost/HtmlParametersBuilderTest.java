package ru.yandex.canvas.service.rtbhost;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.Bundle;
import ru.yandex.canvas.model.CreativeData;
import ru.yandex.canvas.model.MediaSet;
import ru.yandex.canvas.model.MediaSetItem;
import ru.yandex.canvas.model.MediaSetSubItem;
import ru.yandex.canvas.model.SmartCenter;
import ru.yandex.canvas.model.elements.Button;
import ru.yandex.canvas.model.elements.Element;
import ru.yandex.canvas.model.presets.PresetItem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.canvas.model.elements.ElementType.BUTTON;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class HtmlParametersBuilderTest {
    private static final String BACKGROUND_COLOR = "#ABABAB";
    private static final String BORDER_COLOR = "#FFFFFF";
    private static final String COLOR = "#000000";
    private static final String BUNDLE_NAME = "bundle";
    private static final String CONTENT = "content";
    private static final int BUNDLE_VERSION = 1234;
    private static final String CLICK_URL = "https://ya.ru";
    private static final String MEDIA_SET = "media_set";
    private static final String PLACEHOLDER = "placeholder";
    private static final int WIDTH = 100;
    private static final int HEIGHT = 200;
    private static final int X = 100;
    private static final int Y = 200;
    private static final String MEDIA_SET_ITEM_TYPE = "image";
    private static final String FILE_ID = "file_id";
    private static final String MEDIA_SET_NAME = "media_set";
    private static final String URL = "http://site.com";
    private static final String ALIAS = "alias";

    @Test
    public void emptyCreativeDataTest() {
        HtmlParametersBuilder builder = new HtmlParametersBuilder();
        builder.setCreativeData(new CreativeData());

        var proto = builder.build();

        assertTrue(proto.hasData());
        assertFalse(proto.hasPreset());

        var data = proto.getData();
        assertFalse(data.hasSize());
        assertFalse(data.hasLogoSize());
        assertFalse(data.hasOptions());
        assertEquals(0, data.getElementsCount());
        assertEquals(0, data.getMediaSetsCount());
        assertFalse(data.hasBundle());
        assertFalse(data.hasClickUrl());
    }

    @Test
    public void htmlParametersWithCreativeDataTest() {
        var creativeData = new CreativeData();

        creativeData.setWidth(WIDTH);
        creativeData.setHeight(HEIGHT);
        creativeData.setLogoWidth(WIDTH);
        creativeData.setLogoHeight(HEIGHT);
        creativeData.setOptions(createOptions());
        creativeData.setElements(createElements());
        creativeData.setMediaSets(createMediaSets());
        creativeData.setBundle(createBundle());
        creativeData.setClickUrl(CLICK_URL);

        HtmlParametersBuilder builder = new HtmlParametersBuilder();
        builder.setCreativeData(creativeData);

        var proto = builder.build();
        assertTrue(proto.hasData());
        assertFalse(proto.hasPreset());

        var data = proto.getData();
        assertEquals(WIDTH, data.getSize().getWidth());
        assertEquals(HEIGHT, data.getSize().getHeight());

        assertEquals(WIDTH, data.getLogoSize().getWidth());
        assertEquals(HEIGHT, data.getLogoSize().getHeight());

        assertEquals(BACKGROUND_COLOR, data.getOptions().getBackgroundColor());
        assertEquals(BORDER_COLOR, data.getOptions().getBorderColor());
        assertFalse(data.getOptions().getHasAnimation());
        assertTrue(data.getOptions().getHasSocialLabel());
        assertFalse(data.getOptions().getIsAdaptive());
        assertEquals(WIDTH, data.getOptions().getMinSize().getWidth());
        assertEquals(HEIGHT, data.getOptions().getMinSize().getHeight());

        assertEquals(1, data.getElementsCount());
        var element = data.getElements(0);
        assertEquals(BUTTON, element.getType());
        assertEquals(MEDIA_SET, element.getMediaSet());
        assertEquals(CONTENT, element.getOptions().getContent());
        assertEquals(PLACEHOLDER, element.getOptions().getPlaceholder());
        assertEquals(BACKGROUND_COLOR, element.getOptions().getBackgroundColor());
        assertEquals(COLOR, element.getOptions().getColor());
        assertFalse(element.getOptions().hasSize());
        assertFalse(element.getOptions().hasIconColor());
        assertEquals(0, element.getOptions().getCustomOptionsCount());
        assertTrue(element.getIsAvailable());

        assertEquals(1, data.getMediaSetsCount());
        var mediaSet = data.getMediaSets(0);
        assertEquals(MEDIA_SET_NAME, mediaSet.getName());
        assertEquals(1, mediaSet.getItemsCount());
        assertEquals(MEDIA_SET_ITEM_TYPE, mediaSet.getItems(0).getType());
        assertEquals(1, mediaSet.getItems(0).getItemsCount());
        assertEquals(FILE_ID, mediaSet.getItems(0).getItems(0).getFileId());
        assertEquals(URL, mediaSet.getItems(0).getItems(0).getUrl());
        assertEquals(ALIAS, mediaSet.getItems(0).getItems(0).getAlias());
        assertEquals(1, mediaSet.getItems(0).getItems(0).getSmartCentersCount());
        var smartCenter = mediaSet.getItems(0).getItems(0).getSmartCenters(0);
        assertEquals(WIDTH, smartCenter.getW());
        assertEquals(HEIGHT, smartCenter.getH());
        assertEquals(X, smartCenter.getX());
        assertEquals(Y, smartCenter.getY());

        assertEquals(BUNDLE_NAME, data.getBundle().getName());
        assertEquals(BUNDLE_VERSION, data.getBundle().getVersion());

        assertEquals(CLICK_URL, data.getClickUrl());
    }

    @Test
    public void htmlParametersWithPresetTest() {
        PresetItem presetItem = new PresetItem();
        presetItem.setWidth(WIDTH);
        presetItem.setHeight(HEIGHT);
        presetItem.setLogoWidth(WIDTH);
        presetItem.setLogoHeight(HEIGHT);
        presetItem.setOptions(createOptions());
        presetItem.setElements(createElements());
        presetItem.setMediaSets(createMediaSets());
        presetItem.setBundle(createBundle());

        HtmlParametersBuilder builder = new HtmlParametersBuilder();
        builder.setPresetItem(presetItem);

        var proto = builder.build();
        assertFalse(proto.hasData());
        assertTrue(proto.hasPreset());

        var data = proto.getPreset().getData();
        assertFalse(data.hasSize());
        assertFalse(data.hasLogoSize());
        assertFalse(data.hasOptions());
        assertEquals(1, data.getElementsCount());
        assertEquals(0, data.getMediaSetsCount());
        assertFalse(data.hasBundle());
    }

    private static Bundle createBundle() {
        Bundle bundle = new Bundle();
        bundle.setName(BUNDLE_NAME);
        bundle.setVersion(BUNDLE_VERSION);
        return bundle;
    }

    private static CreativeData.Options createOptions() {
        CreativeData.Options options = new CreativeData.Options();
        options.setBackgroundColor(BACKGROUND_COLOR);
        options.setHasAnimation(false);
        options.setHasSocialLabel(true);
        options.setBorderColor(BORDER_COLOR);
        options.setMinHeight(HEIGHT);
        options.setMinWidth(WIDTH);
        return options;
    }

    private static List<Element> createElements() {
        var button = new Button();
        button.setType(BUTTON);
        button.setAvailable(true);
        button.setMediaSet(MEDIA_SET);

        var buttonOptions = new Button.Options();
        buttonOptions.setBackgroundColor(BACKGROUND_COLOR);
        buttonOptions.setColor(COLOR);
        buttonOptions.setContent(CONTENT);
        buttonOptions.setPlaceholder(PLACEHOLDER);

        button.setOptions(buttonOptions);

        return Collections.singletonList(button);
    }

    private Map<String, MediaSet> createMediaSets() {
        var smartCenter = new SmartCenter();
        smartCenter.setW(WIDTH);
        smartCenter.setH(HEIGHT);
        smartCenter.setX(X);
        smartCenter.setY(Y);

        var mediaSetSubItem = new MediaSetSubItem();
        mediaSetSubItem.setWidth(WIDTH);
        mediaSetSubItem.setHeight(HEIGHT);
        mediaSetSubItem.setFileId(FILE_ID);
        mediaSetSubItem.setUrl(URL);
        mediaSetSubItem.setAlias(ALIAS);
        mediaSetSubItem.setSmartCenters(Collections.singletonList(smartCenter));

        var mediaSetItem = new MediaSetItem();
        mediaSetItem.setType(MEDIA_SET_ITEM_TYPE);
        mediaSetItem.setItems(Collections.singletonList(mediaSetSubItem));

        final MediaSet mediaSet = new MediaSet();
        mediaSet.setItems(Collections.singletonList(mediaSetItem));

        return Collections.singletonMap(MEDIA_SET_NAME, mediaSet);
    }
}
