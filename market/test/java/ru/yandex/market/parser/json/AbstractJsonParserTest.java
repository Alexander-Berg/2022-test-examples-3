package ru.yandex.market.parser.json;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import org.junit.Assert;
import org.junit.Test;

public class AbstractJsonParserTest {
    static final String beanContent =
            "{ b: true, bb: false, ba: [true, false], \n" +
                    "bt: 121, btt: -1, bta: [11, 23, 255], \n" +
                    "c: 'ы', cc: 'Ы', ca: ['Ы', 'Ъ'], s: 'string', \n" +
                    "i: -17, ii: 31, ia: [11, 91, -73], ic: [-17, 31, -47, 0], \n" +
                    "d: 71.39, dd: 0.7135, da: [NaN, -7.382, 238], \n" +
                    "l: 17689045173458967, ll: -2435804687124564332, la: [315459544325547098, -230677656062546624], " +
                    "\n" +
                    "bi: 8768436528595490755640564656736525610542856674355467672564561313489, \n" +
                    "bd: 248764104468953067357245462468130123545675.78456515543764891236736773 \n" +
                    " }";

    static void assertBean(Bean bean) {
        Assert.assertEquals(true, bean.b);
        Assert.assertEquals(Boolean.FALSE, bean.bb);
        Assert.assertArrayEquals(new boolean[]{true, false}, bean.ba);

        Assert.assertTrue((byte) 121 == bean.bt);
        Assert.assertEquals(Byte.valueOf((byte) -1), bean.btt);
        Assert.assertArrayEquals(new byte[]{11, 23, -1}, bean.bta);

        Assert.assertEquals('ы', bean.c);
        Assert.assertEquals(new Character('Ы'), bean.cc);
        Assert.assertArrayEquals(new char[]{'Ы', 'Ъ'}, bean.ca);
        Assert.assertEquals("string", bean.s);

        Assert.assertEquals(-17, bean.i);
        Assert.assertEquals(Integer.valueOf(31), bean.ii);
        Assert.assertArrayEquals(new int[]{11, 91, -73}, bean.ia);
        Assert.assertEquals(Arrays.asList(-17, 31, -47, 0), bean.ic);

        Assert.assertEquals(71.39, bean.d, 0d);
        Assert.assertEquals(Double.valueOf(0.7135), bean.dd);
        Assert.assertArrayEquals(new double[]{Double.NaN, -7.382, +238d}, bean.da, 0d);

        Assert.assertEquals(17689045173458967L, bean.l);
        Assert.assertEquals(Long.valueOf(-2435804687124564332L), bean.ll);
        Assert.assertArrayEquals(new long[]{315459544325547098L, -230677656062546624L}, bean.la);

        Assert.assertEquals(new BigInteger("8768436528595490755640564656736525610542856674355467672564561313489"),
                bean.bi);
    }

    /**
     * Для пустого json нет событий
     */
    @Test
    public void emptyJson() {
        MyParser p = new MyParser();
        p.onJson("/", p.jsl);
        p.parse("");
        Assert.assertTrue(p.results.isEmpty());
    }

    /**
     * Проверка корректности работы парсера на скалярных значениях в корне документа
     */
    @Test
    public void scalarRootValues() {
        MyParser p = new MyParser();
        p.onValue("/", p.vl);
        Assert.assertEquals("NULL", p.parse("null"));
        Assert.assertEquals("TRUE", p.parse("true"));
        Assert.assertEquals("FALSE", p.parse("false"));
        Assert.assertEquals("some string", p.parse("\"some string\""));
        Assert.assertEquals("some string", p.parse("'some string'"));

        p.parse("00073926 230956.341");
        Assert.assertEquals(Arrays.asList(73926, 230956.341), p.results);
        p.parse("430987646590174655840903674675840836254174895043673816447596");
        Assert.assertEquals(Arrays.asList(new BigInteger(
                        "430987646590174655840903674675840836254174895043673816447596")),
                p.results);
    }

