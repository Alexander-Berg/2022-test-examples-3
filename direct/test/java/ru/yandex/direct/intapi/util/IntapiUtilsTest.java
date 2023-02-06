package ru.yandex.direct.intapi.util;

import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.intapi.util.IntapiUtils.jsonAsMultiLineString;
import static ru.yandex.direct.intapi.util.IntapiUtils.node;

@Ignore("IntapiUtils - не продакшен код, не нужно тестировать при каждом коммите")
public class IntapiUtilsTest {

    @Test(expected = Test.None.class)
    public void test() {
        String expectedJson = "{\n" +
                "  \"testInt\" : 1,\n" +
                "  \"testLong\" : 2,\n" +
                "  \"inner1\" : {\n" +
                "    \"inner1TestInt\" : 3,\n" +
                "    \"inner1TestBoolean\" : false\n" +
                "  },\n" +
                "  \"testString\" : \"qwe\",\n" +
                "  \"inner2\" : {\n" +
                "    \"inner2testString\" : \"asd\"\n" +
                "  },\n" +
                "  \"testBoolean\" : true\n" +
                "}";

        String json = jsonAsMultiLineString(
                node("testInt", 1),
                node("testLong", 2L),
                node("inner1", node(
                        node("inner1TestInt", 3),
                        node("inner1TestBoolean", false)
                )),
                node("testString", "qwe"),
                node("inner2",
                        node("inner2testString", "asd")
                ),
                node("testBoolean", true)
        );
        System.out.println(json);

        assertThat(json).isEqualTo(expectedJson);
    }

}
