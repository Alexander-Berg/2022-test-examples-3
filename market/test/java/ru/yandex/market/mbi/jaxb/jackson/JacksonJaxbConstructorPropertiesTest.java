package ru.yandex.market.mbi.jaxb.jackson;

import java.beans.ConstructorProperties;
import java.io.IOException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class JacksonJaxbConstructorPropertiesTest {

    private static final ObjectMapper MAPPER = new ApiObjectMapperFactory().createJsonMapper();
    private static final String JSON = "{\"a\": 13, \"b\": \"b-value\"}";

    @Test(expected = IOException.class)
    public void testA() throws IOException {
        MAPPER.readValue(JSON, A.class);
    }

    @Test
    public void testB() throws IOException {
        B value = MAPPER.readValue(JSON, B.class);
        Assert.assertEquals("B{a=13, b='b-value'}", value.toString());
        Assert.assertEquals("{}", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testC() throws IOException {
        C value = MAPPER.readValue(JSON, C.class);
        Assert.assertEquals("C{a=0, b='null'}", value.toString());
        Assert.assertEquals("{}", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testD() throws IOException {
        D value = MAPPER.readValue(JSON, D.class);
        Assert.assertEquals("D{a=13, b='b-value'}", value.toString());
        Assert.assertEquals("{\"a\":13,\"b\":\"b-value\"}", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testE() throws IOException {
        E value = MAPPER.readValue(JSON, E.class);
        Assert.assertEquals("E{a=13, b='b-value'}", value.toString());
        Assert.assertEquals("{\"a\":13,\"b\":\"b-value\"}", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testF() throws IOException {
        F value = MAPPER.readValue(JSON, F.class);
        Assert.assertEquals("F{a=13, b='b-value'}", value.toString());
        Assert.assertEquals("{\"a\":13,\"b\":\"b-value\"}", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testG() throws IOException {
        G value = MAPPER.readValue(JSON, G.class);
        Assert.assertEquals("G{a=13, b='b-value'}", value.toString());
        Assert.assertEquals("{\"a\":13,\"b\":\"b-value\"}", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testH() throws IOException {
        H value = MAPPER.readValue(JSON, H.class);
        Assert.assertEquals("H{a=0, b='null'}", value.toString());
        Assert.assertEquals("{\"a\":0}", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testI() throws IOException {
        I value = MAPPER.readValue(JSON, I.class);
        Assert.assertEquals("I{a=13, b='b-value'}", value.toString());
        Assert.assertEquals("{\"a\":13,\"b\":\"b-value\"}", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testJ() throws IOException {
        J value = MAPPER.readValue(JSON, J.class);
        Assert.assertEquals("J{a=42, b='b-value'}", value.toString());
        Assert.assertEquals("{\"a\":42,\"b\":\"b-value\"}", MAPPER.writeValueAsString(value));
    }

    @Test
    public void testK() throws IOException {
        K value = MAPPER.readValue(JSON, K.class);
        Assert.assertEquals("K{a=13, b='b-value'}", value.toString());
        Assert.assertEquals("{\"a\":13,\"b\":\"b-value\"}", MAPPER.writeValueAsString(value));
    }

    /**
     * Десериализация упадёт!
     *
     * У класса А нет ни одного размеченного конструктора, который мог бы вызвать Jackson.
     */
    @XmlRootElement(name = "a")
    static class A {
        private final int a;
        private final String b;

        A(int a, String b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }

    /**
     * Десериализация работает. А сериализация возвращает странный результат.
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code B{a=13, b='b-value'}}</pre>
     * <p>
     * Повторная сериализация:
     * <pre>{@code {}}</pre>
     * <p>
     * Почему сериализация не пишет ничего, потому что Jackson не видит ни одного:
     * <ul>
     *     <li>{@link javax.xml.bind.annotation.XmlElement {@literal @}XmlElement} или
     *     <li>{@link javax.xml.bind.annotation.XmlAttribute {@literal @}XmlAttribute} или
     *     <li>{@link javax.xml.bind.annotation.XmlValue {@literal @}XmlValue}, &mdash;
     * </ul>
     * <p>
     * а значит нет ни одного свойства, которое он мог бы достать.
     */
    @XmlRootElement(name = "b")
    static class B {
        private final int a;
        private final String b;

        @ConstructorProperties({"a", "b"})
        B(int a, String b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }

    /**
     * Десериализация не падает, но результат странный:
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code C{a=0, b='null'}}</pre>
     * <p>
     * Почему конструктор вызывается с <tt>{@code 0}</tt>, <tt>{@code null}</tt>?
     * Потому что Jackson видит, что конструктору нужны свойства <tt>{@code d}</tt> и <tt>{@code e}</tt>,
     * в JSON-объекте, который мы пытаемся десериализовать, таких свойств нет, Jackson считает:
     * &quol;Значит эти свойства будут иметь дефолтное значение!&quor;.
     * Дефолтное значение &mdash; это <tt>{@code 0}</tt>, <tt>{@code null}</tt>, <tt>{@code false}</tt> и т. п. &mdash;
     * дефолтные значения с точки зрения языка программирования Java.
     * <p>
     * Повторная сериализация:
     * <pre>{@code {}}</pre>
     * Почему сериализация не пишет ничего? Потому что Jackson не видит ни одного:
     * <ul>
     *     <li>{@link javax.xml.bind.annotation.XmlElement {@literal @}XmlElement} или
     *     <li>{@link javax.xml.bind.annotation.XmlAttribute {@literal @}XmlAttribute} или
     *     <li>{@link javax.xml.bind.annotation.XmlValue {@literal @}XmlValue}, &mdash;
     * </ul>
     * <p>
     * а значит нет ни одного свойства, которое он мог бы достать.
     */
    @XmlRootElement(name = "c")
    static class C {
        private final int a;
        private final String b;

        @ConstructorProperties({"d", "e"})
        C(int a, String b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }

    /**
     * Всё работает.
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code D{a=13, b='b-value'}}</pre>
     * <p>
     * Повторная сериализация:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     */
    @XmlRootElement(name = "d")
    static class D {
        private final int a;
        private final String b;

        @ConstructorProperties({"a", "b"})
        D(int a, String b) {
            this.a = a;
            this.b = b;
        }

        @XmlElement(name = "a")
        int a() {
            return a;
        }

        @XmlElement(name = "b")
        String b() {
            return b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }

    /**
     * Всё работает.
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code E{a=13, b='b-value'}}</pre>
     * <p>
     * Как это работает? Это стандартная для JAXB-схема:
     * <ul>
     *     <li>сначала создаётся дефолтный инстанс
     *     <li>для каждого свойства из JSON-объекта
     *         дёргается соответствующий setter у бина, или
     *         сеттится соотвествующее поле класса (даже несмотря на то, что оно <tt>{@code private final}</tt>)
     * </ul>
     * <p>
     * Повторная сериализация:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     */
    @XmlRootElement(name = "e")
    static class E {
        @XmlElement(name = "a")
        private int a;

        @XmlElement(name = "b")
        private String b;

        E() {
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }

    /**
     * Всё работает.
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code F{a=13, b='b-value'}}</pre>
     * <p>
     * Как это работает? Это стандартная для JAXB-схема:
     * <ul>
     *     <li>сначала создаётся дефолтный инстанс
     *     <li>для каждого свойства из JSON-объекта
     *         дёргается соответствующий setter у бина, или
     *         сеттится соотвествующее поле класса (даже несмотря на то, что оно <tt>{@code private final}</tt>).
     *         Но здесь же аннотация весит на геттере, а сеттера нет???
     *         Jackson видит аннотацию на геттере и ищет поле
     *         с таким же именем, как геттер, либо с таким же именем, как геттер, но без префикса <tt>get</tt> и
     *         использует это поля для установки значения!!!
     * </ul>
     * <p>
     * Повторная сериализация:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     */
    @XmlRootElement(name = "f")
    static class F {
        private int a;

        private String b;

        F() {
        }

        @XmlElement(name = "a")
        int a() {
            return a;
        }

        @XmlElement(name = "b")
        String b() {
            return b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }

    /**
     * Всё работает.
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code G{a=13, b='b-value'}}</pre>
     * <p>
     * Как это работает? Это стандартная для JAXB-схема:
     * <ul>
     *     <li>сначала создаётся дефолтный инстанс
     *     <li>для каждого свойства из JSON-объекта
     *         дёргается соответствующий setter у бина, или
     *         сеттится соотвествующее поле класса (даже несмотря на то, что оно <tt>{@code private final}</tt>).
     *         Но здесь же аннотация весит на геттере, а сеттера нет???
     *         Jackson видит аннотацию на геттере и ищет поле
     *         с таким же именем, как геттер, либо с таким же именем, как геттер, но без префикса <tt>get</tt> и
     *         использует это поля для установки значения!!!
     * </ul>
     * <p>
     * Повторная сериализация:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     */
    @XmlRootElement(name = "f")
    static class G {
        private int a;

        private String b;

        G() {
        }

        @XmlElement(name = "a")
        int getA() {
            return a;
        }

        @XmlElement(name = "b")
        String getB() {
            return b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }

    /**
     * Десериализация не падает, но результат странный:
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code H{a=0, b='null'}}</pre>
     * <p>
     * Как это работает? Это стандартная для JAXB-схема:
     * <ul>
     *     <li>сначала создаётся дефолтный инстанс
     *     <li>для каждого свойства из JSON-объекта
     *         дёргается соответствующий setter у бина, или
     *         сеттится соотвествующее поле класса (даже несмотря на то, что оно <tt>{@code private final}</tt>).
     *         Но здесь же аннотация весит на геттере, а сеттера нет???
     *         Jackson видит аннотацию на геттере и ищет поле
     *         с таким же именем, как геттер, либо с таким же именем, как геттер, но без префикса <tt>get</tt> и
     *         использует это поля для установки значения!!!
     *         В данном случае ничего подобного нет, поэтому поля остаются не проставленными.
     * </ul>
     * <p>
     * Повторная сериализация:
     * <pre>{@code {"a":0}}</pre>
     */
    @XmlRootElement(name = "h")
    static class H {
        private int a;

        private String b;

        H() {
        }

        @XmlElement(name = "a")
        int someStrangeMethodName1() {
            return a;
        }

        @XmlElement(name = "b")
        String someStrangeMethodName2() {
            return b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }

    /**
     * Всё работает, но как???
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code I{a=13, b='b-value'}}</pre>
     * <p>
     * Как это работает? Это смешанная Jackson-JAXB-схема:
     * <ul>
     *     <li>сначала находится конструктор, который можно вызывать &mdash;
     *         это конструктор размеченный аннотацией {@literal @}ConstructorProperties.
     *     <li>Этому конструктору нужно два свойства <tt>{@code a}</tt> и <tt>{@code g}</tt>.
     *         В JSON-объекте есть ствойство <tt>{@code a}</tt>, но нет свойства <tt>{@code g}</tt>,
     *         для <tt>{@code g}</tt> используется дефолтное значение.
     *         В итоге конструктор вызывается с аргументами <tt>{@code 13, null}</tt>.
     *     <li>для каждого свойства из JSON-объекта, которое не использовалось в вызове конструктора
     *         (такое свойство осталось одно: <tt>{@code b}</tt>)
     *         дёргается соответствующий setter у бина, или
     *         сеттится соотвествующее поле класса (даже несмотря на то, что оно <tt>{@code private final}</tt>).
     *         Но здесь же аннотация весит на геттере, а сеттера нет???
     *         Jackson видит аннотацию на геттере и ищет поле
     *         с таким же именем, как геттер, либо с таким же именем, как геттер, но без префикса <tt>get</tt> и
     *         использует это поля для установки значения!!!
     * </ul>
     * <p>
     * Повторная сериализация:
     * <pre>{@code {"a":13,"b":"b-value"}}</pre>
     */
    @XmlRootElement(name = "h")
    static class I {
        private final int a;

        private final String b;

        @ConstructorProperties({"a", "g"})
        I(int a, String b) {
            this.a = a;
            this.b = b;
        }

        @XmlElement(name = "a")
        int a() {
            return a;
        }

        @XmlElement(name = "b")
        String b() {
            return b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }


    /**
     * Всё работает, с определёнными странностями
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code J{a=42, b='b-value'}}</pre>
     * <p>
     * Как это работает? Это смешанная Jackson-JAXB-схема:
     * <ul>
     *     <li>сначала находится конструктор, который можно вызывать &mdash;
     *         это конструктор размеченный аннотацией {@literal @}ConstructorProperties.
     *     <li>Этому конструктору нужно два свойства <tt>{@code a}</tt> и <tt>{@code g}</tt>.
     *         В JSON-объекте есть ствойство <tt>{@code a}</tt>, но нет свойства <tt>{@code g}</tt>,
     *         для <tt>{@code g}</tt> используется дефолтное значение.
     *         В итоге конструктор вызывается с аргументами <tt>{@code 13, null}</tt>.
     *         Внутри конструктора поле <tt>{@code a}</tt> выставляется в значение <tt>{@code 42}</tt>,
     *         несмотря на то, что в качестве параметра нам передали 13.
     *     <li>для каждого свойства из JSON-объекта, которое не использовалось в вызове конструктора
     *         (такое свойство осталось одно: <tt>{@code b}</tt>)
     *         дёргается соответствующий setter у бина, или
     *         сеттится соотвествующее поле класса (даже несмотря на то, что оно <tt>{@code private final}</tt>).
     *         Но здесь же аннотация весит на геттере, а сеттера нет???
     *         Jackson видит аннотацию на геттере и ищет поле
     *         с таким же именем, как геттер, либо с таким же именем, как геттер, но без префикса <tt>get</tt> и
     *         использует это поля для установки значения!!!
     *         Поле <tt>{@code a}</tt> не переписывается, если оно уже было в конструкторе!!!
     * </ul>
     * <p>
     * Повторная сериализация:
     * <pre>{@code {"a":42,"b":"b-value"}}</pre>
     */
    @XmlRootElement(name = "h")
    static class J {
        private final int a;

        private final String b;

        @ConstructorProperties({"a", "g"})
        J(int a, String b) {
            this.a = 42;
            this.b = b;
        }

        @XmlElement(name = "a")
        int a() {
            return a;
        }

        @XmlElement(name = "b")
        String b() {
            return b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }

    /**
     * Всё работает
     *
     * JSON:
     * <pre>{@code {"a": 13, "b": "b-value"}}</pre>
     * <p>
     * Результат:
     * <pre>{@code K{a=13, b='b-value'}}</pre>
     * <p>
     * Как это работает? Это смешанная Jackson-JAXB-схема:
     * <ul>
     *     <li>сначала находится конструктор, который можно вызывать &mdash;
     *         это конструктор размеченный аннотацией {@literal @}ConstructorProperties.
     *     <li>Этому конструктору нужно два свойства <tt>{@code i}</tt> и <tt>{@code g}</tt>.
     *         В JSON-объекте нет свойств <tt>{@code i}</tt> и <tt>{@code g}</tt>,
     *         используется дефолтное значение.
     *         В итоге конструктор вызывается с аргументами <tt>{@code 0, null}</tt>.
     *         Внутри конструктора поле <tt>{@code a}</tt> выставляется в значение <tt>{@code 42}</tt>,
     *         несмотря на то, что в качестве параметра нам передали 0.
     *     <li>для каждого свойства из JSON-объекта, которое не использовалось в вызове конструктора
     *         (таких свойств два: <tt>{@code a}</tt> и <tt>{@code b}</tt>)
     *         дёргается соответствующий setter у бина, или
     *         сеттится соотвествующее поле класса (даже несмотря на то, что оно <tt>{@code private final}</tt>).
     *         Но здесь же аннотация весит на геттере, а сеттера нет???
     *         Jackson видит аннотацию на геттере и ищет поле
     *         с таким же именем, как геттер, либо с таким же именем, как геттер, но без префикса <tt>get</tt> и
     *         использует это поля для установки значения!!!
     *         Поле <tt>{@code a}</tt> перезаписывается, уже после работы конструктора, несмотря на то, что
     *         в конструкторе оно вставилось в другое значение, несмотря на то, что
     *         оно если оно <tt>{@code private final}</tt>!!!
     * </ul>
     * <p>
     * Повторная сериализация:
     * <pre>{@code {"a":13,"b":"b-value"}}</pre>
     */
    @XmlRootElement(name = "h")
    static class K {
        private final int a;

        private final String b;

        @ConstructorProperties({"i", "g"})
        K(int a, String b) {
            this.a = 42;
            this.b = b;
        }

        @XmlElement(name = "a")
        int a() {
            return a;
        }

        @XmlElement(name = "b")
        String b() {
            return b;
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + "{" + "a=" + a + ", b='" + b + '\'' + '}';
        }
    }
}