    /**
     * Проверка корректности работы преобразования значений парсера
     */
    @Test
    public void typedListeners() {
        Bean bean = new Bean();
        MyParser p = new MyParser();
        p.onTypedValue("/b", boolean.class, v -> bean.b = v);
        p.onTypedValue("/bb", Boolean.class, v -> bean.bb = v);
        p.onTypedValue("/ba", boolean[].class, v -> bean.ba = v);

        p.onTypedValue("/bt", byte.class, v -> bean.bt = v);
        p.onTypedValue("/btt", Byte.class, v -> bean.btt = v);
        p.onTypedValue("/bta", byte[].class, v -> bean.bta = v);

        p.onTypedValue("/c", char.class, v -> bean.c = v);
        p.onTypedValue("/cc", Character.class, v -> bean.cc = v);
        p.onTypedValue("/ca", char[].class, v -> bean.ca = v);
        p.onTypedValue("/s", String.class, v -> bean.s = v);

        p.onTypedValue("/i", int.class, v -> bean.i = v);
        p.onTypedValue("/ii", Integer.class, v -> bean.ii = v);
        p.onTypedValue("/ia", int[].class, v -> bean.ia = v);
        p.onTypedValue("/ic", Collection.class, v -> bean.ic = v);

        p.onTypedValue("/d", double.class, v -> bean.d = v);
        p.onTypedValue("/dd", Double.class, v -> bean.dd = v);
        p.onTypedValue("/da", double[].class, v -> bean.da = v);

        p.onTypedValue("/l", long.class, v -> bean.l = v);
        p.onTypedValue("/ll", Long.class, v -> bean.ll = v);
        p.onTypedValue("/la", long[].class, v -> bean.la = v);

        p.onTypedValue("/bi", BigInteger.class, v -> bean.bi = v);
        p.onTypedValue("/bd", BigDecimal.class, v -> bean.bd = v);

        //p.onTypedValue("/ic", IntArrayList.class, v -> bean.ic = v);
        p.parse(beanContent);
        assertBean(bean);
        Assert.assertEquals(new BigDecimal("248764104468953067357245462468130123545675.78456515543764891236736773"),
                bean.bd);
    }

    /**
     * Проверка корректности работы парсера на структурных значениях в корне документа
     */
    @Test
    public void structureRootValues() {
        MyParser p = new MyParser();
        p.onJson("/", p.jsl);
        Assert.assertEquals("NULL", p.parse("null"));
        Assert.assertEquals("[ARR]", p.parse(" [ ] "));
        Assert.assertEquals("{OBJ}", p.parse(" { } "));
        Assert.assertEquals("[ARR][ARR]", p.parse(" [ ] [] "));
        Assert.assertEquals("{OBJ}{OBJ}", p.parse(" {} { } "));
        Assert.assertEquals("{OBJ}[ARR]", p.parse(" {} [] "));
        Assert.assertEquals("{OBJ}", p.parse(" { a:[] } "));
        Assert.assertEquals("[ARR]", p.parse(" [ { } ] "));
    }

    /**
     * Вариации с именем поля
     */
    @Test
    public void fieldName() {
        MyParser p = new MyParser();
        p.onValue("/text", p.vl);
        p.onValue("/field_name-with.some\\special=chars!@#$%^&*()+?\"<>", p.vl);
        p.onValue("/field{name}with[some]markup.chars,:''\"", p.vl);
        p.onValue("/Имя поля на русском", p.vl);

        Assert.assertEquals("field name in double qoutes", p.parse("{\n  \"text\" : 'field name in double qoutes'\n}"));
        Assert.assertEquals("field name in single qoutes", p.parse("{\n  'text':\"field name in single qoutes\"\n}"));
        Assert.assertEquals("field name without qoutes", p.parse("{text:'field name without qoutes'}"));
        Assert.assertEquals("field name with some special chars!",
                p.parse("{'field_name-with.some\\\\special=chars!@#$%^&*()+?\"<>':'field name with some special " +
                        "chars!'}"));
        Assert.assertEquals("field name with some markup chars",
                p.parse("{ \"field{name}with[some]markup.chars,:''\\\"\" : 'field name with some markup chars'}"));
        Assert.assertEquals("Имя поля на русском",
                p.parse("{ \"Имя поля на русском\" : 'Имя поля на русском'}"));
    }

