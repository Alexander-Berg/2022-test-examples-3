package ru.yandex.market.mbo.cms.core.utils.schema.changeset;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.FieldType;

public class FielsTypeOperationsMinusTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusNull() {
        FieldsOperations.fieldMinus(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMinusNodeType() {
        FieldsOperations.fieldMinus(null, new FieldType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNodeTypeMinusNull() {
        FieldsOperations.fieldMinus(new FieldType(), null);
    }

    @Test
    public void testEmptyMinusEmpty() {
        FieldType nt1 = new FieldType("fi1");
        FieldType nt2 = new FieldType("fi2");

        FieldType result = FieldsOperations.fieldMinus(nt1, nt2);

        Assert.assertEquals(nt1.getName(), result.getName());

        //TODO это поправка на временный костыль(или не костыль. пока не понятно)
        Assert.assertEquals(0, result.getProperties().size());
    }

    @Test
    public void testFieldTypeMinusEmpty() {
        FieldType ft1 = new FieldType("ft1");
        FieldType ft2 = new FieldType("ft2");

        ft1.addProperty("prop", Collections.emptyList());
        FieldType result = FieldsOperations.fieldMinus(ft1, ft2);

        Assert.assertEquals(ft1.getName(), result.getName());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(1, result.getProperties().size());
        Assert.assertEquals(ft1.getProperties(), result.getProperties());
    }

    @Test
    public void testEmptyMinusFieldType() {
        FieldType ft1 = new FieldType("ft1");
        FieldType ft2 = new FieldType("ft2");

        ft2.addProperty("prop", Collections.emptyList());
        FieldType result = FieldsOperations.fieldMinus(ft1, ft2);

        Assert.assertEquals(ft1.getName(), result.getName());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(0, result.getProperties().size());
        Assert.assertEquals(ft1.getProperties(), result.getProperties());
    }

    @Test
    public void testFieldTypeMinusFieldType() {
        FieldType ft1 = new FieldType("ft1");
        FieldType ft2 = new FieldType("ft2");

        ft1.addProperty("prop1", Collections.emptyList());
        ft2.addProperty("prop2", Collections.emptyList());
        FieldType result = FieldsOperations.fieldMinus(ft1, ft2);

        Assert.assertEquals(ft1.getName(), result.getName());
        Assert.assertNotNull(result.getProperties());
        Assert.assertEquals(1, result.getProperties().size());
        Assert.assertTrue(result.getProperties().containsKey("prop1"));
        Assert.assertFalse(result.getProperties().containsKey("prop2"));
    }

    @Test
    public void testFieldsTypesMinus() {
        Assert.assertNull(FieldsOperations.fieldsMinus(null, null));
        Assert.assertEquals(
                1,
                FieldsOperations.fieldsMinus(
                        toLinkedHashMap(Collections.singletonMap("ft", new FieldType("ft"))), null
                ).size()
        );
        Assert.assertNull(
                FieldsOperations.fieldsMinus(
                        null,
                        toLinkedHashMap(Collections.singletonMap("ft", new FieldType("ft")))
                )
        );
        Assert.assertEquals(
                0,
                FieldsOperations.fieldsMinus(
                        toLinkedHashMap(Collections.singletonMap("ft", new FieldType("ft"))),
                        toLinkedHashMap(Collections.singletonMap("ft", new FieldType("ft")))
                ).size()
        );
        Assert.assertEquals(
                1,
                FieldsOperations.fieldsMinus(
                        toLinkedHashMap(Collections.singletonMap("ft1", new FieldType("ft1"))),
                        toLinkedHashMap(Collections.singletonMap("ft2", new FieldType("ft2")))
                ).size()
        );
    }

    private LinkedHashMap<String, FieldType> toLinkedHashMap(Map<String, FieldType> src) {
        return new LinkedHashMap<>(src);
    }

}
