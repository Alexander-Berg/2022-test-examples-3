package ru.yandex.canvas.model.elements;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ElementOptionsToProtoTest {
    private static final String COLOR = "#FFFFFF";
    private static final String BACKGROUND_COLOR = "#ABABAB";
    private static final String ICON_COLOR = "#000000";
    private static final String CONTENT = "content";
    private static final String PLACEHOLDER = "placeholder";
    private static final String CUSTOM_PROPERTY_NAME = "custom_property_name";
    private static final String CUSTOM_PROPERTY_VALUE = "custom_property_value";
    private static final int WIDTH = 100;
    private static final int HEIGHT = 200;

    @Test
    public void ageRestrictionTest() {
        var options = new AgeRestriction.Options();
        options.setColor(COLOR);
        options.setContent(CONTENT);
        options.setPlaceholder(PLACEHOLDER);

        var proto = options.toProto();
        assertEquals(PLACEHOLDER, proto.getPlaceholder());
        assertEquals(CONTENT, proto.getContent());
        assertEquals(COLOR, proto.getColor());
        assertFalse(proto.hasIconColor());
        assertFalse(proto.hasBackgroundColor());
        assertFalse(proto.hasSize());
        assertEquals(0, proto.getCustomOptionsCount());
    }

    @Test
    public void buttonTest() {
        var options = new Button.Options();
        options.setColor(COLOR);
        options.setBackgroundColor(BACKGROUND_COLOR);
        options.setContent(CONTENT);
        options.setPlaceholder(PLACEHOLDER);

        var proto = options.toProto();
        assertEquals(PLACEHOLDER, proto.getPlaceholder());
        assertEquals(CONTENT, proto.getContent());
        assertEquals(COLOR, proto.getColor());
        assertFalse(proto.hasIconColor());
        assertEquals(BACKGROUND_COLOR, proto.getBackgroundColor());
        assertFalse(proto.hasSize());
        assertEquals(0, proto.getCustomOptionsCount());
    }

    @Test
    public void disclaimerWithAdditionalPropertiesTest() {
        var options = new Disclaimer.Options();
        options.setColor(COLOR);
        options.setContent(CONTENT);
        options.setPlaceholder(PLACEHOLDER);
        options.setAdditionalProperties(CUSTOM_PROPERTY_NAME, CUSTOM_PROPERTY_VALUE);

        var proto = options.toProto();
        assertEquals(PLACEHOLDER, proto.getPlaceholder());
        assertEquals(CONTENT, proto.getContent());
        assertEquals(COLOR, proto.getColor());
        assertFalse(proto.hasIconColor());
        assertFalse(proto.hasBackgroundColor());
        assertFalse(proto.hasSize());
        assertEquals(1, proto.getCustomOptionsCount());
        assertEquals(CUSTOM_PROPERTY_NAME, proto.getCustomOptions(0).getName());
        assertEquals(CUSTOM_PROPERTY_VALUE, proto.getCustomOptions(0).getValue());
    }

    @Test
    public void imageTest() {
        var options = new Image.Options();
        options.setWidth(WIDTH);
        options.setHeight(HEIGHT);

        var proto = options.toProto();
        assertFalse(proto.hasPlaceholder());
        assertFalse(proto.hasContent());
        assertFalse(proto.hasColor());
        assertFalse(proto.hasIconColor());
        assertFalse(proto.hasBackgroundColor());
        assertEquals(WIDTH, proto.getSize().getWidth());
        assertEquals(HEIGHT, proto.getSize().getHeight());
        assertEquals(0, proto.getCustomOptionsCount());
    }

    @Test
    public void legalTest() {
        var options = new Legal.Options();
        options.setColor(COLOR);
        options.setContent(CONTENT);
        options.setPlaceholder(PLACEHOLDER);
        options.setIconColor(ICON_COLOR);

        var proto = options.toProto();
        assertEquals(PLACEHOLDER, proto.getPlaceholder());
        assertEquals(CONTENT, proto.getContent());
        assertEquals(COLOR, proto.getColor());
        assertEquals(ICON_COLOR, proto.getIconColor());
        assertFalse(proto.hasBackgroundColor());
        assertFalse(proto.hasSize());
        assertEquals(0, proto.getCustomOptionsCount());
    }
}
