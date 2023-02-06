package ru.yandex.market.mbo.cms.core.dao.model;

import java.util.Arrays;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PropertyLineTest {

    @Test
    public void testNormalString() {
        assertPlaceholderProperty("PLACEHOLDER.property=value1,value2", "PLACEHOLDER",
                "property", "value1", "value2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyString() {
        PropertyLine.fromString("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyPlaceholder() {
        PropertyLine.fromString(".property=value1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyProperty() {
        PropertyLine.fromString("PLACEHOLDER.=value1");
    }

    @Test
    public void testEmptyValues() {
        assertPlaceholderProperty("PLACEHOLDER.property=", "PLACEHOLDER",
                "property");
    }

    @Test
    public void testEscapeLines() {
        assertPlaceholderProperty("A.b=\\,c,d", "A", "b", ",c", "d");
        assertPlaceholderProperty("A.b=c\\,c,d", "A", "b", "c,c", "d");
        assertPlaceholderProperty("A.b=c\\,,d", "A", "b", "c,", "d");
        assertPlaceholderProperty("A.b=c,\\,d", "A", "b", "c", ",d");
        assertPlaceholderProperty("A.b=c,d\\,", "A", "b", "c", "d,");
        assertPlaceholderProperty("A.b=c,\\", "A", "b", "c", "\\");
        assertPlaceholderProperty("A.b=,\\,", "A", "b", ",");
        assertPlaceholderProperty("A.b=\\,,\\,", "A", "b", ",", ",");
        assertPlaceholderProperty("A.b=\\,\\,", "A", "b", ",,");
        assertPlaceholderProperty("A.b=\\\\,", "A", "b", "\\");
        assertPlaceholderProperty("A.b=\\\\\\,", "A", "b", "\\,");
    }

    @Test
    public void testComplexPlaceholder() {
        assertPlaceholderProperty("PLACEHOLDER[SUB_PLACEHOLDER]/SUB_PLACEHOLDER_2.my_property=value1~@^&",
                "PLACEHOLDER[SUB_PLACEHOLDER]/SUB_PLACEHOLDER_2", "my_property", "value1~@^&");
    }

    private void assertPlaceholderProperty(String rawPlaceholderPropertiesLine,
                                           String placeholder, String property, String... values) {
        assertPlaceholderProperty(PropertyLine.fromString(rawPlaceholderPropertiesLine), placeholder, property, values);
    }

    private void assertPlaceholderProperty(PropertyLine propertyLine, String placeholder, String property,
                                           String... values) {
        assertEquals(placeholder, propertyLine.getPath());
        assertEquals(property, propertyLine.getPropertyName());
        Assert.assertThat(propertyLine.getValues(), CoreMatchers.is(Arrays.asList(values)));
    }

    @Test
    public void testAsStringWithoutPath() {
        String propertyLine = "P.p=a\\,b,c\\,d";
        String propertyLineNoPath = "p=a\\,b,c\\,d";
        PropertyLine pl = PropertyLine.fromString(propertyLine);
        assertEquals(2, pl.getValues().size());
        assertEquals("a,b", pl.getValues().get(0));
        assertEquals("c,d", pl.getValues().get(1));

        assertEquals(propertyLineNoPath,
                PropertyLine.asStringWithoutPath(pl.getPropertyName(), pl.getValues()).toString());
    }

    @Test
    public void testParseCmsProperties() {
        String source = "P.p=v" + "\n";
        Assert.assertEquals(source,
                PropertyLine.branchPropertiesToString(PropertyLine.branchPropertiesFromString(source)));

        String source1 = "";
        Assert.assertEquals(null,
                PropertyLine.branchPropertiesToString(PropertyLine.branchPropertiesFromString(source1)));

        String source2 = "\n";
        Assert.assertEquals(null,
                PropertyLine.branchPropertiesToString(PropertyLine.branchPropertiesFromString(source2)));

        String source3 = "  \n \n ";
        Assert.assertEquals(null,
                PropertyLine.branchPropertiesToString(PropertyLine.branchPropertiesFromString(source3)));

        String source4 = "P.p=v\n" +
                "PP.pp=vv\n";
        Assert.assertEquals(source4,
                PropertyLine.branchPropertiesToString(PropertyLine.branchPropertiesFromString(source4)));

        String source5 = "P.p=v\n" +
                "P.pp=vv\n";
        Assert.assertEquals(source5,
                PropertyLine.branchPropertiesToString(PropertyLine.branchPropertiesFromString(source5)));
    }
}
