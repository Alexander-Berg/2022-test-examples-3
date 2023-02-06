package ru.yandex.direct.grid.processing.util;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.model.KeywordWithMinuses;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdUpdateKeywordsPayloadItem;

public class KeywordsTestUtils {

    public static GdUpdateKeywordsPayloadItem toUpdateKeywordsItemData(Keyword keyword) {
        KeywordWithMinuses keywordWithMinuses = KeywordWithMinuses.fromPhrase(keyword.getPhrase());
        return new GdUpdateKeywordsPayloadItem()
                .withId(keyword.getId())
                .withKeyword(keywordWithMinuses.getPlusKeyword())
                .withIsSuspended(keyword.getIsSuspended())
                .withMinusKeywords(keywordWithMinuses.getMinusKeywords())
                .withDuplicate(false);
    }
}
