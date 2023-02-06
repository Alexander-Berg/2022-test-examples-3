package ru.yandex.market.mbo.gwt.utils;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnitedSizeTemplateUtilTest {

    @Test
    public void testTemplateParse() {
        List<String> expectedResult = Stream.of(
            "abc", "def", "ghi"
        ).collect(Collectors.toList());

        List<String> result = UnitedSizeTemplateUtil.parseTemplateParameters("{abc}/{def}/{ghi}");

        assertEquals(expectedResult, result);
    }

    @Test
    public void testTemplateParseWithUniqueSymbol() {
        List<String> expectedResult = Stream.of(
            "abc", "def", "ghi"
        ).collect(Collectors.toList());

        List<String> result = UnitedSizeTemplateUtil.parseTemplateParameters("{abc}/{def}/{ghi}^");

        assertEquals(expectedResult, result);
    }

    @Test
    public void testIsValidTemplate() {
        assertTrue(UnitedSizeTemplateUtil.isValidTemplate("{abc}/{def}/{ghi}"));
    }

    @Test
    public void testIsNotValidTemplate() {
        assertFalse(UnitedSizeTemplateUtil.isValidTemplate("{abc/{def}/{ghi}"));
    }

    @Test
    public void testIsNotValidTemplateByStart() {
        assertFalse(UnitedSizeTemplateUtil.isValidTemplate("a{def}/{ghi}"));
    }

    @Test
    public void testIsNotValidTemplateByEnd() {
        assertFalse(UnitedSizeTemplateUtil.isValidTemplate("{def}/{ghi}a"));
    }

    @Test
    public void testIsNotValidTemplateByNestedBrackets() {
        assertFalse(UnitedSizeTemplateUtil.isValidTemplate("{d{e}f}/{ghi}"));
    }

    @Test
    public void testFilTemplateWithoutDuplicates() {
        final String expectedString = "abc/cde";
        final Map<String, String> data = new HashMap<>();
        data.put("abc_p", "abc");
        data.put("cde_p", "cde");

        final String template = "{abc_p}/{cde_p}";

        String result = UnitedSizeTemplateUtil.fillTemplate(template, data);

        assertEquals(expectedString, result);
    }

    @Test
    public void testFilTemplateWithDuplicates() {
        final String expectedString = "abc/abc/cde";
        final Map<String, String> data = new HashMap<>();
        data.put("abc_p", "abc");
        data.put("abcd_p", "abc");
        data.put("cde_p", "cde");

        final String template = "{abc_p}/{abcd_p}/{cde_p}";

        String result = UnitedSizeTemplateUtil.fillTemplate(template, data);

        assertEquals(expectedString, result);
    }

    @Test
    public void testFilTemplateWithDuplicatesSymbol() {
        final String expectedString = "abc/cde";
        final Map<String, String> data = new HashMap<>();
        data.put("abc_p", "abc");
        data.put("abcd_p", "abc");
        data.put("cde_p", "cde");

        final String template = "{abc_p}/{abcd_p}/{cde_p}^";

        String result = UnitedSizeTemplateUtil.fillTemplate(template, data);

        assertEquals(expectedString, result);
    }

    @Test
    public void testFilTemplateWithoutDuplicatesWithSymbol() {
        final String expectedString = "abc/cde";
        final Map<String, String> data = new HashMap<>();
        data.put("abc_p", "abc");
        data.put("cde_p", "cde");

        final String template = "{abc_p}/{cde_p}^";

        String result = UnitedSizeTemplateUtil.fillTemplate(template, data);

        assertEquals(expectedString, result);
    }

    @Test
    public void testFilTemplateWithDelimitersAndDuplicates() {
        final String expectedString = "a/c";
        final Map<String, String> data = new HashMap<>();
        data.put("a_p", "a");
        data.put("c_p", "c");

        final String template = "{a_p}/{a_p}/{c_p}^";

        String result = UnitedSizeTemplateUtil.fillTemplate(template, data);

        assertEquals(expectedString, result);
    }

    @Test
    public void testFilTemplateWithoutDelimitersAndDuplicates() {
        final String expectedString = "a/c";
        final Map<String, String> data = new HashMap<>();
        data.put("a_p", "a");
        data.put("c_p", "c");

        final String template = "{a_p}{a_p}/{c_p}^";

        String result = UnitedSizeTemplateUtil.fillTemplate(template, data);

        assertEquals(expectedString, result);
    }

    @Test
    public void testFilTemplateIfFirstParameterNotInDataMap() {
        final String expectedString = "cde";
        final Map<String, String> data = new HashMap<>();
        data.put("cde_p", "cde");

        final String template = "{abc_p}/{cde_p}^";

        String result = UnitedSizeTemplateUtil.fillTemplate(template, data);

        assertEquals(expectedString, result);
    }

    @Test
    public void testFillTemplateIfMiddleParameterNotInDataMap() {
        final Map<String, String> data = new HashMap<>();
        data.put("abc_p", "abc");
        data.put("cde_p", "cde");

        final String template = "{abc_p}/{abacaba_p}/{cde_p}";
        final String expected = "abc/cde";

        String result = UnitedSizeTemplateUtil.fillTemplate(template, data);

        assertEquals(expected, result);
    }

    @Test
    public void testFillTemplateIfLastParameterNotInDataMap() {
        final Map<String, String> data = new HashMap<>();
        data.put("abc_p", "abc");
        data.put("cde_p", "cde");

        final String template = "{abc_p}/{cde_p}/{abacaba_p}";
        final String expected = "abc/cde";

        String result = UnitedSizeTemplateUtil.fillTemplate(template, data);

        assertEquals(expected, result);
    }

}
