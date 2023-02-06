package ru.yandex.market.api.controller.jackson;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.controller.annotations.XmlPolymorphicElement;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.filters.Filter;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ApiStrings;

import javax.xml.bind.annotation.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@WithContext
public class ObjectMapperFactoryTest extends BaseJacksonTest {

    @XmlSeeAlso({Child1.class, Child2.class})
    @XmlType(name = "base")
    @XmlRootElement(name = "base")
    public static class Base {

        public Base() {
        }

        public Base(Object id) {
            this.id = String.valueOf(id);
        }

        @XmlAttribute(name = "id")
        public String id;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Base base = (Base) o;

            if (id != null ? !id.equals(base.id) : base.id != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }

    @XmlType(name = "child1")
    @XmlRootElement(name = "child1")
    public static class Child1 extends Base {
        @XmlAttribute(name = "id1")
        public String id1;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Child1 child1 = (Child1) o;

            if (id1 != null ? !id1.equals(child1.id1) : child1.id1 != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return id1 != null ? id1.hashCode() : 0;
        }
    }

    @XmlSeeAlso({Child3.class})
    @XmlType(name = "child2")
    @XmlRootElement(name = "child2")
    public static class Child2 extends Base {
        @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
        @XmlAttribute(name = "id2")
        public String id2;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            Child2 child2 = (Child2) o;

            return id2 != null ? id2.equals(child2.id2) : child2.id2 == null;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (id2 != null ? id2.hashCode() : 0);
            return result;
        }
    }

    @XmlType(name = "child3type")
    @XmlRootElement(name = "child3root")
    public static class Child3 extends Child2 {
        @XmlAttribute(name = "id3")
        public String id3;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Child3 child3 = (Child3) o;

            if (id3 != null ? !id3.equals(child3.id3) : child3.id3 != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return id3 != null ? id3.hashCode() : 0;
        }
    }

    @XmlRootElement(name = "root")
    public static class Obj {
        @XmlPolymorphicElement
        @XmlElement(name = "el")
        public Base element;

        @XmlPolymorphicElement
        @XmlElementWrapper(name = "elements")
        @XmlElement(name = "element")
        public List<Base> elements;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Obj obj = (Obj) o;

            if (element != null ? !element.equals(obj.element) : obj.element != null) {
                return false;
            }
            if (elements != null ? !elements.equals(obj.elements) : obj.elements != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = element != null ? element.hashCode() : 0;
            result = 31 * result + (elements != null ? elements.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Obj{" +
                "element=" + element +
                ", elements=" + elements +
                '}';
        }
    }

    @XmlRootElement(name = "/data")
    public static class Container {
        @JacksonXmlElementWrapper(useWrapping = false)
        @XmlElementWrapper(name = "elements")
        @XmlElement(name = "element")
        private List<Integer> list;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Container container = (Container) o;

            return !(list != null ? !list.equals(container.list) : container.list != null);

        }

        @Override
        public int hashCode() {
            return list != null ? list.hashCode() : 0;
        }
    }

    @XmlRootElement(name = "rootMixin")
    public abstract class ContainerMixin {
        @JacksonXmlElementWrapper(useWrapping = true)
        @XmlElementWrapper(name = "mixinElements")
        @XmlElement(name = "mixinElem")
        List<Integer> list;
    }

    @XmlRootElement(name = "embeddedObjects")
    public static class EmbeddedObjects {
        @JsonAnySetter
        public void set(String name, Object value) {
            System.out.println("name: " + name + ", value: " + value);
        }
    }

    /**
     * Для проверки сериализации
     */
    @XmlRootElement(name = "cont")
    public static class Cont {
        @XmlElement(name = "element")
        public Base element;

        @XmlElementWrapper(name = "elements1")
        @XmlElement(name = "element")
        public List<Base> elements1;

        @XmlElementWrapper(name = "elements2")
        @XmlElement(name = "element")
        public List<Base> elements2;

        private List<Base> elements3;

        private List<Base> elements4;

        @XmlElementWrapper(name = "elements3")
        @XmlElement(name = "element")
        public List<Base> getElements3() {
            return elements3;
        }

