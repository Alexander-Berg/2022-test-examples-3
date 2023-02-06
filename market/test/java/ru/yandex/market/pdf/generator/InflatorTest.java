package ru.yandex.market.pdf.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.market.pdf.generator.exceptions.InvalidParametersException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class InflatorTest {

    @Test
    public void inflateTemplateWithSingleParameter() {
        Inflator inflator = new HtmlTemplateInflator("samples/simple");
        String html = inflator.inflate(JsonParamsMapper.map("{\"name\":\"Thomas\"}"));
        assertThat(html, containsString("Thomas"));
    }

    @Test
    public void inflateTemplateWithItemsList() {
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
        assertThat(html, containsString("Name: <span>" + name + "</span>"));
        assertThat(html, containsString("Price: <span>" + price + "</span>"));
        assertThat(html, containsString("Count: <span>" + count + "</span>"));
    }

    private String generateItem(String name, Double price, Integer count) {
        return "  {\n" +
                "    \"name\": \"" + name + "\",\n" +
                "    \"price\": " + price + ",\n" +
                "    \"count\": " + count + "\n" +
                "  }";
    }

    @Test
    public void wrongJsonParams() {
        Assertions.assertThrows(InvalidParametersException.class, () -> {
            Inflator inflator = new HtmlTemplateInflator("samples/simple");
            inflator.inflate(JsonParamsMapper.map("{\"name\":\"Thomas\",}"));
        });
    }

}