    /**
     * Проверка корректности работы парсера на вложенных структурах
     */
    @Test
    public void nestedStructured() {
        MyParser p = new MyParser();
        p.onJson("/", p.jsl);
        p.onJson("/[*]", p.jsl);
        p.onJson("/[*]/[*]", p.jsl);
        p.onJson("/[*]/[*]/[*]", p.jsl);
        p.onJson("/a", p.jsl);
        p.onJson("/a/b", p.jsl);
        p.onJson("/a/b/c", p.jsl);
        p.onJson("/a/[*]", p.jsl);
        p.onJson("/a/[*]/b", p.jsl);
        p.onJson("/a/[*]/b/[*]", p.jsl);
        p.onJson("/a/[*]/b/[*]/c", p.jsl);

        Assert.assertEquals("[{OBJ}ARR]", p.parse(" [ { } ] "));
        Assert.assertEquals("[{OBJ}[ARR] string ARR]", p.parse("[ {}, [], ' string ' ]"));
        Assert.assertEquals("{{{ string OBJ}OBJ}OBJ}", p.parse("{ a : { b : { c : ' string ' } } }"));
        Assert.assertEquals("[[[ deep3 ARR] deep-2 ARR] deep-1 ARR]", p.parse("[[[' deep3 '], ' deep-2 '], ' deep-1 " +
                "']"));
        Assert.assertEquals("{[{[{ string OBJ}ARR]OBJ}ARR]OBJ}", p.parse("{ a : [ { b: [ { c: ' string '} ] } ] }"));

    }

    /**
     * Проверка корректности работы со вложенными парсерами
     */
    @Test
    public void withSubParsers() {

        class MyParser1 extends MyParser {
            {
                onJson("/", this.jsl);
                onJson("/[*]", this.jsl);
                onJson("/c", this.jsl);
            }
        }

        class MyParser2 extends MyParser {
            {
                onJson("/", jsl);
                onValue("/[*]", new MyParser1(), v -> setResult(v));
                onValue("/b", new MyParser1(), v -> setResult(v));
            }
        }

        MyParser p3 = new MyParser();
        p3.onJson("/", p3.jsl);
        p3.onValue("/[*]", new MyParser2(), v -> p3.setResult(v));
        p3.onValue("/a", new MyParser2(), v -> p3.setResult(v));

        MyParser1 p1 = new MyParser1();
        MyParser2 p2 = new MyParser2();
        MyParser p4 = new MyParser();
        p4.onJson("/", p4.jsl);
        p4.onJson("/a", p4.jsl);
        p4.onValue("/a/[*]", p2, v -> p4.setResult("(P2R)"));
        p4.onValue("/a/[*]/b/[*]", p1, v -> p4.setResult("(P1R)"));

        Assert.assertEquals("[{OBJ}ARR]", p3.parse(" [ {} ] "));
        Assert.assertEquals("[{OBJ}[ARR] string ARR]", p3.parse("[ {}, [], ' string ' ]"));
        Assert.assertEquals("{{{ string OBJ}OBJ}OBJ}", p3.parse("{ a : { b : { c : ' string ' } } }"));
        Assert.assertEquals("[[[ deep3 ARR] deep-2 ARR] deep-1 ARR]", p3.parse("[[[' deep3 '], ' deep-2 '], ' deep-1 " +
                "']"));
        Assert.assertEquals("{[(P1R)(P2R)ARR]OBJ}", p4.parse("{ a : [ { b: [ { c: ' string '} ] } ] }"));
        Assert.assertEquals("{[{OBJ}ARR]OBJ}", p2.getParsed());
        Assert.assertEquals("{ string OBJ}", p1.getParsed());

    }

