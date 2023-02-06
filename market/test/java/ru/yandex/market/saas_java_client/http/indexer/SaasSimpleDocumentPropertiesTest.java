package ru.yandex.market.saas_java_client.http.indexer;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.saas_java_client.http.common.SaasAttr;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.DoubleKind;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.IntKind;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.IsProperty;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.NoGroup;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.NotSearchable;
import ru.yandex.market.saas_java_client.http.common.SaasAttr.StringKind;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("checkstyle:MagicNumber")
public class SaasSimpleDocumentPropertiesTest {
    private static final String INT_FIELD_NAME = "i_field";
    private static final String STR_FIELD_NAME = "s_field";
    private static final String DOUBLE_FIELD_NAME = "d_field";

    private static final String TEST_ID = "id";

    private static final SaasAttr<IntKind, NotSearchable, NoGroup, IsProperty> INT_FIELD
            = SaasAttr.intAttr(INT_FIELD_NAME).property();

    private static final SaasAttr<StringKind, NotSearchable, NoGroup, IsProperty> STR_FIELD
            = SaasAttr.stringAttr(STR_FIELD_NAME).property();

    private static final SaasAttr<DoubleKind, NotSearchable, NoGroup, IsProperty> DOUBLE_FIELD
            = SaasAttr.doubleAttr(DOUBLE_FIELD_NAME).property();

    private SaasDocument document;

    @Before
    public void createEmptyDocument() {
        document = new SaasSimpleDocument(TEST_ID);
    }

    @Test
    public void testIdProperty() {
        assertEquals(TEST_ID, document.getId());
    }

    @Test
    public void testIntPropertyByAttr() {
        final int testValue = 2;

        document.setProperty(INT_FIELD, testValue);
        SaasDocumentProperty property = (SaasDocumentProperty) document.getProperty(INT_FIELD_NAME);

        assertEquals(testValue, property.getValue());
    }

    @Test
    public void testIntPropertyByName() {
        final int testValue = 2;

        document.setProperty(INT_FIELD_NAME, new SaasDocumentProperty(testValue));
        SaasDocumentProperty property = (SaasDocumentProperty) document.getProperty(INT_FIELD_NAME);

        assertEquals(testValue, property.getValue());
    }

    @Test
    public void testStringPropertyByAttr() {
        final String testValue = "test value";

        document.setProperty(STR_FIELD, testValue);
        SaasDocumentProperty property = (SaasDocumentProperty) document.getProperty(STR_FIELD_NAME);

        assertEquals(testValue, property.getValue());
    }

    @Test
    public void testStringPropertyByName() {
        final String testValue = "test value";

        document.setProperty(STR_FIELD_NAME, new SaasDocumentProperty(testValue));
        SaasDocumentProperty property = (SaasDocumentProperty) document.getProperty(STR_FIELD_NAME);

        assertEquals(testValue, property.getValue());
    }

    @Test
    public void testDoublePropertyByAttr() {
        final double testValue = 123.456;

        document.setProperty(DOUBLE_FIELD, testValue);
        SaasDocumentProperty property = (SaasDocumentProperty) document.getProperty(DOUBLE_FIELD_NAME);

        assertEquals(testValue, property.getValue());
    }

    @Test
    public void testDoublePropertyByName() {
        final double testValue = 123.456;

        document.setProperty(DOUBLE_FIELD_NAME, new SaasDocumentProperty(testValue));
        SaasDocumentProperty property = (SaasDocumentProperty) document.getProperty(DOUBLE_FIELD_NAME);

        assertEquals(testValue, property.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPropertyIntList() {
        Integer[] testArray = new Integer[] {0, -1, 1, 10, 200};

        document.setPropertyIntList(INT_FIELD, Arrays.asList(testArray));

        List<SaasDocumentProperty> actualProperties = (List<SaasDocumentProperty>) document.getProperty(
                INT_FIELD_NAME);
        Integer[] actualValues  = actualProperties.stream()
                .map(property -> (Integer) property.getValue())
                .toArray(Integer[]::new);

        assertArrayEquals(testArray, actualValues);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPropertyIntListFromFactoryMethod() {
        Integer[] testArray = new Integer[] {0, -1, 1, 10, 200};
        Set<Integer> testSet = Arrays.stream(testArray)
                .collect(Collectors.toSet());

        List<SaasDocumentProperty> documentProperties = SaasDocumentProperty.buildPropertiesList(testSet);
        document.setProperty(INT_FIELD_NAME, documentProperties);

        List<SaasDocumentProperty> actualProperties = (List<SaasDocumentProperty>) document.getProperty(
                INT_FIELD_NAME);
        Integer[] actualSortedValues  = actualProperties.stream()
                .map(property -> (Integer) property.getValue())
                .sorted() //sort to compare, since Set rearranges elements
                .toArray(Integer[]::new);

        Integer[] expectedSortedValues = Arrays.stream(testArray)
                .sorted()
                .toArray(Integer[]::new);
        assertArrayEquals(expectedSortedValues, actualSortedValues);
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testPropertyStringList() {
        String[] testArray = new String[] {"some", "string", "test"};

        document.setPropertyStringList(STR_FIELD, Arrays.asList(testArray));

        List<SaasDocumentProperty> actualProperties = (List<SaasDocumentProperty>) document.getProperty(
                STR_FIELD_NAME);
        String[] actualValues  = actualProperties.stream()
                .map(property -> (String) property.getValue())
                .toArray(String[]::new);

        assertArrayEquals(testArray, actualValues);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPropertyDoubleList() {
        Double[] testArray = new Double[] {0.0, -1.1, 22.2, 33.33};

        document.setPropertyDoubleList(DOUBLE_FIELD, Arrays.asList(testArray));

        List<SaasDocumentProperty> actualProperties = (List<SaasDocumentProperty>) document.getProperty(
                DOUBLE_FIELD_NAME);
        Double[] actualValues  = actualProperties.stream()
                .map(property -> (Double) property.getValue())
                .toArray(Double[]::new);

        assertArrayEquals(testArray, actualValues);
    }
}
