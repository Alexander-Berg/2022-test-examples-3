package ru.yandex.market.mbo.gwt.models.params;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SubTypeTest {

    @Test
    public void testEnumMethods() {
        assertEquals(SubType.fromId("NOT_DEFINED"),     SubType.NOT_DEFINED);
        assertEquals(SubType.fromId("RANGE"),           SubType.RANGE);
        assertEquals(SubType.fromId("IMAGE_PICKER"),    SubType.IMAGE_PICKER);
        assertEquals(SubType.fromId("COLOR"),           SubType.COLOR);
        assertEquals(SubType.fromId("SIZE"),            SubType.SIZE);
        assertEquals(SubType.fromId("MATERIAL"),        SubType.MATERIAL);
        assertEquals(SubType.fromId("SHORT"),           SubType.SHORT);
        assertEquals(SubType.fromId(null),              SubType.NOT_DEFINED);

        assertEquals(SubType.NOT_DEFINED.getKey(), "not_defined");
        assertEquals(SubType.RANGE.getKey(), "range");
        assertEquals(SubType.IMAGE_PICKER.getKey(), "image_picker");
        assertEquals(SubType.COLOR.getKey(), "color");
        assertEquals(SubType.SIZE.getKey(), "size");
        assertEquals(SubType.MATERIAL.getKey(), "material");
        assertEquals(SubType.SHORT.getKey(), "short");
    }

}
