package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.FieldType;

public class FielsTypeOperationsPlusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullPlusNull() {
        FieldsOperations.fieldPlus(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPlusNodeType() {
        FieldsOperations.fieldPlus(null, new FieldType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNodeTypePlusNull() {
        FieldsOperations.fieldPlus(new FieldType(), null);
    }

    @Test
    public void testEmptyPlusEmpty() {
        FieldType nt1 = new FieldType("fi1");
        FieldType nt2 = new FieldType("fi2");

        FieldType result = FieldsOperations.fieldPlus(nt1, nt2);

        Assert.assertEquals(nt1.getName(), result.getName());

        //TODO это поправка на временный костыль(или не костыль. пока не понятно)
        Assert.assertEquals(0, result.getProperties().size());
    }

    @Test
    public void testFieldTypePlusEmpty() {
        FieldType ft1 = new FieldType("ft1");
        FieldType ft2 = new FieldType("ft2");

        ft1.addProperty("prop", Collections.emptyList());
        FieldType result = FieldsOperations.fieldPlus(ft1, ft2);

        Assert.assertEquals(ft1.getName(), result.getName());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(1, result.getProperties().size());
        Assert.assertEquals(ft1.getProperties(), result.getProperties());
    }

    @Test
    public void testEmptyPlusFieldType() {
        FieldType ft1 = new FieldType("ft1");
        FieldType ft2 = new FieldType("ft2");

        ft2.addProperty("prop", Collections.emptyList());
        FieldType result = FieldsOperations.fieldPlus(ft1, ft2);

        Assert.assertEquals(ft1.getName(), result.getName());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(1, result.getProperties().size());
        Assert.assertEquals(ft2.getProperties(), result.getProperties());
    }

    @Test
    public void testFieldTypePlusFieldType() {
        FieldType ft1 = new FieldType("ft1");
        FieldType ft2 = new FieldType("ft2");

        ft1.addProperty("prop1", Collections.emptyList());
        ft2.addProperty("prop2", Collections.emptyList());
        FieldType result = FieldsOperations.fieldPlus(ft1, ft2);

        Assert.assertEquals(ft1.getName(), result.getName());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(2, result.getProperties().size());
        Assert.assertTrue(result.getProperties().containsKey("prop1"));
        Assert.assertTrue(result.getProperties().containsKey("prop2"));
    }

    @Test
    public void testFieldsTypesPlus() {
        Assert.assertNull(FieldsOperations.fieldsPlus(null, null));
        Assert.assertEquals(
                1,
                FieldsOperations.fieldsPlus(
                        toLinkedHashMap(Collections.singletonMap("ft", new FieldType("ft"))), null
                ).size()
        );
        Assert.assertEquals(
                1,
                FieldsOperations.fieldsPlus(
                        null,
                        toLinkedHashMap(Collections.singletonMap("ft", new FieldType("ft")))
                ).size()
        );
        Assert.assertEquals(
                1,
                FieldsOperations.fieldsPlus(
                        toLinkedHashMap(Collections.singletonMap("ft", new FieldType("ft"))),
                        toLinkedHashMap(Collections.singletonMap("ft", new FieldType("ft")))
                ).size()
        );
        Assert.assertEquals(
                2,
                FieldsOperations.fieldsPlus(
                        toLinkedHashMap(Collections.singletonMap("ft1", new FieldType("ft1"))),
                        toLinkedHashMap(Collections.singletonMap("ft2", new FieldType("ft2")))
                ).size()
        );
    }

    @Test
    @SuppressWarnings("MagicNumber")
    public void testFieldsTypesPlusWithOrder() {
        LinkedHashMap<String, FieldType> existing = new LinkedHashMap<>();
        LinkedHashMap<String, FieldType> adding = new LinkedHashMap<>();

        existing.put("f1", new FieldType("f1"));
        existing.put("f2", new FieldType("f2"));

        LinkedHashMap<String, FieldType> result = FieldsOperations.fieldsPlus(existing, adding);
        Iterator<Map.Entry<String, FieldType>> iterator = result.entrySet().iterator();
        Assert.assertEquals(2, result.size());
        Assert.assertEquals("f1", iterator.next().getKey());
        Assert.assertEquals("f2", iterator.next().getKey());

        adding.put("f3", new FieldType("f3"));
        adding.put("f4", new FieldType("f4"));

        result = FieldsOperations.fieldsPlus(existing, adding);
        iterator = result.entrySet().iterator();

        Assert.assertEquals(4, result.size());
        Assert.assertEquals("f1", iterator.next().getKey());
        Assert.assertEquals("f2", iterator.next().getKey());
        Assert.assertEquals("f3", iterator.next().getKey());
        Assert.assertEquals("f4", iterator.next().getKey());

        adding.put("f2", new FieldType("f2"));

        result = FieldsOperations.fieldsPlus(existing, adding);
        iterator = result.entrySet().iterator();

        Assert.assertEquals(4, result.size());
        Assert.assertEquals("f1", iterator.next().getKey());
        Assert.assertEquals("f2", iterator.next().getKey());
        Assert.assertEquals("f3", iterator.next().getKey());
        Assert.assertEquals("f4", iterator.next().getKey());

        adding.put("f1", new FieldType("f1"));

        result = FieldsOperations.fieldsPlus(existing, adding);
        iterator = result.entrySet().iterator();

        Assert.assertEquals(4, result.size());
        Assert.assertEquals("f3", iterator.next().getKey());
        Assert.assertEquals("f4", iterator.next().getKey());
        Assert.assertEquals("f2", iterator.next().getKey());
        Assert.assertEquals("f1", iterator.next().getKey());

        adding.put("f5", new FieldType("f1"));

        result = FieldsOperations.fieldsPlus(existing, adding);
        iterator = result.entrySet().iterator();

        Assert.assertEquals(5, result.size());
        Assert.assertEquals("f3", iterator.next().getKey());
        Assert.assertEquals("f4", iterator.next().getKey());
        Assert.assertEquals("f2", iterator.next().getKey());
        Assert.assertEquals("f1", iterator.next().getKey());
        Assert.assertEquals("f5", iterator.next().getKey());

        adding.clear();
        adding.put("f2", new FieldType("f2"));
        adding.put("f1", new FieldType("f1"));

        result = FieldsOperations.fieldsPlus(existing, adding);
        iterator = result.entrySet().iterator();

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("f2", iterator.next().getKey());
        Assert.assertEquals("f1", iterator.next().getKey());
    }

    private LinkedHashMap<String, FieldType> toLinkedHashMap(Map<String, FieldType> src) {
        return new LinkedHashMap<>(src);
    }
}
