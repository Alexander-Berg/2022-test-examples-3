package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.TurboAppMetaContent;
import ru.yandex.direct.turboapps.client.model.TurboAppInfoResponse;

public class TestTurboApps {

    public static final long DEFAULT_APP_ID = 1L;
    public static final String DEFAULT_CONTENT = "{\"TurboAppUrlType\": \"AsIs\"}";
    public static final String DEFAULT_META_CONTENT = "{\"name\": \"Add name\", " +
            "\"iconUrl\": \"https://ya.ru/icon.url\", \"description\": \"App description\"}";

    public static TurboAppInfoResponse defaultTurboAppResponse() {
        return new TurboAppInfoResponse()
                .withAppId(DEFAULT_APP_ID)
                .withContent(DEFAULT_CONTENT)
                .withMetaContent(DEFAULT_META_CONTENT);
    }

    public static TurboAppMetaContent defaultTurboAppMetaContent() {
        return new TurboAppMetaContent()
                .withName("Add name")
                .withDescription("App description")
                .withIconUrl("https://ya.ru/icon.url");
    }

}
