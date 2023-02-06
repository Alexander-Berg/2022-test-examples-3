package ru.yandex.market.mbi.jaxb.jackson;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.google.common.base.MoreObjects;
import org.json.JSONException;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.mbi.jaxb.jackson.adapter.EnumValueOfXmlAdapter;

public class ApiXmlMapperTest extends BaseJacksonTest {

    public ApiXmlMapperTest() {
        ApiObjectMapperFactory objectMapperFactory = new ApiObjectMapperFactory();
        jsonMapper = objectMapperFactory.createJsonMapper();
        xmlMapper = objectMapperFactory.createXmlMapper();
    }

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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Base base = (Base) o;

            if (id != null ? !id.equals(base.id) : base.id != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .toString();
        }
    }

    @XmlType(name = "child1")
    @XmlRootElement(name = "child1")
    public static class Child1 extends Base {
        @XmlAttribute(name = "id1")
        public String id1;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Child1 child1 = (Child1) o;

            if (id1 != null ? !id1.equals(child1.id1) : child1.id1 != null) return false;

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
        @XmlAttribute(name = "id2")
        public String id2;
    }

    @XmlType(name = "child3type")
    @XmlRootElement(name = "child3root")
    public static class Child3 extends Child2 {
        @XmlAttribute(name = "id3")
        public String id3;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Child3 child3 = (Child3) o;

            if (id3 != null ? !id3.equals(child3.id3) : child3.id3 != null) return false;

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
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Obj obj = (Obj) o;

            if (element != null ? !element.equals(obj.element) : obj.element != null) return false;
            if (elements != null ? !elements.equals(obj.elements) : obj.elements != null) return false;

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

    @XmlRootElement(name = "data")
    public static class Container {

        @XmlElement(name = "element")
        @XmlElementWrapper(name = "elements")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Integer> list;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Container container = (Container) o;

            return !(list != null ? !list.equals(container.list) : container.list != null);

        }

        @Override
        public int hashCode() {
            return list != null ? list.hashCode() : 0;
        }
    }

    @XmlRootElement(name = "data")
    public static class ContainerWithoutWrapper {

        @XmlElement(name = "element")
        @JsonProperty("elements")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Integer> list;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ContainerWithoutWrapper container = (ContainerWithoutWrapper) o;
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

    @XmlRootElement(name="embeddedObjects")
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

        @XmlElementWrapper(name = "elements3")
        @XmlElement(name = "element")
        public void setElements3(List<Base> elements3) {
            this.elements3 = elements3;
        }

        @XmlElementWrapper(name = "elements4")
        @XmlElement(name = "element")
        public List<Base> getElements4() {
            return elements4;
        }

        @XmlElementWrapper(name = "elements4")
        @XmlElement(name = "element")
        public void setElements4(List<Base> elements4) {
            this.elements4 = elements4;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Cont obj = (Cont) o;

            if (element != null ? !element.equals(obj.element) : obj.element != null) return false;
            if (elements1 != null ? !elements1.equals(obj.elements1) : obj.elements1 != null) return false;
            if (elements2 != null ? !elements2.equals(obj.elements2) : obj.elements2 != null) return false;
            if (elements3 != null ? !elements3.equals(obj.elements3) : obj.elements3 != null) return false;
            if (elements4 != null ? !elements4.equals(obj.elements4) : obj.elements4 != null) return false;

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

    @XmlRootElement(name="container")
    public static class GenericNotAnnotatedContainer<T> {

        private T data;

        public GenericNotAnnotatedContainer() {
        }

        public GenericNotAnnotatedContainer(T data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GenericNotAnnotatedContainer<?> that = (GenericNotAnnotatedContainer<?>) o;
            return Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("data", data)
                    .toString();
        }
    }

    @Test
    public void shouldProcessEmptyList() throws IOException, JSONException, SAXException {
        Obj obj = new Obj();
        obj.elements = Collections.emptyList();
        test(obj, "{\"elements\":[]}", "<root><elements/></root>");
    }

    @XmlRootElement(name="container")
    public static class ArrayContainer {

        @XmlElement(name = "int")
        @XmlElementWrapper(name = "ints")
        public int[] ints;

        @XmlElement(name = "long")
        @XmlElementWrapper(name = "longs")
        public long[] longs;

        @XmlElement(name = "float")
        @XmlElementWrapper(name = "floats")
        public float[] floats;

        @XmlElement(name = "double")
        @XmlElementWrapper(name = "doubles")
        public double[] doubles;

        @XmlElement(name = "boolean")
        @XmlElementWrapper(name = "booleans")
        public boolean[] booleans;

        @XmlElement(name = "byte")
        @XmlElementWrapper(name = "bytes")
        public byte[] bytes;

        @XmlElement(name = "char")
        @XmlElementWrapper(name = "chars")
        public char[] chars;

        @XmlElement(name = "short")
        @XmlElementWrapper(name = "shorts")
        public short[] shorts;

        @XmlElement(name = "object")
        @XmlElementWrapper(name = "objects")
        public Object[] objects;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("ints", ints)
                              .add("longs", longs)
                              .add("floats", floats)
                              .add("doubles", doubles)
                              .add("booleans", booleans)
                              .add("bytes", bytes)
                              .add("chars", chars)
                              .add("shorts", shorts)
                              .add("objects", objects)
                              .add("integers", integers)
                              .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ArrayContainer that = (ArrayContainer) o;
            return Arrays.equals(ints, that.ints) &&
                    Arrays.equals(longs, that.longs) &&
                    Arrays.equals(floats, that.floats) &&
                    Arrays.equals(doubles, that.doubles) &&
                    Arrays.equals(booleans, that.booleans) &&
                    Arrays.equals(bytes, that.bytes) &&
                    Arrays.equals(chars, that.chars) &&
                    Arrays.equals(shorts, that.shorts) &&
                    Arrays.equals(objects, that.objects) &&
                    Arrays.equals(integers, that.integers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ints, longs, floats, doubles, booleans, bytes, chars, shorts, objects, integers);
        }

        @XmlElement(name = "integer")
        @XmlElementWrapper(name = "integers")
        public Integer[] integers;

    }

    @Test
    public void shouldProcessEmptyArray() throws IOException, JSONException, SAXException {
        ArrayContainer obj = new ArrayContainer();
        obj.longs = new long[0];
        obj.ints = new int[0];
        obj.shorts = new short[0];
        obj.booleans = new boolean[0];
        obj.doubles = new double[0];
        obj.floats = new float[0];
        obj.objects = new Integer[0];
        obj.integers = new Integer[0];
        test(obj,
                "{\"ints\":[],\"longs\":[],\"floats\":[],\"doubles\":[],\"booleans\":[],\"shorts\":[],\"objects\":[],\"integers\":[]}",
                "<container><ints/><longs/><floats/><doubles/><booleans/><shorts/><objects/><integers/></container>");
    }

    @Test
    public void shouldProcessNullArray() throws IOException, JSONException, SAXException {
        ArrayContainer obj = new ArrayContainer();
        obj.longs = null;
        obj.ints = null;
        obj.shorts = null;
        obj.bytes = null;
        obj.booleans = null;
        obj.doubles = null;
        obj.floats = null;
        obj.chars = null;
        obj.objects = null;
        obj.integers = null;
        test(obj, "{}", "<container/>");
    }

    @Test
    public void shouldProcessNonEmptyArray() throws IOException, JSONException, SAXException {
        ArrayContainer obj = new ArrayContainer();
        obj.longs = new long[]{1, 2, 4};
        obj.ints = new int[]{2, 3, 4};
        obj.shorts = new short[]{1, 1, 1};
//        obj.bytes = null;
        obj.booleans = new boolean[]{true, false, true};
        obj.doubles = new double[]{1.2, 2.5};
        obj.floats = new float[]{1.1f, 9.0f};
//        obj.chars = null;
//        obj.objects = new Integer[]{1, 1, 1};
        obj.integers = new Integer[]{2, 2, 2};
        test(obj,
                "{\"ints\":[2,3,4],\"longs\":[1,2,4],\"floats\":[1.1,9.0],\"doubles\":[1.2,2.5],\"booleans\":[true,false,true],\"shorts\":[1,1,1],\"integers\":[2,2,2]}",
                "<container><ints><int>2</int><int>3</int><int>4</int></ints><longs><long>1</long><long>2</long><long>4</long></longs><floats><float>1.1</float><float>9.0</float></floats><doubles><double>1.2</double><double>2.5</double></doubles><booleans><boolean>true</boolean><boolean>false</boolean><boolean>true</boolean></booleans><shorts><short>1</short><short>1</short><short>1</short></shorts><integers><integer>2</integer><integer>2</integer><integer>2</integer></integers></container>");
    }

    @Test
    public void shouldProcessEmptyObject() throws IOException, JSONException, SAXException {
        Obj obj = new Obj();
        test(obj, "{}", "<root/>");
    }

    @Test
    public void shouldNotWrapElements() throws IOException, JSONException, SAXException {
        ContainerWithoutWrapper obj = new ContainerWithoutWrapper();
        obj.list = Arrays.asList(1,2);
        test(obj, "{\"elements\":[1,2]}", "<data><element>1</element><element>2</element></data>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingBaseClass() throws IOException, JSONException, SAXException {
        Obj obj = new Obj();
        obj.element = new Base();
        test(obj, "{\"el\":{\"__type\":\"base\"}}", "<root><el __type=\"base\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingChild1Class() throws IOException, JSONException, SAXException {
        Obj obj = new Obj();
        obj.element = new Child1();
        test(obj, "{\"el\":{\"__type\":\"child1\"}}", "<root><el __type=\"child1\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingChild2Class() throws IOException, JSONException, SAXException {
        Obj obj = new Obj();
        obj.element = new Child2();
        test(obj, "{\"el\":{\"__type\":\"child2\"}}", "<root><el __type=\"child2\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingChild3Class() throws IOException, JSONException, SAXException {
        Obj obj = new Obj();
        obj.element = new Child3();
        test(obj, "{\"el\":{\"__type\":\"child3type\"}}", "<root><el __type=\"child3type\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicElementUsingChild3WithIdAttrClass() throws IOException, JSONException, SAXException {
        Obj obj = new Obj();
        Child3 child3 = new Child3();
        child3.id = "-id";
        child3.id2 = "-id2";
        child3.id3 = "-id3";
        obj.element = child3;
        test(obj,
                "{\"el\":{\"__type\":\"child3type\",\"id\":\"-id\",\"id2\":\"-id2\",\"id3\":\"-id3\"}}",
                "<root><el __type=\"child3type\" id=\"-id\" id2=\"-id2\" id3=\"-id3\"/></root>");
    }

    @Test
    public void shouldProcessPolymorphicList() throws IOException, JSONException, SAXException {
        Obj obj = new Obj();
        obj.elements = new ArrayList<>();
        obj.elements.add(new Base());
        obj.elements.add(new Child1());
        obj.elements.add(new Child2());
        obj.elements.add(new Child3());
        test(obj,
                "{\"elements\":[{\"__type\":\"base\"},{\"__type\":\"child1\"},{\"__type\":\"child2\"}," +
                        "{\"__type\":\"child3type\"}]}",
                "<root><elements><element __type=\"base\"/><element __type=\"child1\"/><element " +
                        "__type=\"child2\"/><element __type=\"child3type\"/></elements></root>");
    }

    @Test
    public void shouldSupportMixinForListsAndWrappers() throws IOException, JSONException, SAXException {
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
        getXmlObjectMapper().readValue("<embeddedObjects attr1=\"value1\"><nestedObject attr=\"value\"><n1 attr2=\"value2\"/></nestedObject></embeddedObjects>".getBytes(), EmbeddedObjects.class);
    }


    @Test
    public void shouldSerializeNotAnnotatedFields() throws IOException, JSONException, SAXException {
        GenericNotAnnotatedContainer<Base> obj = new GenericNotAnnotatedContainer<>(new Base(1));
        test(obj, new GenericNotAnnotatedContainer<>(), "{}", "<container/>");
    }

    /**
     * Тестируем случай когда в классе на разных полях/методах может быть определено несколько {@link XmlElement}
     * с одним и тем-же значением {@link XmlElement#name()} но разными значениями в {@link XmlElementWrapper#name()}
     *
     * @throws IOException
     * @throws JSONException
     */
    @Test
    public void testWrapperUsage() throws IOException, JSONException, SAXException {
        Cont obj = new Cont();
        obj.element = new Base(0);
        obj.elements1 = Arrays.asList(new Base(1));
        obj.elements2 = Arrays.asList(new Base(2));
        obj.elements3 = Arrays.asList(new Base(3));
        obj.elements4 = Arrays.asList(new Base(4));

        test(obj, "{\"element\":{\"id\":\"0\"},\"elements1\":[{\"id\":\"1\"}],\"elements2\":[{\"id\":\"2\"}],\"elements3\":[{\"id\":\"3\"}],\"elements4\":[{\"id\":\"4\"}]}",
            "<cont><element id=\"0\"/><elements1><element id=\"1\"/></elements1><elements2><element id=\"2\"/></elements2><elements3><element id=\"3\"/></elements3><elements4><element id=\"4\"/></elements4></cont>");
    }

    @XmlRootElement(name = "data")
    public static class EnumTest {
        public enum EnumWithoutXmlEnum {
            ONE,
            TWO
        }

        @XmlEnum
        public enum EnumWithoutXmlEnumValue {
            ONE,
            TWO
        }
    }

    @XmlRootElement(name="data")
    public static class TestEnumIds {

        @XmlEnum
        public enum Enummm {
            @XmlEnumValue("111")
            ONE,
            @XmlEnumValue("222")
            TWO
        }

        @XmlAttribute(name="enummm")
        public Enummm enummm;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestEnumIds that = (TestEnumIds) o;
            return enummm == that.enummm;
        }

        @Override
        public int hashCode() {
            return Objects.hash(enummm);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("enummm", enummm)
                    .toString();
        }
    }

    @Test
    public void shouldWorkWithEnumIds() throws IOException, JSONException, SAXException {
        TestEnumIds obj = new TestEnumIds();
        obj.enummm = TestEnumIds.Enummm.ONE;
        test(obj, "{\"enummm\":\"111\"}", "<data enummm=\"111\"/>");
    }


    @Test(expected = JsonMappingException.class)
    public void shouldNotDeserializeEnumWithoutXmlEnum() throws IOException, JSONException {
        EnumTest.EnumWithoutXmlEnum obj = EnumTest.EnumWithoutXmlEnum.ONE;
        testDeserialization(obj, "{\"enum\":\"ONE\"}", "<data enum=\"0\"/>");
        fail();
    }

    @Test(expected = JsonMappingException.class)
    public void shouldNotDeserializeEnumWithoutXmlEnumValue() throws IOException, JSONException {
        EnumTest.EnumWithoutXmlEnumValue obj = EnumTest.EnumWithoutXmlEnumValue.ONE;
        testDeserialization(obj, "{\"enum\":\"ONE\"}", "<data enum=\"0\"/>");
        fail();
    }

    @XmlRootElement(name = "data")
    public static class TimeTest {

        @XmlElement(name = "localDateTime")
        public LocalDateTime localDateTime;

        @XmlElement(name = "localDate")
        public LocalDate localDate;

        @XmlElement(name = "zonedDateTime")
        public ZonedDateTime zonedDateTime;

        @XmlElement(name = "date")
        public Date date;

        @XmlElement(name = "instant")
        public Instant instant;

        @XmlElement(name = "localTime")
        public LocalTime localTime;

        @XmlElement(name = "offsetDateTime")
        public OffsetDateTime offsetDateTime;

        @XmlElement(name = "period")
        public Period period;

        @XmlElement(name = "duration")
        public Duration duration;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("localDateTime", localDateTime)
                              .add("localDate", localDate)
                              .add("zonedDateTime", zonedDateTime)
                              .add("date", date)
                              .add("instant", instant)
                              .add("localTime", localTime)
                              .add("offsetDateTime", offsetDateTime)
                              .add("period", period)
                              .add("duration", duration)
                              .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TimeTest timeTest = (TimeTest) o;
            return Objects.equals(localDateTime, timeTest.localDateTime) &&
                    Objects.equals(localDate, timeTest.localDate) &&
                    Objects.equals(zonedDateTime, timeTest.zonedDateTime) &&
                    Objects.equals(date, timeTest.date) &&
                    Objects.equals(instant, timeTest.instant) &&
                    Objects.equals(localTime, timeTest.localTime) &&
                    Objects.equals(offsetDateTime, timeTest.offsetDateTime) &&
                    Objects.equals(period, timeTest.period) &&
                    Objects.equals(duration, timeTest.duration);
        }

        @Override
        public int hashCode() {
            return Objects.hash(localDateTime, localDate, zonedDateTime, date, instant, localTime, offsetDateTime, period, duration);
        }
    }

    @Test
    public void shouldWorkWithDatesAndTime() throws IOException, JSONException, SAXException {
        ZoneId zoneId = ZoneId.of("Asia/Hong_Kong");
        TimeTest obj = new TimeTest();
        obj.localDateTime = LocalDateTime.of(2001, Month.APRIL, 4, 2, 1, 45, 9);
        obj.zonedDateTime = ZonedDateTime.of(2001, Month.APRIL.getValue(), 4, 2, 1, 45, 9, zoneId);
        obj.offsetDateTime = OffsetDateTime.of(2001, Month.APRIL.getValue(), 4, 2, 1, 45, 9, ZoneOffset.ofHours(7));
        obj.localDate = LocalDate.of(2001, Month.APRIL, 2);
        obj.localTime = LocalTime.of(1, 2, 3, 4);
        obj.date = Date.from(LocalDateTime.of(2001, Month.APRIL, 4, 2, 1, 45, 9).atZone(zoneId).toInstant());
        obj.instant = LocalDateTime.of(2001, Month.APRIL, 4, 2, 1, 45, 88).atZone(zoneId).toInstant();
        obj.period = Period.of(1, 40, 21);
        obj.duration = Duration.ofDays(1).plusHours(2).plusMinutes(3).plusSeconds(4).plusNanos(5);
        test(obj, "{\n" +
                        "  \"localDateTime\": \"2001-04-04T02:01:45.000000009\",\n" +
                        "  \"localDate\": \"2001-04-02\",\n" +
                        "  \"zonedDateTime\": \"2001-04-04T02:01:45.000000009+08:00[Asia/Hong_Kong]\",\n" +
                        "  \"date\": \"2001-04-03T22:01:45+04:00\",\n" +
                        "  \"instant\": \"2001-04-03T22:01:45.000000088+04:00\",\n" +
                        "  \"localTime\": \"01:02:03.000000004\",\n" +
                        "  \"offsetDateTime\": \"2001-04-04T02:01:45.000000009+07:00\",\n" +
                        "  \"period\": \"P1Y40M21D\",\n" +
                        "  \"duration\": \"PT26H3M4.000000005S\"\n" +
                        "}",
                "<data>\n" +
                        "    <localDateTime>2001-04-04T02:01:45.000000009</localDateTime>\n" +
                        "    <localDate>2001-04-02</localDate>\n" +
                        "    <zonedDateTime>2001-04-04T02:01:45.000000009+08:00[Asia/Hong_Kong]</zonedDateTime>\n" +
                        "    <date>2001-04-03T22:01:45+04:00</date>\n" +
                        "    <instant>2001-04-03T22:01:45.000000088+04:00</instant>\n" +
                        "    <localTime>01:02:03.000000004</localTime>\n" +
                        "    <offsetDateTime>2001-04-04T02:01:45.000000009+07:00</offsetDateTime>\n" +
                        "    <period>P1Y40M21D</period>\n" +
                        "    <duration>PT26H3M4.000000005S</duration>\n" +
                        "</data>");
    }


    @XmlRootElement(name = "message")
    public static class TestXmlValueBodyParamDeserialization {

        @XmlValue
        @JsonProperty("body")
        private String body;

        public TestXmlValueBodyParamDeserialization() {
        }

        public String getBody() {
            return body;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestXmlValueBodyParamDeserialization that = (TestXmlValueBodyParamDeserialization) o;
            return Objects.equals(body, that.body);
        }

        @Override
        public int hashCode() {
            return Objects.hash(body);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("body", body)
                    .toString();
        }
    }

    @Test
    public void shouldNotNPEWhenXmlValueHasBodyName() throws IOException, JSONException, SAXException {
        TestXmlValueBodyParamDeserialization obj = new TestXmlValueBodyParamDeserialization();
        obj.body = "123";
        test(obj, "{\"body\":\"123\"}", "<message>123</message>");
    }

    @XmlSeeAlso({Derived11.class, Derived12.class})
    public static class Base1 {
        @XmlAttribute(name="id")
        int id;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Base1 base1 = (Base1) o;
            return id == base1.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .toString();
        }
    }

    @XmlType(name="Derived1")
    public static class Derived11 extends Base1 {

    }

    @XmlType(name="Derived2")
    public static class Derived12 extends Base1 {

    }

    @XmlSeeAlso({Derived21.class, Derived22.class})
    public static class Base2{
        @XmlAttribute(name="id")
        int id;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Base2 base2 = (Base2) o;
            return id == base2.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .toString();
        }

    }

    @XmlType(name="Derived1")
    public static class Derived21 extends Base2 {

    }

    @XmlType(name="Derived2")
    public static class Derived22 extends Base2 {

    }

    @XmlRootElement(name="data")
    public  static class GenericContainer<T> {
        @XmlPolymorphicElement
        @XmlElement(name="object")
        T object;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GenericContainer that = (GenericContainer) o;
            return Objects.equals(object, that.object);
        }

        @Override
        public int hashCode() {
            return Objects.hash(object);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("object", object)
                    .toString();
        }
    }

    public static class GenericContainer1 extends GenericContainer<Base1> {
    }

    public static class GenericContainer2 extends GenericContainer<Base2> {
    }

    @Test
    public void shouldUseRootTypeBasedXmlTypeNamespacedWhenTwoDifferentClassHaveTheSameXmlTypeName()
            throws IOException, JSONException, SAXException {
        GenericContainer1 obj1 = new GenericContainer1();
        obj1.object = new Derived11();
        test(obj1, "{\"object\":{\"__type\":\"Derived1\",\"id\":0}}", "<data><object __type=\"Derived1\" id=\"0\"/></data>");

        GenericContainer2 obj2 = new GenericContainer2();
        obj2.object = new Derived21();
        test(obj2, "{\"object\":{\"__type\":\"Derived1\",\"id\":0}}", "<data><object __type=\"Derived1\" id=\"0\"/></data>");
    }

    public static class EnumCustomSerializer extends EnumValueOfXmlAdapter<EnumWithCustomSerializer> {
        public EnumCustomSerializer() {
            super(EnumWithCustomSerializer.class);
        }
    }

    @XmlJavaTypeAdapter(value = EnumCustomSerializer.class)
    public enum EnumWithCustomSerializer {
        VALUE1,
        VALUE2
    }

    @XmlRootElement(name = "data")
    public static class EnumCustomSerializerContainer {

        @XmlElement(name = "ennum")
        public EnumWithCustomSerializer ennum;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            EnumCustomSerializerContainer that = (EnumCustomSerializerContainer) o;
            return ennum == that.ennum;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ennum);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("ennum", ennum)
                              .toString();
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenUseCustomSerializerForEnum() throws JSONException, SAXException, IOException {
        EnumCustomSerializerContainer obj = new EnumCustomSerializerContainer();
        obj.ennum = EnumWithCustomSerializer.VALUE2;
        test(obj, "{\"ennum\":VALUE2}", "<data><ennum>VALUE2</ennum></data>");

        obj.ennum = null;
        test(obj, "{}", "<data></data>");
    }


    @XmlRootElement(name = "data")
    public static class BooleanContainer {
        @XmlAttribute(name = "trueObject")
        public Boolean trueObject;

        @XmlAttribute(name = "falseObject")
        public Boolean falseObject;

        @XmlAttribute(name = "trueValue")
        public boolean trueValue;

        @XmlAttribute(name = "falseValue")
        public boolean falseValue;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("trueObject", trueObject)
                              .add("falseObject", falseObject)
                              .add("trueValue", trueValue)
                              .add("falseValue", falseValue)
                              .toString();
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BooleanContainer that = (BooleanContainer) o;
            return trueValue == that.trueValue &&
                    falseValue == that.falseValue &&
                    Objects.equals(trueObject, that.trueObject) &&
                    Objects.equals(falseObject, that.falseObject);
        }

        @Override
        public int hashCode() {
            return Objects.hash(trueObject, falseObject, trueValue, falseValue);
        }
    }

    @Test
    public void shouldSerializeBooleans() throws JSONException, SAXException, IOException {
        BooleanContainer obj = new BooleanContainer();
        obj.trueObject = true;
        obj.falseObject = false;
        obj.trueValue = true;
        obj.falseValue = false;
        test(obj,
                "{\"trueObject\":true,\"falseObject\":false,\"trueValue\":true,\"falseValue\":false}",
                "<data trueObject=\"true\" falseObject=\"false\" trueValue=\"true\" falseValue=\"false\"/>");
    }

    @XmlRootElement(name = "data")
    public static class IntegerContainer {
        public Integer integer;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("integer", integer)
                              .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IntegerContainer that = (IntegerContainer) o;
            return Objects.equals(integer, that.integer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(integer);
        }
    }


    public static class ContainerWithDefaultObjectValue {

        public Integer integer = 456;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ContainerWithDefaultObjectValue that = (ContainerWithDefaultObjectValue) o;
            return Objects.equals(integer, that.integer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(integer);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("integer", integer)
                              .toString();
        }
    }


    @Test
    public void shouldNotSerializeNullValues() throws JSONException, SAXException, IOException {
        IntegerContainer obj = new IntegerContainer();
        test(obj, "{}", "<data/>");
    }

    @Test
    public void shouldNotOverrideDefaultValues() throws IOException, JSONException {
        ContainerWithDefaultObjectValue obj = new ContainerWithDefaultObjectValue();
        testDeserialization(obj, "{}", "<data/>");
    }
}
