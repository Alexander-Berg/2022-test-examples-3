package ru.yandex.vendor.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsfr.json.JsonPathListener;
import org.jsfr.json.JsonSurfer;
import org.junit.jupiter.api.Test;
import ru.yandex.vendor.report.brand_products.ProductTitles;
import ru.yandex.vendor.report.brand_products.ReportProduct;
import ru.yandex.vendor.report.brand_products.ReportProductPrices;


import java.io.*;
import java.util.Objects;
import java.util.Optional;

import static org.junit.Assert.*;
import static ru.yandex.vendor.util.JSurferUtil.*;

public class JSurferUtilTest {

    private JsonSurfer surfer = JsonSurfer.jackson();

    /**
     * В report.json - е ничего нет, даже скобок.
     * При работе getElement() на запрос, когда в пути ничего не передается возвращает весь объект, если он есть
     */
    @Test
    public void testGetElementFromNullNode() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetElementFromNullNode/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(Optional.empty(), getElement(node, "somePath"));
                    assertEquals(Optional.empty(), getElement(node, ""));
                    assertEquals(Optional.empty(), getElement(node));
                }
        ).buildAndSurf(reader);
    }

    /**
     * В  report.json - е только скобки {}
     */
    @Test
    public void testGetElementFromEmptyNode() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetElementFromEmptyNode/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(Optional.empty(), getElement(node));
                    assertEquals(Optional.empty(), getElement(node, "somePath"));
                    assertEquals(Optional.empty(), getElement(node, ""));
                }
        ).buildAndSurf(reader);
    }

    /**
     * В  report.json - е в скобках что-то есть
     * <p>
     * Проверяем, что метода не отдаст OptionalEmpty, если в первом уровне вложенности будут как и объекты так и параметры
     */
    @Test
    public void testGetElementFromNodeWithSomeValues() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetElementFromNodeWithSomeValues/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(node, getElement(node).get());
                    assertEquals(Optional.empty(), getElement(node, "somePath"));
                    assertEquals(Optional.empty(), getElement(node, ""));
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testGetElementFromEmptyPath() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetElementFromEmptyPath/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(Optional.empty(), getElement(node, "SomePath"));
                    assertEquals(Optional.empty(), getElement(node, ""));
                    assertEquals(node, getElement(node).get());
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testGetElemenForEmptyArray() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetElemenForEmptyArray/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(Optional.empty(), getElement(node, "mass1"));
                    assertEquals(Optional.empty(), getElement(node, "SomePath"));
                    assertEquals(Optional.empty(), getElement(node, ""));
                    assertEquals(node, getElement(node).get());
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testGetElemenForArray() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetElemenForArray/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(Optional.empty(), getElement(node, "mass1"));
                    assertEquals(Optional.empty(), getElement(node, "SomePath"));
                    assertEquals(Optional.empty(), getElement(node, ""));
                    assertEquals(node, getElement(node).get());
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testGetElementFromArray() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetElementFromArray/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(node.path("mass4"), getElement(node, "mass4").get());
                    assertEquals(node.path("mass2"), getElement(node, "mass2").get());// тут возвращается [[],[]] уместно ли это? и возможно ли?
                    assertEquals(node.path("mass3"), getElement(node, "mass3").get());
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testGetElementFromObject() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetElementFromObject/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(node.path("notMass1").findValue("one"), getElement(node, "notMass1", "one").get());
                    assertEquals(Optional.empty(), getElement(node, "notMass0"));
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testGetObjectFromArray() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetObjectFromArray/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(node.path("mass3"), getElement(node, "mass3").get());
                    assertEquals(node.path("mass3").findValue("two"), getElement(node, "mass3", "two").get());
                    assertEquals(node.path("mass3").findValue("undermass3"), getElement(node, "mass3", "undermass3").get());
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testGetArrayFromObject() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetArrayFromObject/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    assertEquals(node.path("notMass1").findValue("mass"), getElement(node, "notMass1", "mass").get());
                    assertEquals(node.path("notMass1").findValue("two"), getElement(node, "notMass1", "two").get());
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testTakeIntegerFromBodyBody() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testTakeIntegerFromBodyBody/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    ReportProduct reportProduct = new ReportProduct();
                    takeInteger(node, reportProduct::setVbid, "bids", "vbid", "value");
                    assertEquals(Optional.of(75), getInt(node, "bids", "vbid", "value"));
                    assertEquals(75, reportProduct.getVbid().intValue());
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testTakeIntegerForEmptyBodyBids() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testTakeIntegerForEmptyBodyBids/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    ReportProduct reportProduct = new ReportProduct();
                    takeInteger(node, reportProduct::setVbid, "bids", "vbid", "value");
                    assertNull(reportProduct.getVbid());
                    reportProduct.setVbid(getInt(node, "bids", "vbid", "value").orElse(0));
                    assertEquals(0, reportProduct.getVbid().intValue());
                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testTakeForReportProduct() {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testTakeForReportProduct/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;
                    ReportProduct reportProduct = new ReportProduct();
                    takeString(node, reportProduct::setEntity, "entity");
                    assertEquals("product", reportProduct.getEntity());

                    takeLong(node, reportProduct::setId, "id");
                    assertEquals(13327417L, reportProduct.getId().longValue());

                    takeLong(node, reportProduct::setHid, "hid");
                    assertEquals(15685457L, reportProduct.getHid().longValue());

                    takeInteger(node, reportProduct::setOpinions, "opinions");
                    assertEquals(0, reportProduct.getOpinions().intValue());

                    takeDouble(node, reportProduct::setRating, "rating");
                    assertEquals(0d, reportProduct.getRating(), 0);

                    ProductTitles productTitles = new ProductTitles();
                    takeString(node, productTitles::setRaw, "titles", "raw");
                    assertEquals("Корм для стерилизованных кошек Purina Pro Plan Sterilised для профилактики МКБ, с индейкой 10 кг", productTitles.getRaw());


                    ReportProductPrices prices = new ReportProductPrices();
                    takeInteger(node, prices::setMax, "prices", "max");
                    takeInteger(node, prices::setMin, "prices", "mon");
                    reportProduct.setPrices(prices);

                    assertEquals(6054, reportProduct.getPrices().getMax().intValue());
                    assertNull(reportProduct.getPrices().getMin());

                }
        ).buildAndSurf(reader);
    }

    @Test
    public void testGetElement() throws Exception {
        Reader reader = new InputStreamReader(Objects.requireNonNull(getTestInputStreamResource("/testGetElement/report.json")));
        surfer.configBuilder().bind(
                "$.search.results[*]",
                (JsonPathListener) (value, context) -> {
                    ObjectNode node = (ObjectNode) value;

                    assertEquals(((ObjectNode) value).path("mass1"), getElement(node, "mass1").get()); // должен вернуть массив .[]
                    assertEquals(((ObjectNode) value).path("lol1"), getElement(node, "lol1").get()); // должен вернуть значения из .{}
                    assertEquals(Optional.empty(), getElement(node, "id")); // несуществующий элемент .

                    assertEquals(((ObjectNode) value).path("mass1").findValue("mass2"),
                            getElement(node, "mass1", "mass2").get()); // должен вернуть массив из массива[.[]]


                    assertEquals(((ObjectNode) value).path("mass1").findValue("lol"),
                            getElement(node, "mass1", "lol").get()); // должен вернуть {} из массива[.{}]


                    assertEquals(((ObjectNode) value).path("lol1").findValue("mass2"),
                            getElement(node, "lol1", "mass2").get()); // должен вернуть массив из {}  {.[]}

                    assertEquals("mass1:lol:lol1", getString(node, "mass1", "lol", "lol1").get());//[{.}]
                    assertEquals("mass1:lol1", getString(node, "mass1", "lol1").get());//[.]
                    assertEquals("mass1:mass2:lol", getString(node, "mass1", "mass2", "lol").get());//[[.]]
                    assertEquals("lol", getString(node, "lol").get());//.
                    assertEquals("lol1:lol", getString(node, "lol1", "lol").get());//{.}
                    assertEquals("lol1:lol1", getString(node, "lol1", "lol1").get());//{.}
                    assertEquals("lol1:mass2:lol", getString(node, "lol1", "mass2", "lol").get());//{[.]}
                    assertEquals("lol1:mass2:results:lol", getString(node, "lol1", "mass2", "results", "lol").get());//{[[.]]}
                    assertEquals("lol1:mass2:results:results", getString(node, "lol1", "mass2", "results", "results").get());//{[[.]]}
                    assertEquals(Optional.empty(), getElement(null, "lol1", "mass2", "results", "results"));// node = null
                    assertEquals(Optional.empty(), getElement(null));// path.size() = 0 и node = null
                    assertEquals(node, getElement(node).get());// path.size() = 0
                    assertEquals(Optional.empty(), getElement(null));
                    assertEquals(((ObjectNode) value).path("mass1").findValue("lol"),
                            getElement(node, "mass1", "lol").get());
                }
        ).buildAndSurf(reader);
        reader.close();
    }

    private InputStream getTestInputStreamResource(String filename) {
        ClassLoader classLoader = getClass().getClassLoader();
        return classLoader.getResourceAsStream("ru/yandex/vendor/util/" + getClass().getSimpleName() + filename);
    }
}
