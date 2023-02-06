package ru.yandex.market.yt;

import org.junit.Test;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.yt.YtTableUtilsTestClasses.BendAllFieldsClass;
import ru.yandex.market.yt.YtTableUtilsTestClasses.BendableClass;
import ru.yandex.market.yt.YtTableUtilsTestClasses.BendableSubClass;
import ru.yandex.market.yt.YtTableUtilsTestClasses.BendableSubClassWithSameName;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class YtTablesUtilsTest {

    private static final String SCHEMA = "schema";

    @Test(expected = IllegalArgumentException.class)
    public void bendAllFieldsTest() {
        YtTablesUtils.makeSchemaAttributes(BendAllFieldsClass.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sameNamesInHierarchyTest() {
        YtTablesUtils.getFieldsNames(BendableSubClassWithSameName.class);
    }

    @Test
    public void makeSchemaAttributes() {
        MapF<String, YTreeNode> schemaWithAttributes = YtTablesUtils.makeSchemaAttributes(BendableClass.class);
        YTreeNode schema = schemaWithAttributes.getOrThrow(SCHEMA);
        MapF<String, YTreeNode> schemaWithAttributesFromSubClass =
            YtTablesUtils.makeSchemaAttributes(BendableSubClass.class);
        YTreeNode schemaWithSubClass = schemaWithAttributesFromSubClass.getOrThrow(SCHEMA);
        assertEquals(2, schema.asList().size());
        assertEquals(3, schemaWithSubClass.asList().size());
    }


    @Test
    public void getFieldsNames() {
        Set<String> fieldsNames = YtTablesUtils.getFieldsNames(BendableClass.class);
        Set<String> expectedNames = new HashSet<>();
        expectedNames.add(BendableClass.MY_INT_NAME);
        expectedNames.add(BendableClass.MY_STRING_NAME);

        Set<String> fieldsNamesFromSubClass = YtTablesUtils.getFieldsNames(BendableSubClass.class);
        Set<String> expectedNamesFromSubClass = new HashSet<>();
        expectedNamesFromSubClass.add(BendableClass.MY_INT_NAME);
        expectedNamesFromSubClass.add(BendableClass.MY_STRING_NAME);
        expectedNamesFromSubClass.add(BendableSubClass.MY_DOUBLE_NAME);

        assertEquals(expectedNames, fieldsNames);
        assertEquals(expectedNamesFromSubClass, fieldsNamesFromSubClass);
    }
}