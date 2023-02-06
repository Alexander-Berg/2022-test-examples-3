package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class TestContentPromotionCommonData {
    private static final String URL = "https://some-url.ru";
    private static final String EXTERNAL_ID = "EXTERNAL_ID";
    private static final String METADATA = "{\"metadata\": \"metadata\"}";

    public static ContentPromotionContent defaultContentPromotion(
            ClientId clientId, ContentPromotionContentType type) {
        return new ContentPromotionContent()
                .withClientId(ifNotNull(clientId, ClientId::asLong))
                .withType(type)
                .withUrl(URL)
                .withPreviewUrl(URL)
                .withExternalId(EXTERNAL_ID + "_" + type.toString() + "_" + randomAlphanumeric(16))
                .withMetadata(METADATA)
                .withIsInaccessible(false);
    }
}
