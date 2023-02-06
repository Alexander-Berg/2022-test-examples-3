package ru.yandex.market.mbo.gwt.client.pages.model;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 29.03.2017
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ImageParamTypeTest {
    @Test
    public void testDependentParamXslNamesXLPicture() throws Exception {
        List<String> names = ImageParamType.dependentParamXslNames("XL-Picture");
        assertNotNull(names);
        assertEquals(8, names.size());
        assertTrue(names.contains("XL-Picture_mdata"));
        assertTrue(names.contains("XLPictureSizeX"));
        assertTrue(names.contains("XLPictureSizeY"));
        assertTrue(names.contains("XLPictureOrig"));
        assertTrue(names.contains("XLPictureOrig_mdata"));
        assertTrue(names.contains("XLPictureUrl"));
        assertTrue(names.contains("XLPictureColorness"));
        assertTrue(names.contains("XLPictureColornessAvg"));
    }

    @Test
    public void testDependentParamXslNamesXLPictureN() throws Exception {
        List<String> names = ImageParamType.dependentParamXslNames("XL-Picture_7");
        assertNotNull(names);
        assertEquals(8, names.size());
        assertTrue(names.contains("XL-Picture_7_mdata"));
        assertTrue(names.contains("XLPictureSizeX_7"));
        assertTrue(names.contains("XLPictureSizeY_7"));
        assertTrue(names.contains("XLPictureOrig_7"));
        assertTrue(names.contains("XLPictureOrig_7_mdata"));
        assertTrue(names.contains("XLPictureUrl_7"));
        assertTrue(names.contains("XLPictureColorness_7"));
        assertTrue(names.contains("XLPictureColornessAvg_7"));
    }

}
