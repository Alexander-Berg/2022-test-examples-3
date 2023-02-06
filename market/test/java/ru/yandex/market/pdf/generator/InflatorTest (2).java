package ru.yandex.market.pdf.generator;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pdf.generator.exceptions.InvalidParametersException;

import static org.assertj.core.api.HamcrestCondition.matching;
import static org.hamcrest.core.StringContains.containsString;

class InflatorTest extends BaseTest {

    @Test
    void inflateTemplateWithSingleParameter() {
        Inflator inflator = new HtmlTemplateInflator("samples/simple");
        String html = inflator.inflate(JsonParamsMapper.map("{\"name\":\"Thomas\"}"));
        softly.assertThat(html).is(matching(containsString("Thomas")));
    }

    @Test
    void inflateTemplateWithItemsList() {
        Inflator inflator = new HtmlTemplateInflator("samples/iteration");
        String jsonParams = "{\"items\":[\n" +
            generateItem("IPhone", 99999.99, 1) + ",\n" +
            generateItem("Samsung", 88888.88, 1) + ",\n" +
            generateItem("Xiaomi", 33333.33, 2) + "]}";
        String html = inflator.inflate(JsonParamsMapper.map(jsonParams));
        assertCountainsItem(html, "IPhone", 99999.99, 1);
        assertCountainsItem(html, "Samsung", 88888.88, 1);
        assertCountainsItem(html, "Xiaomi", 33333.33, 2);
    }

    private void assertCountainsItem(String html, String name, Double price, Integer count) {
        softly.assertThat(html).is(matching(containsString("Name: <span>" + name + "</span>")));
        softly.assertThat(html).is(matching(containsString("Price: <span>" + price + "</span>")));
        softly.assertThat(html).is(matching(containsString("Count: <span>" + count + "</span>")));
    }

    private String generateItem(String name, Double price, Integer count) {
        return "  {\n" +
            "    \"name\": \"" + name + "\",\n" +
            "    \"price\": " + price + ",\n" +
            "    \"count\": " + count + "\n" +
            "  }";
    }

    @Test
    void wrongJsonParams() {
        Inflator inflator = new HtmlTemplateInflator("samples/simple");
        softly.assertThatThrownBy(
            () -> inflator.inflate(JsonParamsMapper.map("{\"name\":\"Thomas\",}"))
        ).isInstanceOf(InvalidParametersException.class);
    }

}