    /**
     * Проверка корректности работы парсера с комментариями в Json
     */
    @Test
    public void comments() {
        MyParser p = new MyParser();
        p.onJson("/", p.jsl);
        p.parse("/* {} */\n" +
                "// []");
        Assert.assertTrue(p.results.isEmpty());
    }

    /**
     * Проверка корректности работы парсера
     */
    @Test
    public void arrayIndex() {
        MyParser p = new MyParser();
        p.onJson("/[0]", p.jsl);
        p.onJson("/[2]", p.jsl);
        p.onJson("/[0]/[0]", p.jsl);
        p.onJson("/[0]/[3]", p.jsl);
        p.onJson("/a/[0]/b", p.jsl);
        p.onJson("/a", p.jsl);

        Assert.assertEquals("{OBJ}", p.parse(" [ {} ] "));
        Assert.assertEquals("02", p.parse(" [ '0', '1', '2', '3' ] "));
        Assert.assertEquals("[03ARR]", p.parse(" [ [ '0', '1', '2', '3' ] ] "));
        Assert.assertEquals("[ STR ARR]", p.parse(" { a: [ { b: ' STR ' }, {} ] } "));
    }

    /**
     * Проверка корректности работы парсера c объектами биндинга
     */
    @Test
    public void bindedObjects() {
        MyParser p = new MyParser();
        p.onTypedValue("/[*]", Bean.class, bean -> p.setResult(bean));
        Bean bean = (Bean) p.extract("[ " + beanContent + " ]");
        assertBean(bean);
        Assert.assertEquals(BigDecimal.valueOf(
                Double.parseDouble("248764104468953067357245462468130123545675.78456515543764891236736773")), bean.bd);
    }

    public static class Bean {

        public boolean b;
        public Boolean bb;
        public boolean[] ba;

        public byte bt;
        public Byte btt;
        public byte[] bta;

        public char c;
        public Character cc;
        public char[] ca;
        public String s;

        public int i;
        public int[] ia;
        public Integer ii;
        public Collection<Integer> ic;

        public double d;
        public Double dd;
        public double[] da;

        public long l;
        public Long ll;
        public long[] la;

        public BigInteger bi;
        public BigDecimal bd;
    }

    class MyParser extends AbstractJsonParser<String> {

        List<Object> results = new ArrayList();
        StringBuilder sb = new StringBuilder();
        public final StartJsonListener stl = b -> setResult(b ? "{" : "[");
        public final EndJsonListener enl = b -> setResult(b ? "}" : "]");
        public final ValueJsonListener<JsonNode> vl = v -> {
            Object result = v.isNull() ? "NULL" :
                    v.isBoolean() ? (v.booleanValue() ? "TRUE" : "FALSE") :
                            v.isObject() ? "OBJ" : v.isArray() ? "ARR" :
                                    v.isTextual() ? v.textValue() :
                                            v.isNumber() ? v.numberValue() : "OTHER";
            setResult(result);
        };
        public final ElementJsonListener<JsonNode> jsl = new ElementJsonListener<JsonNode>() {

            @Override
            public void onEnd(boolean isObject) {
                enl.onEnd(isObject);
            }

            @Override
            public void onStart(boolean isObject) {
                stl.onStart(isObject);
            }

            @Override
            public void onValue(JsonNode jsonValue) {
                vl.onValue(jsonValue);
            }
        };

        @Override
        protected void resetImpl() {
            results.clear();
            sb.setLength(0);
        }

        public void setResult(Object result, String... views) {
            this.results.add(result);
            if (views.length == 0 && result instanceof CharSequence) {
                sb.append((CharSequence) result);
            } else {
                for (String view : views) {
                    sb.append(view);
                }
            }
        }

        @Override
        public String toString() {
            return sb.toString();
        }

        @Override
        public String getParsed() {
            return sb.toString();
        }

        public String parse(String s) {
            return super.parse(s.getBytes(Charsets.UTF_8));
        }

        public Object extract(String s) {
            super.parse(s.getBytes(Charsets.UTF_8));
            return results.size() > 1 ? results : results.isEmpty() ? null : results.get(0);
        }
    }
}