        @XmlElementWrapper(name = "elements4")
        @XmlElement(name = "element")
        public List<Base> getElements4() {
            return elements4;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Cont obj = (Cont) o;

            if (element != null ? !element.equals(obj.element) : obj.element != null) {
                return false;
            }
            if (elements1 != null ? !elements1.equals(obj.elements1) : obj.elements1 != null) {
                return false;
            }
            if (elements2 != null ? !elements2.equals(obj.elements2) : obj.elements2 != null) {
                return false;
            }
            if (elements3 != null ? !elements3.equals(obj.elements3) : obj.elements3 != null) {
                return false;
            }
            if (elements4 != null ? !elements4.equals(obj.elements4) : obj.elements4 != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = element != null ? element.hashCode() : 0;
            result = 31 * result + (elements1 != null ? elements1.hashCode() : 0);
            result = 31 * result + (elements2 != null ? elements2.hashCode() : 0);
            result = 31 * result + (elements3 != null ? elements3.hashCode() : 0);
            result = 31 * result + (elements4 != null ? elements4.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Obj{" +
                "element=" + element + ", elements1=" + elements1 + ", elements2=" + elements2 +
                ", elements3=" + elements3 + ", elements4=" + elements4 +
                '}';
        }
    }

    @Test
    public void shouldProcessEmptyList() throws IOException, JSONException {
        Obj obj = new Obj();
        obj.elements = Collections.emptyList();
        ContextHolder.get().setVersion(Version.V2_0_0);

        test(obj, "{\"elements\":[]}", "<root><elements/></root>");
    }

    @Test
    public void shouldProcessEmptyObject() throws IOException, JSONException {
        Obj obj = new Obj();
        ContextHolder.get().setVersion(Version.V2_0_0);

        test(obj, "{}", "<root/>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingBaseClass() throws IOException, JSONException {
        Obj obj = new Obj();
        obj.element = new Base();
        ContextHolder.get().setVersion(Version.V2_0_0);

        test(obj, "{\"el\":{\"__type\":\"base\"}}", "<root><el __type=\"base\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingChild1Class() throws IOException, JSONException {
        Obj obj = new Obj();
        obj.element = new Child1();
        ContextHolder.get().setVersion(Version.V2_0_0);

        test(obj, "{\"el\":{\"__type\":\"child1\"}}", "<root><el __type=\"child1\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingChild2Class() throws IOException, JSONException {
        Obj obj = new Obj();
        obj.element = new Child2();
        ContextHolder.get().setVersion(Version.V2_0_0);

        test(obj, "{\"el\":{\"__type\":\"child2\"}}", "<root><el __type=\"child2\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingChild3Class() throws IOException, JSONException {
        Obj obj = new Obj();
        obj.element = new Child3();
        ContextHolder.get().setVersion(Version.V2_0_0);

        test(obj, "{\"el\":{\"__type\":\"child3type\"}}", "<root><el __type=\"child3type\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingChild3WithIdAttrClass() throws IOException, JSONException {
        Obj obj = new Obj();
        Child3 child3 = new Child3();
        child3.id = "-id";
        child3.id2 = "-id2";
        child3.id3 = "-id3";
        obj.element = child3;
        ContextHolder.get().setVersion(Version.V2_0_0);

        test(obj,
            "{\"el\":{\"__type\":\"child3type\",\"id\":\"-id\",\"id2\":\"-id2\",\"id3\":\"-id3\"}}",
            "<root><el __type=\"child3type\" id=\"-id\" id2=\"-id2\" id3=\"-id3\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicList() throws IOException, JSONException {
        Obj obj = new Obj();
        obj.elements = new ArrayList<>();
        obj.elements.add(new Base());
        obj.elements.add(new Child1());
        obj.elements.add(new Child2());
        obj.elements.add(new Child3());
        ContextHolder.get().setVersion(Version.V2_0_0);

        test(obj,
            "{\"elements\":[{\"__type\":\"base\"},{\"__type\":\"child1\"},{\"__type\":\"child2\"}," +
                "{\"__type\":\"child3type\"}]}",
            "<root><elements><element __type=\"base\"/><element __type=\"child1\"/><element " +
                "__type=\"child2\"/><element __type=\"child3type\"/></elements></root>");
    }

    @Test
    public void shouldSupportMixinForListsAndWrappers() throws IOException, JSONException {
        ContextHolder.get().setVersion(Version.V2_0_0);

        Container obj = new Container();
        getJsonObjectMapper().addMixIn(Container.class, ContainerMixin.class);
        getXmlObjectMapper().addMixIn(Container.class, ContainerMixin.class);
        obj.list = Arrays.asList(1, 2, 3);

        test(obj,
            "{\"mixinElements\":[1,2,3]}",
            "<rootMixin><mixinElements><mixinElem>1</mixinElem><mixinElem>2</mixinElem><mixinElem>3</mixinElem></mixinElements></rootMixin>");
    }

    @Test
    public void shouldUseJsonAnySetterAnnotation() throws IOException {
        getXmlObjectMapper().readValue("<embeddedObjects attr1=\"value1\"><nestedObject attr=\"value\"><n1 attr2=\"value2\"/></nestedObject></embeddedObjects>".getBytes(ApiStrings.UTF8), EmbeddedObjects.class);
    }

    @Test
    public void checkVersionedAnnotation() throws IOException {
        ModelV2 model = new ModelV2();
        model.setId(17L);
        Filter filter = new Filter();
        filter.setId("-2");
        filter.setName("test");
        model.setFilters(Lists.newArrayList(filter));

        ContextHolder.get().setVersion(Version.V1_0_0);
        Assert.assertEquals("{\"id\":17,\"isNew\":false}", getJsonObjectMapper().writeValueAsString(model));
        Assert.assertEquals("<model id=\"17\" isNew=\"false\"/>", getXmlObjectMapper().writeValueAsString(model));

        ContextHolder.get().setVersion(Version.V2_0_0);
        Assert.assertEquals("{\"id\":17,\"isNew\":false,\"filters\":{\"filtersList\":[{\"id\":\"-2\",\"name\":\"test\"}]}}",
            getJsonObjectMapper().writeValueAsString(model));
        Assert.assertEquals("<model id=\"17\" isNew=\"false\"><filters><filtersList><filter id=\"-2\" name=\"test\"/></filtersList></filters></model>",
            getXmlObjectMapper().writeValueAsString(model));

        ContextHolder.get().setVersion(Version.V2_0_1);
        Assert.assertEquals("{\"id\":17,\"isNew\":false,\"filters\":[{\"id\":\"-2\",\"name\":\"test\"}]}", getJsonObjectMapper().writeValueAsString(model));
        Assert.assertEquals("<model id=\"17\" isNew=\"false\"><filters><filter id=\"-2\" name=\"test\"/></filters></model>", getXmlObjectMapper().writeValueAsString(model));
    }

    /**
     * Тестируем случай когда в классе на разных полях/методах может быть определено несколько {@link XmlElement}
     * с одним и тем-же значением {@link XmlElement#name()} но разными значениями в {@link XmlElementWrapper#name()}
     * <p>
     * См. {@link CustomPOJOPropertiesCollector}
     *
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testWrapperUsage() throws IOException, JSONException {
        Cont obj = new Cont();
        obj.element = new Base(0);
        obj.elements1 = Arrays.asList(new Base(1));
        obj.elements2 = Arrays.asList(new Base(2));
        obj.elements3 = Arrays.asList(new Base(3));
        obj.elements4 = Arrays.asList(new Base(4));
        ContextHolder.get().setVersion(Version.V2_0_0);

        test(obj, "{\"element\":{\"id\":\"0\"},\"elements1\":[{\"id\":\"1\"}],\"elements2\":[{\"id\":\"2\"}],\"elements3\":[{\"id\":\"3\"}],\"elements4\":[{\"id\":\"4\"}]}",
            "<cont><element id=\"0\"/><elements1><element id=\"1\"/></elements1><elements2><element id=\"2\"/></elements2><elements3><element id=\"3\"/></elements3><elements4><element id=\"4\"/></elements4></cont>");
    }
}
