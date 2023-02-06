package ru.yandex.market.global.index;


import java.util.List;

import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.jupiter.api.Test;

import ru.yandex.market.global.common.elastic.XContentBuilderFactory;

public class XContentBuilderFactoryTest {

    //@formatter:off
    @SneakyThrows
    private static XContentBuilder createShopIndexSettings() {
        return XContentFactory.jsonBuilder()
                .startObject()
                    .startObject("mappings")
                        .startObject("properties")
                            .startObject("deliveryArea")
                                .field("type", "geo_shape")
                            .endObject()
                            .startObject("address")
                                .startObject("properties")
                                    .startObject("coordinates")
                                        .field("type", "geo_point")
                                    .endObject()
                                .endObject()
                            .endObject()
                            .startObject("schedule")
                                .field("type", "nested")
                            .endObject()
                            .startObject("returnPolicy")
                                .startObject("properties")
                                    .startObject("branches")
                                        .field("type", "nested")
                                    .endObject()
                                .endObject()
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
    }

    @SneakyThrows
    private XContentBuilder getCategoryIndexSettings() {
        return XContentFactory.jsonBuilder()
            .startObject()
                .startObject("mappings")
                    .startObject("properties")
                        .startObject("attributes")
                            .field("type", "nested")
                        .endObject()
                    .endObject()
                .endObject()
                .startObject("settings")
                    .startObject("analysis")
                        .startObject("analyzer")
                            // Установка analysis.analyzer.default как написано в документации
                            // не приводит к использованию этого анализатора по умолчанию.
                            // Но если просто назвать анализатор как default то он начинает использоваться
                            // по умолчанию.
                            .startObject("default")
                                .field("tokenizer", "standard")
                                .array("filter", "lowercase", "he_IL", "en_GB")
                            .endObject()
                        .endObject()
                        .startObject("filter")
                            .startObject("he_IL")
                                .field("type", "hunspell")
                                .field("locale", "he_IL")
                                .field("language", "he_IL")
                            .endObject()
                            .startObject("en_GB")
                                .field("type", "hunspell")
                                .field("locale", "en_GB")
                                .field("language", "en_GB")
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    @SneakyThrows
    public XContentBuilder getOfferIndexSettings() {
        return XContentFactory.jsonBuilder()
            .startObject()
                .startObject("mappings")
                    .startObject("properties")
                        .startObject("shop")
                            .startObject("properties")
                                .startObject("deliveryArea")
                                    .field("type", "geo_shape")
                                .endObject()
                                .startObject("coordinates")
                                    .field("type", "geo_point")
                                .endObject()
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject()
                .startObject("settings")
                    .startObject("analysis")
                        .startObject("analyzer")
                            // Установка analysis.analyzer.default как написано в документации
                            // не приводит к использованию этого анализатора по умолчанию.
                            // Но если просто назвать анализатор как default то он начинает использоваться
                            // по умолчанию.
                            .startObject("default")
                                .field("tokenizer", "standard")
                                .array("filter", "lowercase", "he_IL", "en_GB")
                            .endObject()
                        .endObject()
                        .startObject("filter")
                            .startObject("he_IL")
                                .field("type", "hunspell")
                                .field("locale", "he_IL")
                                .field("language", "he_IL")
                            .endObject()
                            .startObject("en_GB")
                                .field("type", "hunspell")
                                .field("locale", "en_GB")
                                .field("language", "en_GB")
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }
    //@formatter:on

    @Test
    public void testShop() {
        XContentBuilder shopIndexSettings = createShopIndexSettings();
        XContentBuilder actual = new XContentBuilderFactory()
                .addField(List.of("mappings", "properties", "deliveryArea"), "type", "geo_shape")
                .addField(List.of("mappings", "properties", "address", "properties", "coordinates"),
                        "type", "geo_point")
                .addField(List.of("mappings", "properties", "schedule"), "type", "nested")
                .addField(List.of("mappings", "properties", "returnPolicy", "properties", "branches"),
                        "type", "nested").build();
        shopIndexSettings.close();
        actual.close();
        Assertions.assertThat(BytesReference.bytes(actual).utf8ToString())
                .isEqualTo(BytesReference.bytes(shopIndexSettings).utf8ToString());
    }

    @Test
    public void testCategory() {
        XContentBuilder shopIndexSettings = getCategoryIndexSettings();
        XContentBuilder actual = new XContentBuilderFactory()
                .addField(List.of("mappings", "properties", "attributes"), "type", "nested")
                .addField(List.of("settings", "analysis", "analyzer", "default"), "tokenizer", "standard")
                .addArray(List.of("settings", "analysis", "analyzer", "default"),
                        "filter", "lowercase", "he_IL", "en_GB")
                .addField(List.of("settings", "analysis", "filter", "he_IL"), "type", "hunspell")
                .addField(List.of("settings", "analysis", "filter", "he_IL"), "locale", "he_IL")
                .addField(List.of("settings", "analysis", "filter", "he_IL"), "language", "he_IL")
                .addField(List.of("settings", "analysis", "filter", "en_GB"), "type", "hunspell")
                .addField(List.of("settings", "analysis", "filter", "en_GB"), "locale", "en_GB")
                .addField(List.of("settings", "analysis", "filter", "en_GB"), "language", "en_GB")
                .build();
        shopIndexSettings.close();
        actual.close();
        Assertions.assertThat(BytesReference.bytes(actual).utf8ToString())
                .isEqualTo(BytesReference.bytes(shopIndexSettings).utf8ToString());
    }

    @Test
    public void testOffer() {
        XContentBuilder shopIndexSettings = getOfferIndexSettings();
        XContentBuilder actual = new XContentBuilderFactory()
                .addField(List.of("mappings", "properties", "shop", "properties", "deliveryArea"), "type", "geo_shape")
                .addField(List.of("mappings", "properties", "shop", "properties", "coordinates"), "type", "geo_point")
                .addField(List.of("settings", "analysis", "analyzer", "default"), "tokenizer", "standard")
                .addArray(List.of("settings", "analysis", "analyzer", "default"),
                        "filter", "lowercase", "he_IL", "en_GB")
                .addField(List.of("settings", "analysis", "filter", "he_IL"), "type", "hunspell")
                .addField(List.of("settings", "analysis", "filter", "he_IL"), "locale", "he_IL")
                .addField(List.of("settings", "analysis", "filter", "he_IL"), "language", "he_IL")
                .addField(List.of("settings", "analysis", "filter", "en_GB"), "type", "hunspell")
                .addField(List.of("settings", "analysis", "filter", "en_GB"), "locale", "en_GB")
                .addField(List.of("settings", "analysis", "filter", "en_GB"), "language", "en_GB")
                .build();
        shopIndexSettings.close();
        actual.close();
        Assertions.assertThat(BytesReference.bytes(actual).utf8ToString())
                .isEqualTo(BytesReference.bytes(shopIndexSettings).utf8ToString());
    }

}
