package ru.yandex.market.tsum.pipe.engine.source_code.model;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.ResourceField;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.ResourceInfo;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.inputs.checkbox.CheckboxListField;
import ru.yandex.market.tsum.pipe.engine.definition.resources.forms.inputs.checkbox.CheckboxValue;
import ru.yandex.market.tsum.pipe.engine.source_code.model.forms.FieldControlType;
import ru.yandex.market.tsum.pipe.engine.source_code.model.forms.controls.SelectFieldControl;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 25/01/2019
 */
public class ResourceObjectConverterTest {
    @Test
    public void getsTypeFromClass() {
        ResourceObject object = ResourceObjectConverter.convert(
            UUID.randomUUID(), TestNotAnnotatedResource.class, new HashMap<>()
        );

        Assert.assertEquals(object.getFields().size(), 4);

        ResourceFieldObject aField = object.getFields().get(0);
        ResourceFieldObject bField = object.getFields().get(1);
        ResourceFieldObject cField = object.getFields().get(2);
        ResourceFieldObject dField = object.getFields().get(3);

        Assert.assertEquals("A", aField.getTitle());
        Assert.assertEquals(FieldControlType.NUMBER, aField.getControl().getType());

        Assert.assertEquals("B", bField.getTitle());
        Assert.assertEquals(FieldControlType.CHECKBOX, bField.getControl().getType());

        Assert.assertEquals("C", cField.getTitle());
        Assert.assertEquals(FieldControlType.SELECT, cField.getControl().getType());
        Assert.assertTrue(((SelectFieldControl) cField.getControl()).getCreatable());
        Assert.assertTrue(((SelectFieldControl) cField.getControl()).getMultiple());

        Assert.assertEquals("D", dField.getTitle());
        Assert.assertEquals(FieldControlType.SELECT, dField.getControl().getType());
        Assert.assertEquals(2, ((SelectFieldControl) dField.getControl()).getOptions().size());
    }

    @Test
    public void checkInheritance() {
        ResourceObject object = ResourceObjectConverter.convert(
            UUID.randomUUID(), NestedTestResource.class, new HashMap<>()
        );

        Assert.assertEquals(object.getFields().size(), 3);

        ResourceFieldObject aField = object.getFields().get(0);
        ResourceFieldObject bField = object.getFields().get(1);
        ResourceFieldObject cField = object.getFields().get(2);

        Assert.assertEquals("aField", aField.getTitle());
        Assert.assertEquals("B", bField.getTitle());
        Assert.assertEquals("cField", cField.getTitle());
    }

    private enum TestEnum {
        TEST_A, TEST_B
    }

    @ResourceInfo(title = "TestNotAnnotatedResource")
    private static class TestNotAnnotatedResource implements Resource {
        private int a;
        private boolean b;
        private List<String> c;
        private TestEnum d;

        public int getA() {
            return a;
        }

        public boolean isB() {
            return b;
        }

        public List<String> getC() {
            return c;
        }

        public TestEnum getD() {
            return d;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("87390840-8e90-4220-8f03-f30472c9171b");
        }
    }

    @ResourceInfo(title = "TestResource")
    private static class TestResource implements Resource {
        @ResourceField(title = "aField")
        @CheckboxListField({@CheckboxValue(value = "1", label = "1")})
        private List<String> a;

        private String b;

        public List<String> getA() {
            return a;
        }

        public String getB() {
            return b;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("edf148e5-078b-4337-8c16-2277fb676bdc");
        }
    }

    private static class NestedTestResource extends TestResource {
        @ResourceField(title = "cField")
        @CheckboxListField({@CheckboxValue(value = "2", label = "2")})
        private List<String> c;

        public List<String> getC() {
            return c;
        }

        @Override
        public UUID getSourceCodeId() {
            return UUID.fromString("15af025c-80ba-4fb6-93cb-19a5f8f204bb");
        }
    }
}
