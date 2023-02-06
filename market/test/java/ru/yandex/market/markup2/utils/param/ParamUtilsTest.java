package ru.yandex.market.markup2.utils.param;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author inenakhov
 */
public class ParamUtilsTest {
    @Test
    public void notMainPicturePatternTest() throws Exception {
        ArrayList<String> validStrings = Lists.newArrayList("XL-Picture_3", "XL-Picture_31", "XL-Picture_2");
        ArrayList<String> notValidStrings = Lists.newArrayList("XL-Picture", "XL-Picture_2_mdata",
                                                               "XLPictureSizeX", "XL-Picture_21_mdata");

        Pattern p = ParamUtils.NOT_MAIN_PICTURES_PATTERN;
        validStrings.forEach(str -> assertTrue(p.matcher(str).find()));
        notValidStrings.forEach(str -> assertFalse(p.matcher(str).find()));
    }

    @Test
    public void mainPicturePatternTest() throws Exception {
        ArrayList<String> validStrings = Lists.newArrayList("XL-Picture", "XL-Picture_3", "XL-Picture_31",
                                                             "XL-Picture_2");
        ArrayList<String> notValidStrings = Lists.newArrayList("XL-Picture_2_mdata",
                                                               "XLPictureSizeX", "XL-Picture_21_mdata");

        Pattern p = ParamUtils.XL_PICTURE_PATTERN;
        validStrings.forEach(str -> assertTrue(p.matcher(str).matches()));
        notValidStrings.forEach(str -> assertFalse(p.matcher(str).matches()));
    }
}
