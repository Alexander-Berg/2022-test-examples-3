package ru.yandex.market.core.indexer.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampExplanation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.FunctionalTest;

/**
 * Date: 03.12.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class FeedLogJsonDeserializerTest extends FunctionalTest {

    @Autowired
    private FeedLogJsonDeserializer feedLogJsonDeserializer;

    @DisplayName("Десириализация. В случае корректного json получаем справочник")
    @Test
    void deserializeToMap_json_filledMap() {
        String json = StringTestUtil.getString(this.getClass(),
                "FeedLogJsonDeserializer/json/test.json");
        Map<String, String> map = feedLogJsonDeserializer.deserializeToMap(json);
        Assertions.assertThat(map)
                .hasSize(8)
                .containsEntry("attrName", "unit")
                .containsEntry("attrValue", "")
                .containsEntry("tagName", "true")
                .containsEntry("value", "100")
                .containsEntry("null", "null")
                .containsEntry("context", "[line 55, col 9]")
                .containsEntry("code", "35B")
                .containsEntry("tree", "");
    }

    @DisplayName("Десириализация. В случае json = null возвращается пустой справочник")
    @Test
    void deserializeToMap_null_emptyMap() {
        Assertions.assertThat(feedLogJsonDeserializer.deserializeToMap(null))
                .isEmpty();
    }

    @DisplayName("Десириализация. В случае некорекктного json возвращается пустой справочник")
    @Test
    void deserializeToMap_incorrectJson_emptyMap() {
        Assertions.assertThat(feedLogJsonDeserializer.deserializeToMap("{name:OZ}"))
                .isEmpty();
    }

    @DisplayName("Сериализация. В случае пустого справочника сохраняем только скобки")
    @Test
    void serializeMap_emptyMap_bracketJson() {
        Assertions.assertThat(feedLogJsonDeserializer.serializeMap(Collections.emptyMap()))
                .isEqualTo("{}");
    }

    @DisplayName("Сериализация. В случае справочника сохраняем json в строку")
    @Test
    void serializeMap_map_successful() {
        Map<String, String> map = Map.of(
                "attrName", "unit",
                "context", "[line 55, col 9]",
                "tree", ""
        );

        String json = feedLogJsonDeserializer.serializeMap(map);
        Map<String, String> result = feedLogJsonDeserializer.deserializeToMap(json);

        Assertions.assertThat(result)
                .hasSize(3)
                .containsEntry("attrName", "unit")
                .containsEntry("context", "[line 55, col 9]")
                .containsEntry("tree", "");
    }

    @DisplayName("Сериализация списка Explanation параметров в строку")
    @Test
    void serializeParams_toString_successful() {
        List<DataCampExplanation.Explanation.Param> params = List.of(
                DataCampExplanation.Explanation.Param.newBuilder()
                        .setName("param1")
                        .setValue("value1")
                        .build(),
                DataCampExplanation.Explanation.Param.newBuilder()
                        .setName("param_list")
                        .setValue("value_list_1")
                        .build(),
                DataCampExplanation.Explanation.Param.newBuilder()
                        .setName("param_list")
                        .setValue("value_list_2")
                        .build(),
                DataCampExplanation.Explanation.Param.newBuilder()
                        .setName("param_list")
                        .setValue("value_list_3")
                        .build()
        );

        String actual = feedLogJsonDeserializer.serializeExplanationParams(params);
        Assertions.assertThat(actual)
                .isEqualTo("{\"param1\":\"value1\",\"param_list\":[\"value_list_1\",\"value_list_2\",\"value_list_3\"]}");
    }

    @DisplayName("Конвертация списка Explanation параметров в Map")
    @Test
    void convertParams_toMap_successful() {
        List<DataCampExplanation.Explanation.Param> params = List.of(
                DataCampExplanation.Explanation.Param.newBuilder()
                        .setName("param1")
                        .setValue("value1")
                        .build(),
                DataCampExplanation.Explanation.Param.newBuilder()
                        .setName("param_list")
                        .setValue("value_list_1")
                        .build(),
                DataCampExplanation.Explanation.Param.newBuilder()
                        .setName("param_list")
                        .setValue("value_list_2")
                        .build(),
                DataCampExplanation.Explanation.Param.newBuilder()
                        .setName("param_list")
                        .setValue("value_list_3")
                        .build()
        );

        Map<String, Object> actual = feedLogJsonDeserializer.toMap(params);
        Map<String, Object> expected = Map.of(
                "param1", "value1",
                "param_list", List.of("value_list_1", "value_list_2", "value_list_3")
        );

        Assertions.assertThat(actual)
                .isEqualTo(expected);
    }
}
