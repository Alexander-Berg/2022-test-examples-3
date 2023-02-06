package ru.yandex.direct.libs.collections.model.serpdata;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpData.fromMap;

public class CollectionSerpDataTest {

    private static final String TEST_NORMALIZED_JSON_FILE = "test_normalized_serp_data.json";
    private static final String TEST_NORMALIZED_EXTRA_FIELDS_JSON_FILE = "test_normalized_extra_fields_serp_data.json";

    private String testNormalizedJson;
    private String testNormalizedExtraFieldsJson;
    private Map<String, Object> serpDataMap;
    private Map<String, Object> itemMap;
    private Map<String, Object> itemMap2;

    @Before
    public void before() throws IOException {
        testNormalizedJson = IOUtils.toString(getClass().getResourceAsStream(TEST_NORMALIZED_JSON_FILE), UTF_8).strip();
        testNormalizedExtraFieldsJson = IOUtils.toString(getClass()
                .getResourceAsStream(TEST_NORMALIZED_EXTRA_FIELDS_JSON_FILE), UTF_8).strip();
        serpDataMap = getCollectionMap();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) serpDataMap.get("items");
        itemMap = items.get(0);
        itemMap2 = items.get(1);
    }

    @Test
    public void fromMap_checkCollection() {
        CollectionSerpData serpData = fromMap(serpDataMap);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(serpData.getName()).isEqualTo(serpDataMap.get("name"));
            softly.assertThat(serpData.getUrl()).isEqualTo(serpDataMap.get("url"));
            softly.assertThat(serpData.getThumbId()).isEqualTo(serpDataMap.get("thumb_id"));
            softly.assertThat(serpData.getCardsCount()).isEqualTo(serpDataMap.get("cards_count"));
            softly.assertThat(serpData.getId()).isEqualTo(serpDataMap.get("id"));
            softly.assertThat(serpData.getItems()).hasSize(2);
            softly.assertThat(serpData.getNormalizedJson()).isEqualTo(testNormalizedJson);
        });
    }

    @Test
    public void fromMap_checkCollectionItems() {
        CollectionSerpData serpData = fromMap(serpDataMap);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(serpData.getItems()).hasSize(2);
            softly.assertThat(serpData.getNormalizedJson()).isEqualTo(testNormalizedJson);
            // first item
            softly.assertThat(serpData.getItems().get(0).getUrl()).isEqualTo(itemMap.get("url"));
            softly.assertThat(serpData.getItems().get(0).getText()).isEqualTo(itemMap.get("text"));
            softly.assertThat(serpData.getItems().get(0).getPos()).isEqualTo(itemMap.get("pos"));
            softly.assertThat(serpData.getItems().get(0).getImageSrc()).isEqualTo(itemMap.get("image_src"));
            softly.assertThat(serpData.getItems().get(0).getCardId()).isEqualTo(itemMap.get("card_id"));
            // second item
            softly.assertThat(serpData.getItems().get(1).getUrl()).isEqualTo(itemMap2.get("url"));
            softly.assertThat(serpData.getItems().get(1).getText()).isEqualTo(itemMap2.get("text"));
            softly.assertThat(serpData.getItems().get(1).getPos()).isEqualTo(itemMap2.get("pos"));
            softly.assertThat(serpData.getItems().get(1).getImageSrc()).isEqualTo(itemMap2.get("image_src"));
            softly.assertThat(serpData.getItems().get(1).getCardId()).isEqualTo(itemMap2.get("card_id"));
        });
    }

    @Test
    public void fromMap_checkEmptyCollection() {
        CollectionSerpData serpData = fromMap(Collections.emptyMap());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(serpData.getName()).isEqualTo(null);
            softly.assertThat(serpData.getUrl()).isEqualTo(null);
            softly.assertThat(serpData.getThumbId()).isEqualTo(null);
            softly.assertThat(serpData.getCardsCount()).isEqualTo(0);
            softly.assertThat(serpData.getId()).isEqualTo(null);
            softly.assertThat(serpData.getItems()).isEqualTo(null);
            softly.assertThat(serpData.getNormalizedJson()).isEqualTo("{}");
        });
    }

    @Test
    public void fromMap_checkCollectionEmptyItem() {
        Map<String, Object> emptySerpDataMap = Map.of("items", List.of(Collections.emptyMap()));
        CollectionSerpData serpData = fromMap(emptySerpDataMap);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(serpData.getItems()).hasSize(1);
            softly.assertThat(serpData.getNormalizedJson()).isEqualTo("{\"items\":[{}]}");
            softly.assertThat(serpData.getItems().get(0).getUrl()).isEqualTo(null);
            softly.assertThat(serpData.getItems().get(0).getText()).isEqualTo(null);
            softly.assertThat(serpData.getItems().get(0).getPos()).isEqualTo(null);
            softly.assertThat(serpData.getItems().get(0).getImageSrc()).isEqualTo(null);
            softly.assertThat(serpData.getItems().get(0).getCardId()).isEqualTo(null);
        });
    }

    @Test
    public void fromMap_checkCollectionWithExtraFields() {
        Map<String, Object> extraSerpDataMap = new HashMap<>(serpDataMap);
        extraSerpDataMap.put("extra_key", "extra_value");

        Map<String, Object> extraItemMap = new HashMap<>(itemMap);
        extraItemMap.put("extra_item_key", "extra_item_value");
        List<Map<String, Object>> itemMapsList = List.of(extraItemMap, itemMap2);
        extraSerpDataMap.put("items", itemMapsList);

        CollectionSerpData serpData = fromMap(extraSerpDataMap);
        assertThat(serpData.getNormalizedJson()).isEqualTo(testNormalizedExtraFieldsJson);
    }

    private Map<String, Object> getCollectionMap() {
        List<Map<String, Object>> items = List.of(
                Map.of(
                        "url", "http://some-item-url.ru",
                        "text", "Some item text",
                        "pos", 0,
                        "image_src", "http://some-item-src.ru",
                        "card_id", "some_item_card_id"
                ), Map.of(
                        "url", "http://some-item2-url.ru",
                        "text", "Some item 2 text",
                        "pos", 1,
                        "image_src", "http://some-item2-src.ru",
                        "card_id", "some_item2_card_id"
                ));

        @SuppressWarnings("redundant")
        Map<String, Object> collectionMap = Map.of(
                "name", "some name",
                "url", "http://some-url.ru",
                "thumb_id", "some_thumb_id",
                "cards_count", 2,
                "items", items,
                "id", "some_collection_id"
        );

        return collectionMap;
    }
}
