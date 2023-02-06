package ru.yandex.market.stat.dicts.loaders.jdbc.db;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeEntityNodeImpl;
import ru.yandex.market.stat.dicts.common.ConversionStrategy;
import ru.yandex.market.stat.dicts.records.DictionaryRecord;
import ru.yandex.market.stat.dicts.utils.YtServiceUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class JsonYtTypesTest {

    private static final YTreeEntityNodeImpl NULL_NODE = new YTreeEntityNodeImpl(Cf.map());

    @Test
    public void testParseJsonListToStruct() {
        String stringFromObjectField = "[{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\"}]";
        JsonArrayYtType result = JsonArrayYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(1));
        assertThat(result.getData().get(0).size(), equalTo(2));
        assertThat(result.getData().get(0).get("source"), equalTo("PARTNER"));
        assertThat(result.getData().get(0).get("barcode"), equalTo("2867022358810"));
    }

    @Test
    public void testParseBadJsonListToString() {
        String stringFromPgResultset = "[{\"source\": \"PARTNER\", \"barcode\": \"2867022358810}]";
        JsonArrayYtType result = JsonArrayYtType.parse(stringFromPgResultset);
        assertNull(result.getData());
        assertThat(result.getDataAsString(), equalTo(result.getStructOrString()));
        assertThat(result.getStructOrString(), equalTo(stringFromPgResultset));
    }

    @Test
    public void testParseNullJsonListToStruct() {
        String stringFromObjectField = null;
        JsonArrayYtType result = JsonArrayYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(0));
        assertThat(result.getStructOrString(), equalTo(Collections.emptyList()));
        assertNull(result.getDataAsString());
    }

    @Test
    public void testParseStringNullJsonListToStruct() {
        String stringFromObjectField = "null";
        JsonArrayYtType result = JsonArrayYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(0));
        assertThat(result.getStructOrString(), equalTo(Collections.emptyList()));
        assertNull(result.getDataAsString());
    }

    @Test
    public void testParseEmptyJsonListToStruct() {
        String stringFromObjectField = JsonArrayYtType.EMPTY;
        JsonArrayYtType result = JsonArrayYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(0));
        assertThat(result.getStructOrString(), equalTo(Collections.emptyList()));
        assertNull(result.getDataAsString());
    }

    @Test
    public void testParseJsonToStruct() {
        String stringFromObjectField = "{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\", \"the_answer\": 42}";
        JsonYtType result = JsonYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(3));
        assertThat(result.getData().get("source"), equalTo("PARTNER"));
        assertThat(result.getData().get("barcode"), equalTo("2867022358810"));
        assertThat(result.getData().get("the_answer"), equalTo(42));
    }

    @Test
    public void testParseBadJsonToString() {
        String stringFromObjectField = "{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\", \"the_answer\": 42";
        JsonYtType result = JsonYtType.parse(stringFromObjectField);
        assertNull(result.getData());
        assertThat(result.getDataAsString(), equalTo(result.getStructOrString()));
        assertThat(result.getStructOrString(), equalTo(stringFromObjectField));
    }

    @Test
    public void testParseNullJsonToStruct() {
        String stringFromObjectField = null;
        JsonYtType result = JsonYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(0));
        assertThat(result.getStructOrString(), equalTo(Collections.emptyMap()));
        assertNull(result.getDataAsString());
    }


    @Test
    public void testParseEmptyJsonToStruct() {
        String stringFromObjectField = JsonYtType.EMPTY;
        JsonYtType result = JsonYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(0));
        assertThat(result.getStructOrString(), equalTo(Collections.emptyMap()));
        assertNull(result.getDataAsString());
    }

    @Test
    public void testParseNullStringJsonToStruct() {
        String stringFromObjectField = "null";
        JsonYtType result = JsonYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(0));
        assertThat(result.getStructOrString(), equalTo(Collections.emptyMap()));
        assertNull(result.getDataAsString());
    }

    @Test
    public void testParseJsonWithNullToStruct() {
        String stringFromObjectField = "{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\", \"the_answer\": null}";
        JsonYtType result = JsonYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(3));
        assertThat(result.getData().get("source"), equalTo("PARTNER"));
        assertThat(result.getData().get("barcode"), equalTo("2867022358810"));
        assertThat(result.getData().get("the_answer"), equalTo(NULL_NODE));
    }

    @Test
    public void testParseComplexJsonToStruct() {
        String stringFromObjectField = "{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\", " +
                "\"the_answer\": {\"id\": 42, \"reason\":\"unknown\"}}";
        JsonYtType result = JsonYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(3));
        assertThat(result.getData().get("source"), equalTo("PARTNER"));
        assertThat(result.getData().get("barcode").toString(), equalTo("2867022358810"));
        assertThat(result.getData().get("the_answer"), equalTo(ImmutableMap.of("id", 42, "reason", "unknown")));
    }

    @Test
    public void testParseComplexJsonWithNullToStruct() {
        String stringFromObjectField = "{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\", " +
                "\"the_answer\": {\"id\": 42, \"reason\": { \"first\": \"blabla\", \"second\": null, \"third\": {\"value\": null}}}}";
        JsonYtType result = JsonYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(3));
        assertThat(result.getData().get("source"), equalTo("PARTNER"));
        assertThat(result.getData().get("barcode"), equalTo("2867022358810"));
        assertThat(((Map) result.getData().get("the_answer")).size(), equalTo(2));
        assertThat(((Map) ((Map) result.getData().get("the_answer")).get("reason")).size(), equalTo(3));
        assertThat((((Map) ((Map) result.getData().get("the_answer")).get("reason")).get("second")), equalTo(NULL_NODE));

        Map<String, Object> expectedAnswer = new HashMap<>();
        Map<String, Object> reason = new HashMap<>();
        Map<String, Object> third = new HashMap<>();
        third.put("value", NULL_NODE);
        reason.put("first", "blabla");
        reason.put("second", NULL_NODE);
        reason.put("third", third);
        expectedAnswer.put("id", 42);
        expectedAnswer.put("reason", reason);
        assertThat(result.getData().get("the_answer").toString(), equalTo(expectedAnswer.toString()));

    }

    @Test
    public void testParseVeryComplexJsonToStruct() {
        String stringFromObjectField =
                "{\"item_id\": " +
                        "{\"shop_sku\": \"ADD3000000000000007827\", \"supplier_id\": 1339}, " +
                        "\"remaining_information\": [" +
                        "{\"source\": {\"id\": \"171\", \"type\": \"WAREHOUSE\"}, " +
                        "\"shipping_configuration\": [" +
                        "{\"item_unit\": " +
                        "{\"weights\": " +
                        "{\"weight_net_mg\":  {\"updated_ts\": 1000}, \"weight_tare_mg\": {\"updated_ts\": 1000}, " +
                        "\"weight_gross_mg\": {\"updated_ts\": 1000}}, " +
                        "\"dimensions\": " +
                        "{\"width_micrometer\": {\"updated_ts\": 1000}, \"height_micrometer\": {\"updated_ts\": 1000}, " +
                        "\"length_micrometer\": {\"updated_ts\": 1000}}}}]}, " +
                        "{\"source\": {\"id\": \"147\", \"type\": \"WAREHOUSE\"}, " +
                        "\"shipping_configuration\": [" +
                        "{\"item_unit\": " +
                        "{\"weights\": " +
                        "{\"weight_net_mg\": {\"updated_ts\": 1000}, \"weight_tare_mg\": {\"updated_ts\": 1000}, " +
                        "\"weight_gross_mg\": {\"updated_ts\": 1000}}, " +
                        "\"dimensions\": " +
                        "{\"width_micrometer\": {\"updated_ts\": 1000}, \"height_micrometer\": {\"updated_ts\": 1000}, " +
                        "\"length_micrometer\": {\"updated_ts\": 1000}}}}" +
                        "]" +
                        "}" +
                        "]" +
                        "}\n";
        JsonYtType result = JsonYtType.parse(stringFromObjectField);
        assertThat(result.getData().size(), equalTo(2));
        assertThat(((Map<String, Object>) result.getData().get("item_id")).get("supplier_id"), equalTo(1339));

        Map<String, Object> remainingInformation =
                ((List<Map<String, Object>>) result.getData().get("remaining_information")).get(0);
        Map<String, Object> shippingConfiguration =
                ((List<Map<String, Object>>) remainingInformation.get("shipping_configuration")).get(0);
        assertThat(shippingConfiguration.get("item_unit"), is(instanceOf(Map.class)));
        Map<String, Map> weights = ((Map<String, Map<String, Map>>) shippingConfiguration.get("item_unit")).get("weights");
        assertThat(weights.get("weight_gross_mg").get("updated_ts"), is(1000));
        Map<String, Object> source = ((Map<String, Object>) remainingInformation.get("source"));
        assertThat(source.get("id"), is("171"));


    }

    @Test
    public void testPrepareJsonForYt() {
        String json = "{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\", " +
                "\"the_answer\": {\"id\": 42, \"reason\": 43}}";
        String jsonArray = "[{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\"}]";
        TestRecord hello = new TestRecord(JsonYtType.parse(json), JsonArrayYtType.parse(jsonArray), "hello");
        YtServiceUtils.convertToYsonNode(hello, LocalDate.now(), ConversionStrategy.STANDARD);
    }

    @Test
    public void testPrepareJsonForYtWithNulls() {
        String json = "{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\", " +
                "\"the_answer\": {\"id\": 42, \"reason\": { \"first\": \"blabla\", \"second\": null, \"third\": {\"value\": null}}}}";
        String jsonArray = "[{\"source\": \"PARTNER\", \"barcode\": null}]";
        TestRecord hello = new TestRecord(JsonYtType.parse(json), JsonArrayYtType.parse(jsonArray), null);
        //тут раньше был exception
        YtServiceUtils.convertToYsonNode(hello, LocalDate.now(), ConversionStrategy.STANDARD);
    }

    @Test
    public void testPrepareJsonForYtWithNullsInList() {
        String json = "{\"source\": \"PARTNER\", \"barcode\": \"2867022358810\", " +
                "\"the_answer\": {\"id\": 42, \"reason\": { \"first\": \"blabla\", \"second\": null, \"third\": [{\"value\": null}]}}}";
        String jsonArray = "[{\"source\": \"PARTNER\", \"barcode\": null}]";
        TestRecord hello = new TestRecord(JsonYtType.parse(json), JsonArrayYtType.parse(jsonArray), null);
        //тут раньше был exception
        YtServiceUtils.convertToYsonNode(hello, LocalDate.now(), ConversionStrategy.STANDARD);
    }

    @AllArgsConstructor
    public class TestRecord implements DictionaryRecord {
        private JsonYtType jsonfield;
        private JsonArrayYtType jsonListfield;
        private String normalField;
    }
}
