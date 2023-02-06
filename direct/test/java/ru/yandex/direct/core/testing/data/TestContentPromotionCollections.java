package ru.yandex.direct.core.testing.data;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpData;
import ru.yandex.direct.utils.HashingUtils;
import ru.yandex.direct.utils.JsonUtils;

import static ru.yandex.direct.core.entity.contentpromotion.type.collection.ContentPromotionCollectionUtils.convertToPreviewUrl;

public class TestContentPromotionCollections {
    public static ContentPromotionContent fromSerpData(CollectionSerpData serpData, ClientId clientId) {
        String url = serpData.getUrl();
        String serpDataJson = serpData.getNormalizedJson();

        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        return new ContentPromotionContent()
                .withUrl(url)
                .withType(ContentPromotionContentType.COLLECTION)
                .withExternalId(serpData.getId())
                .withPreviewUrl(convertToPreviewUrl(serpData.getThumbId(), EnvironmentType.TESTING))
                .withMetadata(serpDataJson)
                .withMetadataHash(HashingUtils.getMd5HalfHashUtf8(serpDataJson))
                .withClientId(clientId.asLong())
                .withIsInaccessible(false)
                .withMetadataRefreshTime(now)
                .withMetadataCreateTime(now)
                .withMetadataModifyTime(now);
    }

    public static CollectionSerpData realLifeCollection() throws IOException {
        return realLifeCollection(Collections.emptyMap());
    }

    /**
     * Получить тестовую коллекцию с изменёнными полями.
     * @param changedFields Мапа изменяемых в тестовой коллекции полей и их значений. Новые ключи также будут
     *                      добавлены в объект. Для примера возможных полей см. файл
     *                      ru/yandex/direct/libs/collections/test_collection_serp_data.json
     */
    public static CollectionSerpData realLifeCollection(Map<String, Object> changedFields) throws IOException {
        URL url = Resources.getResource("ru/yandex/direct/libs/collections/test_collection_serp_data.json");
        String json = Resources.toString(url, Charsets.UTF_8);
        @SuppressWarnings("unchecked")
        Map<String, Object> deserializedJson = JsonUtils.fromJson(json, Map.class);
        deserializedJson.putAll(changedFields);
        return CollectionSerpData.fromMap(deserializedJson);
    }
}
