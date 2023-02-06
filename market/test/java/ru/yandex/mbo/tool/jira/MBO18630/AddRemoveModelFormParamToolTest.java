package ru.yandex.mbo.tool.jira.MBO18630;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.server.utils.ModelFormConverter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Ignore
public class AddRemoveModelFormParamToolTest {

    private String sourceFormXml;
    private String removedParamFormXml;
    private String addedParamFormXml;

    @Before
    public void setup() throws IOException, URISyntaxException {
        File file1 = Paths.get(getClass().getClassLoader().getResource("MBO18630/source-form.xml").toURI()).toFile();
        sourceFormXml = FileUtils.readFileToString(file1, StandardCharsets.UTF_8);

        File file2 =
            Paths.get(getClass().getClassLoader().getResource("MBO18630/removed-param-form.xml").toURI()).toFile();
        removedParamFormXml = FileUtils.readFileToString(file2, StandardCharsets.UTF_8);

        File file3 =
            Paths.get(getClass().getClassLoader().getResource("MBO18630/added-param-form.xml").toURI()).toFile();
        addedParamFormXml = FileUtils.readFileToString(file3, StandardCharsets.UTF_8);
    }

    @Test
    public void testDoubleConversionIsIdempotent() {
        assertXmlEquals(sourceFormXml, convert(convert(sourceFormXml)));
    }

    @Test
    public void testNonExistingTabSkipped() {
        ModelForm form = convert(sourceFormXml);
        String tab = "Парламенты миледи"; // nonexistent tab
        String block = "Общие характеристики";
        String param = "Unbearable lightness of being";

        boolean add = false;
        assertFalse(AddRemoveModelFormParamTool.processModelForm(form, tab, block, param, add));
        assertXmlEquals(sourceFormXml, convert(form));

        add = true;
        assertFalse(AddRemoveModelFormParamTool.processModelForm(form, tab, block, param, add));
        assertXmlEquals(sourceFormXml, convert(form));
    }

    @Test
    public void testNonExistingBlockSkipped() {
        ModelForm form = convert(sourceFormXml);
        String tab = "Параметры модели";
        String block = "Обещие харакиристики"; // nonexistent block
        String param = "Unbearable lightness of being";

        boolean add = false;
        assertFalse(AddRemoveModelFormParamTool.processModelForm(form, tab, block, param, add));
        assertXmlEquals(sourceFormXml, convert(form));

        add = true;
        assertFalse(AddRemoveModelFormParamTool.processModelForm(form, tab, block, param, add));
        assertXmlEquals(sourceFormXml, convert(form));
    }

    @Test
    public void testNonExistingParamNotRemoved() {
        ModelForm form = convert(sourceFormXml);
        String tab = "Параметры модели";
        String block = "Общие характеристики";
        String param = "Unbearable likeness of bean"; // nonexistent param

        boolean add = false;
        assertFalse(AddRemoveModelFormParamTool.processModelForm(form, tab, block, param, add));
        assertXmlEquals(sourceFormXml, convert(form));
    }

    @Test
    public void testExistingParamNotAdded() {
        ModelForm form = convert(sourceFormXml);
        String tab = "Параметры модели";
        String block = "Общие характеристики";
        String param = "Unbearable lightness of being"; // param exists!

        boolean add = true;
        assertFalse(AddRemoveModelFormParamTool.processModelForm(form, tab, block, param, add));
        assertXmlEquals(sourceFormXml, convert(form));
    }

    @Test
    public void testExistentParamRemoved() {
        ModelForm form = convert(sourceFormXml);
        String tab = "Параметры модели";
        String block = "Общие характеристики";
        String param = "Unbearable lightness of being"; // param exists!

        boolean add = false;
        assertTrue(AddRemoveModelFormParamTool.processModelForm(form, tab, block, param, add));
        assertXmlEquals(removedParamFormXml, convert(form));
    }

    @Test
    public void testNonExistentParamAdded() {
        ModelForm form = convert(sourceFormXml);
        String tab = "Параметры модели";
        String block = "Общие характеристики";
        String param = "Glokaya kuzdra"; // nonexistent param

        boolean add = true;
        assertTrue(AddRemoveModelFormParamTool.processModelForm(form, tab, block, param, add));
        assertXmlEquals(addedParamFormXml, convert(form));
    }

    private static String convert(ModelForm form) {
        return ModelFormConverter.toXml(form);
    }

    private static ModelForm convert(String form) {
        return ModelFormConverter.fromXml(form);
    }

    private static void assertXmlEquals(String expected, String actual) {
        expected = Arrays.stream(expected.split("\n")).map(String::trim).collect(Collectors.joining("\n"));
        actual = Arrays.stream(actual.split("\n")).map(String::trim).collect(Collectors.joining("\n"));
        assertEquals(expected, actual);
    }
}
